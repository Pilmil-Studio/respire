package fr.respire.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contraste WCAG 2.x sur les vraies couleurs de la palette. */
class RingContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private val rings = listOf("Ring1" to Calme.Ring1, "Ring2" to Calme.Ring2, "Ring3" to Calme.Ring3)

    @Test
    fun ringEdge_hasAtLeast3to1_againstBackgroundAndEveryRing() {
        (listOf("Ground" to Calme.Ground) + rings).forEach { (name, c) ->
            val ratio = contrast(Calme.RingEdge, c)
            assertTrue("RingEdge / $name = $ratio", ratio >= 3.0)
        }
    }

    @Test
    fun phaseWord_isReadableOnEveryRing() {
        rings.forEach { (name, c) ->
            val ratio = contrast(Calme.Ink, c)
            assertTrue("Ink / $name = $ratio", ratio >= 4.5)
        }
    }
}
