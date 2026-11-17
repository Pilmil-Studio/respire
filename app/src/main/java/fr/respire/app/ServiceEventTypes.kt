package fr.respire.app

import android.view.accessibility.AccessibilityEvent

/**
 * Types d'événements d'accessibilité écoutés par [PauseService].
 *
 * Les pauses n'ont besoin que des changements de fenêtre. Les changements de contenu et les
 * défilements ne servent qu'au filtre Reels / Shorts : on ne les écoute que lorsqu'il est activé.
 */
internal object ServiceEventTypes {

    const val MINIMAL = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

    const val WITH_SHORT_FILTER = MINIMAL or
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
        AccessibilityEvent.TYPE_VIEW_SCROLLED

    fun forShortFilter(enabled: Boolean): Int = if (enabled) WITH_SHORT_FILTER else MINIMAL
}
