package fr.respire.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/**
 * Repère le lecteur plein écran des Reels (Instagram, Facebook) et des Shorts (YouTube)
 * à partir des identifiants techniques des vues (Instagram, YouTube) ou, pour Facebook dont
 * les identifiants sont masqués, de libellés d'accessibilité d'éléments d'interface
 * (voir [FacebookReels]), puis fait « retour ». Le reste de l'appli reste utilisable.
 *
 * Seuls ces identifiants et libellés sont consultés, uniquement dans ces trois applis,
 * uniquement si l'option est active. Rien n'est envoyé : le diagnostic reste
 * dans les préférences locales ; il ne contient que des identifiants techniques et, pour
 * Facebook, des libellés d'éléments d'interface tronqués (jamais de texte saisi).
 *
 * Le blocage repose uniquement sur une liste fermée d'identifiants connus, recherchés
 * directement (quelques millisecondes). L'exploration complète de l'écran ne sert qu'au
 * diagnostic : elle signale des candidats, mais ne déclenche jamais de « retour »,
 * car une détection trop large confond le fil d'accueil avec le lecteur de Shorts.
 *
 * Identifiants connus repris du jeu de règles ouvert short-video-rules (MIT) et de appblock.
 */
object ShortFormBlocker {

    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
    const val FACEBOOK = "com.facebook.katana"

    val PACKAGES = setOf(INSTAGRAM, YOUTUBE, FACEBOOK)

    private val reelsIds = setOf("clips_viewer_view_pager", "clips_video_container", "clips_viewer_container")
    private val shortsIds = setOf(
        "reel_recycler",
        "reel_player_page_container",
        "reel_watch_fragment_root",
        "reel_watch_player",
        "shorts_player",
    )

    /**
     * Facebook : tous les identifiants y sont masqués (« (name removed) »), donc aucun n'est utilisable.
     * La détection passe par les libellés d'accessibilité, voir [FacebookReels].
     */
    private val facebookReelsIds = emptySet<String>()

    /** Reel ouvert depuis une conversation : on le laisse passer. */
    private val dmIds = listOf("sender_username_or_fullname", "reply_bar_edittext")

    /** Le lecteur doit couvrir au moins 70 % de la hauteur (évite les vignettes de l'accueil). */
    private const val MIN_COVER = 0.7f
    private const val MAX_NODES = 1500
    private const val WIDE_COVER = 40
    private const val MAX_DESCRIPTIONS = 40
    private const val MAX_DESC_LEN = 60

    data class Result(
        val blocked: Boolean,
        val windows: Int,
        val method: String,
        val nodes: Int,
        val durationMs: Long,
        /** Identifiants intéressants vus, avec leur couverture en % : « reel_recycler 98% ». */
        val seen: List<String>,
        val reason: String,
        /** Nœuds qui portent un identifiant (exploration seulement). */
        val withId: Int = 0,
        /** Facebook, exploration seulement : libellés d'éléments d'interface vus, tronqués. */
        val descriptions: List<String> = emptyList(),
    )

    /** Présents quand on regarde un direct ou une vidéo classique : jamais de blocage. */
    private val youtubeNegativeIds = listOf("immersive_live_player", "live_chat_content", "live_chat_text_field_bar")

    fun knownIds(pkg: String): Set<String> = when (pkg) {
        INSTAGRAM -> reelsIds
        FACEBOOK -> facebookReelsIds
        else -> shortsIds
    }

    /** Fenêtres de l'appli visée ; à défaut, la fenêtre active. */
    fun roots(
        service: AccessibilityService,
        pkg: String,
        onTreeError: () -> Unit = {},
    ): List<AccessibilityNodeInfo> {
        val fromWindows = try {
            service.windows
                .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                .mapNotNull { it.root }
                .filter { it.packageName?.toString() == pkg }
        } catch (e: RuntimeException) {
            onTreeError() // compté dans le diagnostic, sans rien consigner de l'écran
            emptyList()
        }
        if (fromWindows.isNotEmpty()) return fromWindows
        val active = service.rootInActiveWindow ?: return emptyList()
        return if (active.packageName?.toString() == pkg) listOf(active) else emptyList()
    }

    /** Recherche rapide par identifiants connus. Renvoie l'identifiant trouvé, ou null. */
    fun quickMatch(roots: List<AccessibilityNodeInfo>, pkg: String): String? {
        if (pkg == FACEBOOK) return facebookMatch(roots)
        val rect = Rect()
        for (root in roots) {
            val height = rootHeight(root)
            for (id in knownIds(pkg)) {
                val nodes = root.findAccessibilityNodeInfosByViewId("$pkg:id/$id") ?: continue
                val hit = nodes.any { n ->
                    n.isVisibleToUser && run { n.getBoundsInScreen(rect); rect.height() >= height * MIN_COVER }
                }
                recycleNodes(nodes, root)
                if (hit) return id
            }
        }
        return null
    }

