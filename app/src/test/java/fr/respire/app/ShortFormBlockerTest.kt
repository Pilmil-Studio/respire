package fr.respire.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortFormBlockerTest {

    @Test
    fun knownIds_returnsExpectedIdsPerPackage() {
        val ig = ShortFormBlocker.knownIds(ShortFormBlocker.INSTAGRAM)
        assertTrue("clips_viewer_view_pager" in ig)
        assertTrue("clips_video_container" in ig)
        assertTrue("clips_viewer_container" in ig)

        val yt = ShortFormBlocker.knownIds(ShortFormBlocker.YOUTUBE)
        assertTrue("reel_recycler" in yt)
        assertTrue("shorts_player" in yt)

        // Facebook masque tous ses identifiants : la détection passe par les libellés d'accessibilité.
        assertTrue(ShortFormBlocker.knownIds(ShortFormBlocker.FACEBOOK).isEmpty())
        assertTrue("Reels tab details" in FacebookReels.allLabels)
        assertTrue("Reel details" in FacebookReels.allLabels)
    }

    @Test
    fun packages_containsSupportedApps() {
        assertEquals(
            setOf(ShortFormBlocker.INSTAGRAM, ShortFormBlocker.YOUTUBE, ShortFormBlocker.FACEBOOK),
            ShortFormBlocker.PACKAGES,
        )
    }

    @Test
    fun coversEnough_enforcesVisibilityAnd70PercentThreshold() {
        assertTrue(ShortFormBlocker.coversEnough(visible = true, nodeHeight = 700, rootHeight = 1000))
        assertFalse(ShortFormBlocker.coversEnough(visible = true, nodeHeight = 699, rootHeight = 1000))
        assertFalse(ShortFormBlocker.coversEnough(visible = false, nodeHeight = 1000, rootHeight = 1000))
        assertFalse(ShortFormBlocker.coversEnough(visible = true, nodeHeight = 0, rootHeight = 0))
    }

    @Test
    fun evaluateScan_coversBlockedExclusionsAndExploration() {
        val noWin = ShortFormBlocker.evaluateScan(0, null, null, dm = false, negative = false, durationMs = 5)
        assertFalse(noWin.blocked)
        assertEquals("-", noWin.method)

        val quickHit = ShortFormBlocker.evaluateScan(1, "reel_recycler", null, dm = false, negative = false, durationMs = 4)
        assertTrue(quickHit.blocked)
        assertEquals("rapide", quickHit.method)
        assertTrue("reel_recycler" in quickHit.reason)

        val dmAllowed = ShortFormBlocker.evaluateScan(1, "clips_viewer_view_pager", null, dm = true, negative = false, durationMs = 3)
        assertFalse(dmAllowed.blocked)

        val liveAllowed = ShortFormBlocker.evaluateScan(1, "reel_recycler", null, dm = false, negative = true, durationMs = 3)
        assertFalse(liveAllowed.blocked)

        val full = ShortFormBlocker.FullScan(
            match = "reel_something",
            seen = listOf("reel_something 85%"),
            nodes = 120,
            withId = 40,
            descriptions = listOf("desc"),
        )
        val exploreMiss = ShortFormBlocker.evaluateScan(2, null, full, dm = false, negative = false, durationMs = 15)
        assertFalse(exploreMiss.blocked)
        assertEquals("exploration", exploreMiss.method)
        assertEquals(120, exploreMiss.nodes)
        assertTrue("reel_something" in exploreMiss.reason)
    }
}
