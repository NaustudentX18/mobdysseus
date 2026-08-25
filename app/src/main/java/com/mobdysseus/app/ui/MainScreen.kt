package com.mobdysseus.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mobdysseus.app.cookbook.HardwareDetector
import com.mobdysseus.app.data.ChatStore
import com.mobdysseus.app.data.DocumentsStore
import com.mobdysseus.app.data.NotesStore
import com.mobdysseus.app.data.TasksStore
import com.mobdysseus.app.provider.ProviderAdapter
import com.mobdysseus.app.provider.ProviderStore
import kotlinx.coroutines.launch

enum class Tab { Chat, Notes, Documents, Tasks, Cookbook, Settings }

private data class Section(val tab: Tab, val label: String, val icon: ImageVector)

private val sections = listOf(
    Section(Tab.Chat, "Chat", Icons.Filled.Home),
    Section(Tab.Notes, "Notes", Icons.Filled.Edit),
    Section(Tab.Documents, "Documents", Icons.AutoMirrored.Filled.List),
    Section(Tab.Tasks, "Tasks", Icons.Filled.Check),
    Section(Tab.Cookbook, "Cookbook", Icons.Filled.Build),
    Section(Tab.Settings, "Settings", Icons.Filled.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    providerStore: ProviderStore,
    notesStore: NotesStore,
    tasksStore: TasksStore,
    chatStore: ChatStore,
    documentsStore: DocumentsStore,
) {
    var config by remember { mutableStateOf(providerStore.load()) }
    var tab by rememberSaveable { mutableStateOf(Tab.Chat) }
    val context = LocalContext.current
    val hardware = remember { HardwareDetector.detect(context) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Mobdysseus",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                )
                sections.forEach { section ->
                    NavigationDrawerItem(
                        label = { Text(section.label) },
                        selected = tab == section.tab,
                        onClick = {
                            tab = section.tab
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(section.icon, contentDescription = section.label) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(sections.first { it.tab == tab }.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    Tab.Chat -> ChatScreen(ProviderAdapter(config), chatStore)
                    Tab.Notes -> NotesScreen(notesStore)
                    Tab.Documents -> DocumentsScreen(documentsStore)
                    Tab.Tasks -> TasksScreen(tasksStore)
                    Tab.Cookbook -> CookbookScreen(hardware)
                    Tab.Settings -> SettingsScreen(config) { newCfg ->
                        config = newCfg
                        providerStore.save(newCfg)
                    }
                }
            }
        }
    }
}