    /**
     * Facebook : cherche les libellés connus (la recherche par texte d'Android couvre aussi la
     * description d'accessibilité), puis laisse [FacebookReels.detect] trancher sur le libellé exact,
     * la position et la couverture. Renvoie « desc:libellé », ou null.
     */
    private fun facebookMatch(roots: List<AccessibilityNodeInfo>): String? {
        for (root in roots) {
            val screen = boxOf(root)
            val candidates = ArrayList<FacebookReels.FbNode>()
            for (label in FacebookReels.allLabels) {
                val found = root.findAccessibilityNodeInfosByText(label) ?: continue
                for (n in found) {
                    if (FacebookReels.mainLabel(n.contentDescription?.toString()) == null) continue
                    candidates.add(
                        FacebookReels.FbNode(
                            description = n.contentDescription?.toString(),
                            visible = n.isVisibleToUser,
                            bounds = boxOf(n),
                            coverHeight = coverHeight(n),
                        ),
                    )
                }
                recycleNodes(found, root)
            }
            if (candidates.isEmpty()) continue
            val hit = FacebookReels.detect(candidates, screen) { visibleInputs(root) }
            if (hit != null) return "desc:$hit"
        }
        return null
    }

    private fun boxOf(n: AccessibilityNodeInfo): FacebookReels.Box {
        val r = Rect()
        n.getBoundsInScreen(r)
        return FacebookReels.Box(r.left, r.top, r.right, r.bottom)
    }

    /** Plus grande hauteur parmi le nœud et ses trois ancêtres les plus proches. */
    private fun coverHeight(n: AccessibilityNodeInfo): Int {
        var best = boxOf(n).height
        var cur: AccessibilityNodeInfo? = n.parent
        var depth = 0
        while (cur != null && depth < 3) {
            best = maxOf(best, boxOf(cur).height)
            val next = cur.parent
            recycleNode(cur)
            cur = next
            depth++
        }
        cur?.let { recycleNode(it) }
        return best
    }

    /** Champs de saisie visibles (EditText) : seulement leurs bornes, jamais leur contenu. */
    private fun visibleInputs(root: AccessibilityNodeInfo): List<FacebookReels.Box> {
        val out = ArrayList<FacebookReels.Box>()
        var count = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        try {
            while (queue.isNotEmpty() && count < MAX_NODES) {
                val node = queue.removeFirst()
                try {
                    count++
                    if (node.isVisibleToUser && node.className?.toString() == "android.widget.EditText") out.add(boxOf(node))
                    for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
                } finally {
                    if (node !== root) recycleNode(node)
                }
            }
        } finally {
            while (queue.isNotEmpty()) {
                val remaining = queue.removeFirst()
                if (remaining !== root) recycleNode(remaining)
            }
        }
        return out
    }

    fun hasNegative(roots: List<AccessibilityNodeInfo>, pkg: String): Boolean {
        if (pkg != YOUTUBE) return false
        return youtubeNegativeIds.any { id ->
            roots.any { root ->
                val nodes = root.findAccessibilityNodeInfosByViewId("$pkg:id/$id") ?: return@any false
                val hit = nodes.any { it.isVisibleToUser }
                recycleNodes(nodes, root)
                hit
            }
        }
    }

    fun isDirectMessage(roots: List<AccessibilityNodeInfo>, pkg: String): Boolean {
        if (pkg != INSTAGRAM) return false
        return dmIds.all { id ->
            roots.any { root ->
                val nodes = root.findAccessibilityNodeInfosByViewId("$pkg:id/$id") ?: return@any false
                val hit = nodes.any { it.isVisibleToUser }
                recycleNodes(nodes, root)
                hit
            }
        }
    }

    /** Exploration complète de l'arbre (lente). Sert uniquement au diagnostic. */
    data class FullScan(
        val match: String?,
        val seen: List<String>,
        val nodes: Int,
        val withId: Int,
        val descriptions: List<String> = emptyList(),
    )

