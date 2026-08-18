package com.jakemalby.odysseusmobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.ui.graphics.vector.ImageVector

/** Stable app-shell contract; feature implementations do not navigate to each other. */
enum class Destination(val label: String, val icon: ImageVector) {
    CHAT("Chat", Icons.Outlined.AutoAwesome),
    COOKBOOK("Cookbook", Icons.Outlined.MenuBook),
    BRAIN("Brain", Icons.Outlined.Psychology),
    NOTES("Notes", Icons.Outlined.Notes),
    TASKS("Tasks", Icons.Outlined.CheckCircle),
    MORE("More", Icons.Outlined.MoreHoriz),
}
