package com.jakemalby.odysseusmobile.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jakemalby.odysseusmobile.navigation.Destination

/** Material window-size classes used to pick the S25 adaptive navigation. */
enum class MobdysseusWindowSize { COMPACT, MEDIUM, EXPANDED }

/** Which navigation surface to render for a given width. */
enum class AdaptiveNavigation { BOTTOM_BAR, NAVIGATION_RAIL }

/**
 * Pure, JVM-testable decision: compact phones use a bottom bar; medium/expanded
 * (landscape, tablets, multi-window) use a navigation rail.
 */
fun adaptiveNavigationFor(widthDp: Int): AdaptiveNavigation = when {
    widthDp >= 600 -> AdaptiveNavigation.NAVIGATION_RAIL
    else -> AdaptiveNavigation.BOTTOM_BAR
}

fun windowSizeFor(widthDp: Int): MobdysseusWindowSize = when {
    widthDp >= 840 -> MobdysseusWindowSize.EXPANDED
    widthDp >= 600 -> MobdysseusWindowSize.MEDIUM
    else -> MobdysseusWindowSize.COMPACT
}

/**
 * S25-safe adaptive navigation. Renders a bottom bar on compact widths and a
 * navigation rail on medium/expanded widths, with TalkBack-friendly labels.
 */
@Composable
fun MobdysseusAdaptiveNavigation(
    selected: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val navigation = adaptiveNavigationFor(maxWidth.roundToDp())
        when (navigation) {
            AdaptiveNavigation.BOTTOM_BAR -> BottomBar(selected, onSelect)
            AdaptiveNavigation.NAVIGATION_RAIL -> NavigationRail(selected, onSelect)
        }
    }
}

@Composable
private fun BottomBar(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        Destination.entries.forEach { item ->
            NavigationBarItem(
                selected = selected == item,
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
private fun NavigationRail(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        Destination.entries.forEach { item ->
            NavigationRailItem(
                selected = selected == item,
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

private fun androidx.compose.ui.unit.Dp.roundToDp(): Int = (value + 0.5f).toInt()