    fun fullScan(roots: List<AccessibilityNodeInfo>, pkg: String): FullScan {
        val fallback = if (pkg == INSTAGRAM) "clips_viewer" else "reel"
        // Facebook nomme peu ses vues : on relève aussi toute zone qui couvre une bonne part de l'écran.
        val wide = pkg == FACEBOOK
        val seen = LinkedHashMap<String, Int>()
        var match: String? = null
        var count = 0
        var withId = 0
        val descriptions = LinkedHashSet<String>()
        val rect = Rect()
        for (root in roots) {
            val height = rootHeight(root)
            val screen = boxOf(root)
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            try {
                while (queue.isNotEmpty() && count < MAX_NODES) {
                    val node = queue.removeFirst()
                    try {
                        count++
                        if (wide && descriptions.size < MAX_DESCRIPTIONS) noteDescription(node, screen, descriptions)
                        val fullId = node.viewIdResourceName
                        if (fullId != null && node.isVisibleToUser) {
                            withId++
                            val id = fullId.substringAfter(":id/")
                            val lower = id.lowercase()
                            node.getBoundsInScreen(rect)
                            val cover = rect.height() * 100 / height
                            val named = "reel" in lower || "short" in lower || "clips" in lower || (wide && "video" in lower)
                            if (named || (wide && cover >= WIDE_COVER)) {
                                if ((seen[id] ?: -1) < cover) seen[id] = cover
                                if (match == null && cover >= MIN_COVER * 100 && fallback in lower) match = id
                            }
                        }
                        for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
                    } finally {
                        if (node !== root) recycleNode(node)
                    }
                }
            } finally {
                while (queue.isNotEmpty()) {
                    val remaining = queue.removeFirst()
                    if (remaining !== root) recycleNode(remaining)
                }
            }
        }
        val top = seen.entries.sortedByDescending { it.value }.take(25)
        return FullScan(match, top.map { "${it.key} ${it.value}%" }, count, withId, descriptions.toList())
    }

    /**
     * Facebook, diagnostic : relève la description d'un nœud visible non éditable, s'il contient le
     * centre de l'écran ou s'il est large. Tronquée, jamais de champ de saisie ni de contenu saisi.
     */
    private fun noteDescription(node: AccessibilityNodeInfo, screen: FacebookReels.Box, out: MutableSet<String>) {
        if (!node.isVisibleToUser || node.isEditable || node.className?.toString() == "android.widget.EditText") return
        val d = node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val b = boxOf(node)
        val centred = b.contains((screen.left + screen.right) / 2, (screen.top + screen.bottom) / 2)
        val large = b.height * 100 >= screen.height * WIDE_COVER || b.width * 2 >= screen.width
        if (centred || large) out.add(d.replace('\n', ' ').take(MAX_DESC_LEN))
    }

    @Suppress("DEPRECATION")
    private fun recycleNode(node: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            node.recycle()
        }
    }

    private fun recycleNodes(nodes: List<AccessibilityNodeInfo>, root: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            for (n in nodes) {
                if (n !== root) {
                    @Suppress("DEPRECATION")
                    n.recycle()
                }
            }
        }
    }

    /** Analyse complète utilisée par le service : rapide d'abord, exploration si demandé. */
    fun scan(roots: List<AccessibilityNodeInfo>, pkg: String, explore: Boolean): Result {
        val start = SystemClock.uptimeMillis()
        if (roots.isEmpty()) {
            return Result(false, 0, "-", 0, 0, emptyList(), "aucune fenêtre lisible")
        }
        val match = quickMatch(roots, pkg)
        var method = "rapide"
        var seen = emptyList<String>()
        var nodes = 0
        var withId = 0
        var descriptions = emptyList<String>()
        var candidate: String? = null
        if (match == null && explore) {
            val full = fullScan(roots, pkg)
            method = "exploration"
            seen = full.seen
            nodes = full.nodes
            withId = full.withId
            descriptions = full.descriptions
            candidate = full.match
        }
        val dm = match != null && isDirectMessage(roots, pkg)
        val negative = match != null && hasNegative(roots, pkg)
        val reason = when {
            match == null && candidate != null -> "lecteur non trouvé (candidat non utilisé : $candidate)"
            match == null -> "lecteur non trouvé"
            dm -> "Reel ouvert depuis un message, laissé passer"
            negative -> "direct ou vidéo classique, laissé passer"
            else -> "lecteur détecté via $match"
        }
        return Result(
            blocked = match != null && !dm && !negative,
            windows = roots.size,
            method = method,
            nodes = nodes,
            durationMs = SystemClock.uptimeMillis() - start,
            seen = seen,
            reason = reason,
            withId = withId,
            descriptions = descriptions,
        )
    }

    private fun rootHeight(root: AccessibilityNodeInfo): Int {
        val r = Rect()
        root.getBoundsInScreen(r)
        return r.height().coerceAtLeast(1)
    }

    fun goBack(service: AccessibilityService) {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }
}
