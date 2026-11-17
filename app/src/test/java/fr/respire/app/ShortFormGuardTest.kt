package fr.respire.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortFormGuardTest {

    private fun blockedResult(method: String = "rapide") = ShortFormBlocker.Result(
        blocked = true,
        windows = 1,
        method = method,
        nodes = 0,
        durationMs = 2,
        seen = emptyList(),
        reason = "lecteur détecté",
    )

    private fun clearResult(method: String = "rapide") = ShortFormBlocker.Result(
        blocked = false,
        windows = 1,
        method = method,
        nodes = 0,
        durationMs = 2,
        seen = emptyList(),
        reason = "lecteur non trouvé",
    )

    @Test
    fun requiresTwoStepConfirmationBeforeGoingBack() {
        val guard = ShortFormGuard()

        val pre1 = guard.beforeScan(10_000L) as ShortFormGuard.PreScan.Scan
        assertTrue(pre1.explore)
        val post1 = guard.afterScan(10_000L, blockedResult())
        assertTrue(post1.saveDiagnostic)
        assertEquals(ShortFormGuard.Action.Confirm(ShortFormGuard.DEFAULT_CONFIRM_MS), post1.action)

        val pre2 = guard.beforeScan(10_250L) as ShortFormGuard.PreScan.Scan
        assertFalse(pre2.explore)
        val post2 = guard.afterScan(10_250L, blockedResult())
        assertEquals(
            ShortFormGuard.Action.GoBack(
                showToast = true,
                recheckDelayMs = ShortFormGuard.DEFAULT_BACK_COOLDOWN_MS,
            ),
            post2.action,
        )
    }

    @Test
    fun cooldownWaitsUntilBackTransitionCompletes() {
        val guard = ShortFormGuard()

        guard.beforeScan(10_000L)
        guard.afterScan(10_000L, blockedResult())
        guard.beforeScan(10_250L)
        guard.afterScan(10_250L, blockedResult())

        val wait = guard.beforeScan(10_500L)
        assertEquals(ShortFormGuard.PreScan.Wait(650L), wait)

        val ready = guard.beforeScan(11_150L)
        assertTrue(ready is ShortFormGuard.PreScan.Scan)
    }

    @Test
    fun clearDetectionResetsConfirmationAndConsecutiveBackLimit() {
        val guard = ShortFormGuard()

        // Furtive detection does not trigger back once cleared.
        guard.beforeScan(10_000L)
        guard.afterScan(10_000L, blockedResult())
        guard.beforeScan(10_250L)
        val cleared = guard.afterScan(10_250L, clearResult())
        assertEquals(ShortFormGuard.Action.None, cleared.action)

        // Next blocked detection starts confirmation from scratch.
        guard.beforeScan(11_000L)
        val confirmAgain = guard.afterScan(11_000L, blockedResult())
        assertEquals(ShortFormGuard.Action.Confirm(ShortFormGuard.DEFAULT_CONFIRM_MS), confirmAgain.action)
    }

    @Test
    fun stopsAfterMaxConsecutiveBacksUntilScreenClears() {
        val guard = ShortFormGuard(maxConsecutiveBacks = 3)
        var t = 10_000L

        repeat(3) { index ->
            guard.beforeScan(t)
            assertTrue(guard.afterScan(t, blockedResult()).action is ShortFormGuard.Action.Confirm)
            t += 250L
            guard.beforeScan(t)
            val back = guard.afterScan(t, blockedResult()).action as ShortFormGuard.Action.GoBack
            assertEquals(index == 0, back.showToast)
            t += ShortFormGuard.DEFAULT_BACK_COOLDOWN_MS
        }

        // 4th attempt hits guardrail instead of backing out of the app.
        guard.beforeScan(t)
        assertTrue(guard.afterScan(t, blockedResult()).action is ShortFormGuard.Action.Confirm)
        t += 250L
        guard.beforeScan(t)
        assertEquals(ShortFormGuard.Action.LimitReached, guard.afterScan(t, blockedResult()).action)

        // Leaving the player resets the counter.
        t += 500L
        guard.beforeScan(t)
        assertEquals(ShortFormGuard.Action.None, guard.afterScan(t, clearResult()).action)

        t += 500L
        guard.beforeScan(t)
        assertTrue(guard.afterScan(t, blockedResult()).action is ShortFormGuard.Action.Confirm)
        t += 250L
        guard.beforeScan(t)
        val backAfterReset = guard.afterScan(t, blockedResult()).action as ShortFormGuard.Action.GoBack
        assertTrue(backAfterReset.showToast)
    }

    @Test
    fun diagnosticsThrottleRegularMissesButAlwaysSaveExplorations() {
        val guard = ShortFormGuard(diagEveryMs = 2000L, exploreEveryMs = 6000L)

        guard.beforeScan(10_000L)
        assertTrue(guard.afterScan(10_000L, clearResult("rapide")).saveDiagnostic)

        guard.beforeScan(10_500L)
        assertFalse(guard.afterScan(10_500L, clearResult("rapide")).saveDiagnostic)

        guard.beforeScan(11_000L)
        assertTrue(guard.afterScan(11_000L, clearResult("exploration")).saveDiagnostic)
    }
}
