package com.jakemalby.odysseusmobile.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveNavigationTest {
    @Test
    fun compactPhonesUseBottomBar() {
        assertEquals(AdaptiveNavigation.BOTTOM_BAR, adaptiveNavigationFor(360))
        assertEquals(AdaptiveNavigation.BOTTOM_BAR, adaptiveNavigationFor(599))
        assertEquals(MobdysseusWindowSize.COMPACT, windowSizeFor(360))
    }

    @Test
    fun mediumWidthsUseNavigationRail() {
        assertEquals(AdaptiveNavigation.NAVIGATION_RAIL, adaptiveNavigationFor(600))
        assertEquals(AdaptiveNavigation.NAVIGATION_RAIL, adaptiveNavigationFor(720))
        assertEquals(MobdysseusWindowSize.MEDIUM, windowSizeFor(720))
    }

    @Test
    fun expandedWidthsUseNavigationRail() {
        assertEquals(AdaptiveNavigation.NAVIGATION_RAIL, adaptiveNavigationFor(840))
        assertEquals(AdaptiveNavigation.NAVIGATION_RAIL, adaptiveNavigationFor(1280))
        assertEquals(MobdysseusWindowSize.EXPANDED, windowSizeFor(1280))
    }
}
