package com.jakemalby.odysseusmobile.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared S25-safe UI modifiers. They deliberately contain no feature or persistence knowledge. */
internal fun Modifier.mobdysseusImeSafe(): Modifier = imePadding()

/** Use on custom icon/tap controls that do not receive Material's default 48dp target. */
internal fun Modifier.mobdysseusTouchTarget(): Modifier = sizeIn(minWidth = 48.dp, minHeight = 48.dp)
