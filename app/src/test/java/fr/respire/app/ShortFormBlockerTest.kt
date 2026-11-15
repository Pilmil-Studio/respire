package fr.respire.app

import org.junit.Assert.assertEquals
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
}
