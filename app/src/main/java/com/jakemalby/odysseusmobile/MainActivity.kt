package com.jakemalby.odysseusmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jakemalby.odysseusmobile.core.Workspace
import com.jakemalby.odysseusmobile.core.seedWorkspace
import com.jakemalby.odysseusmobile.core.theme.AppTheme
import com.jakemalby.odysseusmobile.navigation.Destination
import com.jakemalby.odysseusmobile.persistence.WorkspacePersistenceController
import com.jakemalby.odysseusmobile.platform.task.AndroidTaskReminderScheduler
import com.jakemalby.odysseusmobile.ui.AdaptiveNavigation
import com.jakemalby.odysseusmobile.ui.MobdysseusAdaptiveNavigation
import com.jakemalby.odysseusmobile.ui.MobdysseusAppTheme
import com.jakemalby.odysseusmobile.ui.MobdysseusThemeColors
import com.jakemalby.odysseusmobile.ui.adaptiveNavigationFor
import com.jakemalby.odysseusmobile.ui.mobdysseusImeSafe

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { MobdysseusApp() }
    }
}

internal val Obsidian = Color(0xFF111318)
internal val Panel = Color(0xFF1B1E25)
internal val PanelRaised = Color(0xFF242833)
internal val Border = Color(0xFF343946)
internal val Coral = Color(0xFFE06C75)
internal val Ink = Color(0xFFE7E9F0)
internal val Muted = Color(0xFFABB1C0)
internal val Success = Color(0xFF7BC99A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobdysseusApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val persistence = remember { WorkspacePersistenceController(context) }
    val reminderScheduler = remember(context) { AndroidTaskReminderScheduler(context) }
    var workspace by remember { mutableStateOf<Workspace?>(null) }
    var loadFailure by remember { mutableStateOf<Throwable?>(null) }
    var loadAttempt by remember { mutableStateOf(0) }
    val saveFailure by persistence.error.collectAsState()
    var destination by rememberSaveable { mutableStateOf(Destination.CHAT) }
    DisposableEffect(persistence) { onDispose { persistence.close() } }
    LaunchedEffect(loadAttempt) {
        loadFailure = null
        runCatching { persistence.initialize() }
            .onSuccess { workspace = it }
            .onFailure { loadFailure = it }
    }
    fun update(change: (Workspace) -> Workspace) {
        val current = workspace ?: return
        val changed = change(current)
        workspace = changed
        persistence.enqueueSave(changed)
    }

    val activeTheme = AppTheme.fromId(workspace?.settings?.theme)

    MobdysseusAppTheme(theme = activeTheme) {
        val colors = MobdysseusThemeColors.current
        val current = workspace
        if (current == null) {
            Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("MOBDYSSEUS", color = colors.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        val failure = loadFailure
                        if (failure == null) {
                            Text("Opening encrypted workspace…", color = colors.onSurfaceVariant)
                        } else {
                            Text("The encrypted workspace could not be opened.", color = colors.onBackground)
                            Text(failure.message ?: "Unknown storage error", color = colors.primary, fontSize = 12.sp)
                            Button(onClick = { loadAttempt += 1 }) { Text("Retry") }
                        }
                    }
                }
            }
        } else {
            val widthDp = LocalConfiguration.current.screenWidthDp
            val useRail = adaptiveNavigationFor(widthDp) == AdaptiveNavigation.NAVIGATION_RAIL
            val topBar: @Composable () -> Unit = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("◢", color = colors.primary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text(" MOBDYSSEUS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    },
                    actions = {
                        Text(
                            if (saveFailure != null) "SAVE ERROR" else if (current.settings.localOnly) "LOCAL" else "HYBRID",
                            color = colors.primary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 18.dp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background,
                        titleContentColor = colors.onBackground,
                    ),
                )
            }
            val content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit = { padding ->
                AnimatedContent(destination, modifier = Modifier.padding(padding).mobdysseusImeSafe(), label = "native-workspace") { page ->
                    when (page) {
                        Destination.CHAT -> ChatScreen(current, ::update)
                        Destination.COOKBOOK -> CookbookScreen(current, ::update)
                        Destination.BRAIN -> BrainScreen(current, ::update)
                        Destination.NOTES -> NotesScreen(current, ::update)
                        Destination.TASKS -> TasksScreen(current, ::update, reminderScheduler)
                        Destination.MORE -> MoreScreen(current, ::update) {
                            val reset = seedWorkspace()
                            workspace = reset
                            persistence.enqueueSave(reset)
                        }
                    }
                }
            }
            if (useRail) {
                Scaffold(containerColor = colors.background, topBar = topBar) { padding ->
                    Row(Modifier.fillMaxSize()) {
                        MobdysseusAdaptiveNavigation(destination, { destination = it }, Modifier.fillMaxHeight())
                        Box(Modifier.weight(1f)) { content(padding) }
                    }
                }
            } else {
                Scaffold(
                    containerColor = colors.background,
                    topBar = topBar,
                    bottomBar = { MobdysseusAdaptiveNavigation(destination, { destination = it }) },
                ) { padding -> content(padding) }
            }
        }
    }
}

@Composable
internal fun SimpleRow(text: String, onDelete: () -> Unit) {
    val colors = MobdysseusThemeColors.current
    Card(colors = CardDefaults.cardColors(containerColor = colors.surface), border = BorderStroke(1.dp, colors.border)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 5.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), color = colors.onSurface)
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete", tint = colors.onSurfaceVariant) }
        }
    }
}

@Composable
internal fun EmptyCard(title: String, detail: String) {
    val colors = MobdysseusThemeColors.current
    Surface(color = colors.surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, colors.border)) {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(detail, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
        }
    }
}
