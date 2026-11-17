package fr.respire.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.view.inputmethod.InputMethodManager

/**
 * Service d'accessibilité.
 *
 * Pauses : seul le nom du paquet de l'appli au premier plan est utilisé.
 *  1. Ouverture : passage d'une autre appli vers une appli ciblée.
 *  2. Fin de session : l'utilisateur est toujours dans l'appli quand la session autorisée expire.
 *
 * Reels / Shorts (options) : dans Instagram, YouTube et Facebook uniquement, les identifiants
 * techniques des vues (et, dans Facebook, des libellés d'accessibilité d'éléments d'interface) sont
 * consultés pour repérer le lecteur plein écran (voir ShortFormBlocker).
 */
class PauseService : AccessibilityService() {

    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())

    private var lastPkg: String? = null
    private val lastTrigger = HashMap<String, Long>()
    private var ignored: Set<String> = emptySet()

    private var sessionPkg: String? = null
    private val sessionCheck = Runnable { onSessionTimer() }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ignored = computeIgnored()
        applyEventTypes()
        prefs.registerShortFilterListener(shortFilterListener)
        // La version 3.3 mémorisait des identifiants trop larges : on les efface.
        prefs.clearLearnedShortIds()
    }

    // Référence forte : SharedPreferences ne garde que des références faibles à ses écouteurs.
    private val shortFilterListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in Prefs.SHORT_FILTER_KEYS) applyEventTypes()
    }

    /** Minimum (changements de fenêtre) tant que le filtre est coupé ; élargi quand il est activé. */
    private fun applyEventTypes() {
        val info = serviceInfo ?: return
        val types = ServiceEventTypes.forShortFilter(prefs.anyShortFilter)
        if (info.eventTypes == types) return
        info.eventTypes = types
        serviceInfo = info
    }

    /** Fenêtres qui ne doivent pas compter comme « changer d'appli » (clavier, barre système). */
    private fun computeIgnored(): Set<String> {
        val set = mutableSetOf("com.android.systemui", "android")
        getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.forEach { set.add(it.packageName) }
        return set
    }

    // ---- Reels / Shorts ---------------------------------------------------------

    // Les analyses tournent sur un fil à part : une exploration lente ne bloque jamais le service.
    private val shortThread = HandlerThread("respire-shorts").apply { start() }
    private val shortHandler = Handler(shortThread.looper)

    private val shortLock = Any()
    private var shortPkg: String? = null
    private var shortScheduled = false
    private val shortCheck = Runnable { runShortCheck() }

    private fun scheduleShortCheck(delayMs: Long) {
        synchronized(shortLock) {
            if (!shortScheduled) {
                shortScheduled = true
                shortHandler.postDelayed(shortCheck, delayMs)
            }
        }
    }

    private fun onShortFormCandidate(pkg: String) {
        val enabled = when (pkg) {
            ShortFormBlocker.INSTAGRAM -> prefs.blockReels
            ShortFormBlocker.YOUTUBE -> prefs.blockShorts
            ShortFormBlocker.FACEBOOK -> prefs.blockFacebookReels
            else -> false
        }
        if (!enabled) return
        synchronized(shortLock) {
            shortPkg = pkg
            // Au plus une vérification toutes les 120 ms, même si les événements s'enchaînent.
            if (!shortScheduled) {
                shortScheduled = true
                shortHandler.postDelayed(shortCheck, SHORT_CHECK_MS)
            }
        }
    }

    private val shortGuard = ShortFormGuard()

    private fun runShortCheck() {
        val pkg = synchronized(shortLock) {
            shortScheduled = false
            shortPkg
        } ?: return
        val now = SystemClock.uptimeMillis()

        // Juste après un « retour », l'écran est encore en transition : on attend,
        // puis on vérifie une seule fois si le lecteur est toujours là.
        val pre = shortGuard.beforeScan(now)
        if (pre is ShortFormGuard.PreScan.Wait) {
            scheduleShortCheck(pre.delayMs)
            return
        }
        val explore = (pre as ShortFormGuard.PreScan.Scan).explore

        val roots = ShortFormBlocker.roots(this, pkg) { prefs.recordShortFailure(Prefs.FAIL_TREE_ERROR) }
        if (roots.isEmpty()) {
            prefs.recordShortFailure(Prefs.FAIL_NO_WINDOW)
            return
        }

        val result = ShortFormBlocker.scan(roots, pkg, explore)
        val post = shortGuard.afterScan(now, result)
        if (post.saveDiagnostic) {
            prefs.saveShortDiagnostic(pkg, result)
        }

        when (val action = post.action) {
            ShortFormGuard.Action.None,
            ShortFormGuard.Action.LimitReached -> return
            is ShortFormGuard.Action.Confirm -> {
                scheduleShortCheck(action.delayMs)
                return
            }
            is ShortFormGuard.Action.GoBack -> {
                ShortFormBlocker.goBack(this)
                prefs.recordShortBlocked(System.currentTimeMillis(), pkg)
                if (action.showToast) {
                    val msg = if (pkg == ShortFormBlocker.YOUTUBE) R.string.toast_shorts_blocked else R.string.toast_reels_blocked
                    handler.post { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                }
                // Vérification de contrôle à la fin de la transition.
                scheduleShortCheck(action.recheckDelayMs)
            }
        }
    }

    // ---- Pauses -----------------------------------------------------------------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ShortFormBlocker.PACKAGES) {
            onShortFormCandidate(pkg)
            // Facebook : on garde le nom technique des écrans pour le diagnostic.
            if (pkg == ShortFormBlocker.FACEBOOK && prefs.blockFacebookReels &&
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            ) {
                event.className?.toString()?.takeIf { it.startsWith("com.facebook") }
                    ?.let { prefs.noteScreen(pkg, it.removePrefix("com.facebook.")) }
            }
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (pkg in ignored) return

        if (pkg == packageName) {
            lastPkg = pkg
            return
        }
        // L'appli ciblée qui finit de se charger derrière notre écran de pause : on ignore.
        if (PauseActivity.showingFor == pkg) {
            val until = prefs.allowedUntil(pkg)
            if (System.currentTimeMillis() < until) scheduleSessionCheck(pkg, until)
            return
        }

        val previous = lastPkg
        lastPkg = pkg
        if (pkg == previous) return

        val targets = prefs.targets
        if (pkg !in targets) return

        val now = System.currentTimeMillis()
        val until = prefs.allowedUntil(pkg)
        if (now < until) {
            // Retour dans une session encore valide : on surveille sa fin.
            scheduleSessionCheck(pkg, until)
            return
        }
        if (now - (lastTrigger[pkg] ?: 0L) < DEBOUNCE_MS) return
        lastTrigger[pkg] = now

        prefs.recordAttempt(pkg, now)
        showPause(pkg, PauseActivity.MODE_OPEN)
    }

    private fun scheduleSessionCheck(pkg: String, until: Long) {
        handler.removeCallbacks(sessionCheck)
        sessionPkg = pkg
        val delay = (until - System.currentTimeMillis()).coerceAtLeast(0L) + 500L
        handler.postDelayed(sessionCheck, delay)
    }

    private fun onSessionTimer() {
        val pkg = sessionPkg ?: return
        sessionPkg = null
        // Toujours dans l'appli et session expirée : on interrompt le défilement.
        if (lastPkg == pkg && System.currentTimeMillis() >= prefs.allowedUntil(pkg)) {
            showPause(pkg, PauseActivity.MODE_SESSION_END)
        }
    }

    private fun showPause(pkg: String, mode: String) {
        val intent = Intent(this, PauseActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(PauseActivity.EXTRA_PKG, pkg)
            putExtra(PauseActivity.EXTRA_MODE, mode)
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        prefs.unregisterShortFilterListener(shortFilterListener)
        handler.removeCallbacksAndMessages(null)
        shortHandler.removeCallbacksAndMessages(null)
        shortThread.quitSafely()
        super.onDestroy()
    }

    companion object {
        private const val DEBOUNCE_MS = 1500L
        private const val SHORT_CHECK_MS = 120L
    }
}
