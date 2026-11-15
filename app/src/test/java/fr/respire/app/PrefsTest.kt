package fr.respire.app

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Tests unitaires de la persistance locale ([Prefs]) avec un [SharedPreferences] en mémoire.
 */
class PrefsTest {

    private lateinit var savedTz: TimeZone
    private lateinit var sp: InMemorySharedPreferences
    private lateinit var prefs: Prefs

    @Before
    fun setUp() {
        savedTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"))
        sp = InMemorySharedPreferences()
        prefs = Prefs(FakeContext(sp))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(savedTz)
    }

    private fun at(y: Int, month: Int, d: Int, h: Int, min: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(y, month - 1, d, h, min, 0)
        }.timeInMillis

    @Test
    fun defaults_matchExpectedValues() {
        assertTrue(prefs.targets.isEmpty())
        assertEquals(10, prefs.baseDelay)
        assertEquals(5, prefs.stepDelay)
        assertEquals(10, prefs.sessionMinutes)
        assertEquals(6, prefs.dailyQuota)
        assertFalse(prefs.blockReels)
        assertFalse(prefs.blockShorts)
        assertFalse(prefs.blockFacebookReels)
        assertFalse(prefs.accessibilityConsent)
        assertFalse(prefs.onboardingDone)
        assertFalse(prefs.serviceSeenOn)
        assertFalse(prefs.reviewPromptDone)
        assertEquals(0L, prefs.lastServiceWarning)
        assertFalse(prefs.riskEnabled)
        assertEquals(22, prefs.riskStart)
        assertEquals(7, prefs.riskEnd)
        assertEquals(0, prefs.riskQuota)
    }

    @Test
    fun settingsAndAllow_roundTrip() {
        prefs.targets = setOf("com.instagram.android", "com.google.android.youtube")
        prefs.baseDelay = 15
        prefs.stepDelay = 3
        prefs.sessionMinutes = 20
        prefs.dailyQuota = 4
        prefs.blockReels = true
        prefs.blockShorts = true
        prefs.blockFacebookReels = true
        prefs.accessibilityConsent = true
        prefs.onboardingDone = true
        prefs.serviceSeenOn = true
        prefs.reviewPromptDone = true
        prefs.lastServiceWarning = 12345L
        prefs.riskEnabled = true
        prefs.riskStart = 21
        prefs.riskEnd = 6
        prefs.riskQuota = 2
        prefs.allow("com.instagram.android", 999_999L)

        assertEquals(setOf("com.instagram.android", "com.google.android.youtube"), prefs.targets)
        assertEquals(15, prefs.baseDelay)
        assertEquals(3, prefs.stepDelay)
        assertEquals(20, prefs.sessionMinutes)
        assertEquals(4, prefs.dailyQuota)
        assertTrue(prefs.blockReels)
        assertTrue(prefs.blockShorts)
        assertTrue(prefs.blockFacebookReels)
        assertTrue(prefs.accessibilityConsent)
        assertTrue(prefs.onboardingDone)
        assertTrue(prefs.serviceSeenOn)
        assertTrue(prefs.reviewPromptDone)
        assertEquals(12345L, prefs.lastServiceWarning)
        assertTrue(prefs.riskEnabled)
        assertEquals(21, prefs.riskStart)
        assertEquals(6, prefs.riskEnd)
        assertEquals(2, prefs.riskQuota)
        assertEquals(999_999L, prefs.allowedUntil("com.instagram.android"))
        assertEquals(0L, prefs.allowedUntil("com.unknown.app"))
    }

    @Test
    fun appendLog_prunesEntriesOlderThan24Hours() {
        val pkg = "com.instagram.android"
        val t0 = at(2026, 6, 10, 10, 0)
        prefs.recordAttempt(pkg, t0)
        prefs.recordAttempt(pkg, t0 + 2 * Rules.HOUR_MS)

        // 25 h après t0, t0 doit être purgé à l'écriture suivante, mais t0 + 2h reste (< 24 h).
        val tLater = t0 + 25 * Rules.HOUR_MS
        prefs.recordAttempt(pkg, tLater)

        assertEquals(2, prefs.attemptsSince(pkg, t0))
        assertEquals(1, prefs.attemptsSince(pkg, tLater - 1))
    }

    @Test
    fun todayCounters_countAttemptsOpensGiveUpsAndShorts() {
        val now = System.currentTimeMillis()
        val ig = "com.instagram.android"
        val yt = "com.google.android.youtube"

        prefs.recordAttempt(ig, now)
        prefs.recordAttempt(ig, now)
        prefs.recordAttempt(yt, now)
        prefs.recordOpen(ig, now)
        prefs.recordGiveUp(now, ig)
        prefs.recordGiveUp(now)
        prefs.recordShortBlocked(now, yt)

        assertEquals(3, prefs.attemptsTodayAll())
        assertEquals(1, prefs.opensToday(ig))
        assertEquals(0, prefs.opensToday(yt))
        assertEquals(2, prefs.giveUpsToday())
        assertEquals(1, prefs.shortsBlockedToday(now))
    }

    @Test
    fun delayFor_scalesWithAttemptsAndRiskWindow() {
        val pkg = "com.instagram.android"
        val dayTime = at(2026, 6, 10, 14, 0)
        val nightTime = at(2026, 6, 10, 23, 0)

        prefs.baseDelay = 10
        prefs.stepDelay = 5
        prefs.riskEnabled = true
        prefs.riskStart = 22
        prefs.riskEnd = 7

        prefs.recordAttempt(pkg, dayTime)
        assertEquals(10, prefs.delayFor(pkg, dayTime))

        prefs.recordAttempt(pkg, dayTime + 1000L)
        assertEquals(15, prefs.delayFor(pkg, dayTime + 1000L))

        // Hors plage si riskEnabled = false, puis doublé une fois activé.
        prefs.recordAttempt(pkg, nightTime)
        assertEquals(20, prefs.delayFor(pkg, nightTime))

        prefs.riskEnabled = false
        assertFalse(prefs.inRiskWindow(nightTime))
        assertNull(prefs.riskWindowStart(nightTime))
        assertEquals(10, prefs.delayFor(pkg, nightTime))
    }

