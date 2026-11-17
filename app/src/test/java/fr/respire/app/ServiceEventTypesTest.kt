package fr.respire.app

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceEventTypesTest {

    @Test
    fun filterOff_listensToWindowChangesOnly() {
        assertEquals(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, ServiceEventTypes.forShortFilter(false))
    }

    @Test
    fun filterOn_addsContentAndScrollEvents() {
        val types = ServiceEventTypes.forShortFilter(true)
        assertTrue(types and AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED != 0)
        assertTrue(types and AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED != 0)
        assertTrue(types and AccessibilityEvent.TYPE_VIEW_SCROLLED != 0)
    }

    @Test
    fun turningFilterOffGoesBackToMinimum() {
        assertEquals(ServiceEventTypes.MINIMAL, ServiceEventTypes.forShortFilter(false))
        assertEquals(ServiceEventTypes.WITH_SHORT_FILTER, ServiceEventTypes.forShortFilter(true))
    }
}
