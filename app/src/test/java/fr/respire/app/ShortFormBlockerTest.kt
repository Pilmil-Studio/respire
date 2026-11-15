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

        assertTrue(ShortFormBlocker.knownIds(ShortFormBlocker.FACEBOOK).isEmpty())
    }

    @Test
    fun packages_containsSupportedApps() {
        assertEquals(
            setOf(ShortFormBlocker.INSTAGRAM, ShortFormBlocker.YOUTUBE, ShortFormBlocker.FACEBOOK),
            ShortFormBlocker.PACKAGES,
        )
    }
}