    @Test
    fun blockReason_checksDailyAndRiskQuotas() {
        val pkg = "com.instagram.android"
        val nightTime = at(2026, 6, 10, 23, 0)

        prefs.dailyQuota = 5
        prefs.riskEnabled = true
        prefs.riskStart = 22
        prefs.riskEnd = 7
        prefs.riskQuota = 1

        assertEquals(Prefs.Block.NONE, prefs.blockReason(pkg, nightTime))
        prefs.recordOpen(pkg, nightTime)
        assertEquals(Prefs.Block.RISK, prefs.blockReason(pkg, nightTime))

        // Hors plage à risque, le quota journalier (5) n'est pas atteint.
        val dayTime = at(2026, 6, 11, 12, 0)
        assertEquals(Prefs.Block.NONE, prefs.blockReason(pkg, dayTime))
    }

    @Test
    fun appWeek_hoursOf_and_giveUpsLastWeek_aggregateHistory() {
        val pkg = "com.instagram.android"
        val today = at(2026, 6, 10, 15, 0)
        val yesterday = at(2026, 6, 9, 21, 0)

        assertTrue(prefs.appWeek(pkg, today).isEmpty)

        prefs.recordAttempt(pkg, yesterday)
        prefs.recordGiveUp(yesterday, pkg)
        prefs.recordAttempt(pkg, today)
        prefs.recordOpen(pkg, today)
        prefs.recordShortBlocked(today, pkg)

        val week = prefs.appWeek(pkg, today)
        assertFalse(week.isEmpty)
        assertEquals(listOf(0, 0, 0, 0, 0, 1, 1), week.attempts)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 1), week.opens)
        assertEquals(listOf(0, 0, 0, 0, 0, 1, 0), week.giveUps)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 1), week.shorts)
        assertEquals(1, week.hours[21])
        assertEquals(2, week.hours[15]) // 1 tentative + 1 short à 15 h

        val hoursToday = prefs.hoursOf(today)
        assertEquals(1, hoursToday.attempts[15])
        assertEquals(1, hoursToday.shorts[15])
        assertEquals(0, hoursToday.giveUps[15])

        val giveUps = prefs.giveUpsLastWeek(today, count = 3)
        assertEquals(listOf(0, 1, 0), giveUps)
    }

    @Test
    fun pruneOldDays_removesOnlyExpiredDayKeys() {
        sp.edit()
            .putInt("d_att_2025_1", 3)   // expiré (> 60 jours avant juin 2026)
            .putInt("d_att_2026_160", 2) // récent
            .putString("allow_com.foo", "123")
            .apply()

        prefs.pruneOldDays(at(2026, 6, 10, 12, 0))

        assertFalse(sp.contains("d_att_2025_1"))
        assertTrue(sp.contains("d_att_2026_160"))
        assertTrue(sp.contains("allow_com.foo"))
    }

    @Test
    fun diagnostics_noteScreen_failures_and_clearLearnedShortIds() {
        val pkg = ShortFormBlocker.FACEBOOK
        repeat(8) { i -> prefs.noteScreen(pkg, "Screen$i") }
        prefs.noteScreen(pkg, "Screen3") // remonté en tête sans doublon

        prefs.saveShortDiagnostic(
            pkg,
            ShortFormBlocker.Result(
                blocked = false,
                windows = 1,
                method = "exploration",
                nodes = 12,
                durationMs = 4,
                seen = listOf("reel_container 80%"),
                reason = "lecteur non trouvé",
                withId = 3,
            ),
        )
        prefs.recordShortFailure(Prefs.FAIL_NO_WINDOW, at(2026, 6, 10, 14, 30))

        val diag = prefs.shortDiagnostic()
        assertTrue(diag.contains(pkg))
        assertTrue(diag.contains("reel_container 80%"))
        assertTrue(diag.contains("Screen3, Screen7, Screen6, Screen5, Screen4, Screen2"))
        assertFalse(diag.contains("Screen0"))
        assertTrue(diag.contains("aucune fenêtre lisible : 1 fois"))

        sp.edit().putString("learned_yt", "foo").putString("keep_me", "bar").apply()
        prefs.clearLearnedShortIds()
        assertFalse(sp.contains("learned_yt"))
        assertTrue(sp.contains("keep_me"))
    }

    private class FakeContext(private val prefs: SharedPreferences) : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = LinkedHashMap<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(data)
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (data[key] as? Set<String>)?.toMutableSet() ?: defValues
        }
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = LinkedHashMap<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?) = apply { if (key != null) pending[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply {
                if (key != null) pending[key] = values?.toSet()
            }
            override fun putInt(key: String?, value: Int) = apply { if (key != null) pending[key] = value }
            override fun putLong(key: String?, value: Long) = apply { if (key != null) pending[key] = value }
            override fun putFloat(key: String?, value: Float) = apply { if (key != null) pending[key] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) pending[key] = value }
            override fun remove(key: String?) = apply { if (key != null) pending[key] = REMOVE }
            override fun clear() = apply { clearAll = true }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) data.clear()
                for ((k, v) in pending) {
                    if (v === REMOVE) data.remove(k) else data[k] = v
                }
            }
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private companion object {
            val REMOVE = Any()
        }
    }
}
