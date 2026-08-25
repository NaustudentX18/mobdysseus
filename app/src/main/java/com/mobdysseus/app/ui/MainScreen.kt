package com.mobdysseus.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mobdysseus.app.data.NotesStore
import com.mobdysseus.app.provider.ProviderAdapter
import com.mobdysseus.app.provider.ProviderStore

enum class Tab { Chat, Notes, Settings }

@Composable
fun MainScreen(providerStore: ProviderStore, notesStore: NotesStore) {
    var config by remember { mutableStateOf(providerStore.load()) }
    var tab by rememberSaveable { mutableStateOf(Tab.Chat) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Chat,
                    onClick = { tab = Tab.Chat },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Chat") },
                    label = { Text("Chat") },
                )
                NavigationBarItem(
                    selected = tab == Tab.Notes,
                    onClick = { tab = Tab.Notes },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = "Notes") },
                    label = { Text("Notes") },
                )
                NavigationBarItem(
                    selected = tab == Tab.Settings,
                    onClick = { tab = Tab.Settings },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.Chat -> ChatScreen(ProviderAdapter(config))
                Tab.Notes -> NotesScreen(notesStore)
                Tab.Settings -> SettingsScreen(config) { newCfg ->
                    config = newCfg
                    providerStore.save(newCfg)
                }
            }
        }
    }
}
