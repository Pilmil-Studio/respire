package fr.respire.app

import fr.respire.app.FacebookReels.Box
import fr.respire.app.FacebookReels.FbNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class FacebookReelsTest {

    private val screen = Box(0, 0, 1080, 2400)
    private val full = Box(0, 0, 1080, 2400)
    private val offScreen = Box(1080, 0, 2160, 2400)

    private fun node(
        desc: String?,
        bounds: Box = full,
        visible: Boolean = true,
        cover: Int = bounds.height,
    ) = FbNode(desc, visible, bounds, cover)

    private fun detect(vararg nodes: FbNode, inputs: List<Box> = emptyList()) =
        FacebookReels.detect(nodes.toList(), screen) { inputs }

    @Test
    fun exactLabelAtCenter_isBlocked() {
        assertEquals("Reels tab details", detect(node("Reels tab details")))
        assertEquals("Reel details", detect(node("Reel details")))
    }

    @Test
    fun comparisonIsExact_caseAndSpacesCount() {
        assertNull(detect(node("  Reels tab details ")))
        assertNull(detect(node("reels TAB details")))
        assertNull(detect(node("reel details")))
    }

    @Test
    fun sameLabelOffScreen_isNotBlocked() {
        assertNull(detect(node("Reels tab details", offScreen)))
    }

    @Test
    fun invisibleNode_isNotBlocked() {
        assertNull(detect(node("Reels tab details", visible = false)))
    }

    @Test
    fun partialOrOtherLabels_areNotBlocked() {
        assertNull(detect(node("Reel")))
        assertNull(detect(node("Reels tab")))
        assertNull(detect(node("View reel by Alice")))
        assertNull(detect(node("Reels")))
        assertNull(detect(node("Navigate to your Reels profile")))
        assertNull(detect(node("Reel details of the day")))
        assertNull(detect(node(null)))
        assertNull(detect(node("   ")))
    }

    @Test
    fun reelDetails_needsLargeCoverage() {
        val small = Box(300, 1000, 800, 1400) // contient le centre, mais couvre 400 px sur 2400
        assertNull(detect(node("Reel details", small)))
        // Le conteneur proche (ancêtre) couvre l'écran : accepté.
        assertEquals("Reel details", detect(node("Reel details", small, cover = 2300)))
    }

    @Test
    fun reelsTabDetails_needsNoCoverage() {
        val small = Box(300, 1000, 800, 1400)
        assertEquals("Reels tab details", detect(node("Reels tab details", small)))
    }

    @Test
    fun inputFieldAtBottom_isNotBlocked() {
        val field = Box(40, 2150, 1040, 2300)
        assertNull(detect(node("Reels tab details"), inputs = listOf(field)))
    }

    @Test
    fun inputFieldAtTop_doesNotPrevent() {
        val search = Box(40, 100, 1040, 250)
        assertEquals("Reel details", detect(node("Reel details"), inputs = listOf(search)))
    }

    @Test
    fun inputsAreOnlyListedWhenACandidateStands() {
        var asked = false
        val r = FacebookReels.detect(listOf(node("Reel")), screen) { asked = true; emptyList() }
        assertNull(r)
        assertFalse(asked)
    }

    @Test
    fun labelInUnknownLanguage_isNotBlocked() {
        assertNull(detect(node("Détails du Reel")))
        assertNull(detect(node("Detalles del Reel")))
        assertNull(detect(node("Reel-Details")))
    }

    @Test
    fun coverLabels_areKnownLabels() {
        FacebookReels.needsCover.forEach { (lang, list) ->
            assertEquals(true, FacebookReels.labels[lang].orEmpty().containsAll(list))
        }
    }
}
