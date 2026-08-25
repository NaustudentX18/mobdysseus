package com.mobdysseus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mobdysseus.app.data.ChatStore
import com.mobdysseus.app.data.NotesStore
import com.mobdysseus.app.data.TasksStore
import com.mobdysseus.app.provider.ProviderStore
import com.mobdysseus.app.theme.MobdysseusTheme
import com.mobdysseus.app.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val providerStore = ProviderStore(this)
        val notesStore = NotesStore(this)
        val tasksStore = TasksStore(this)
        val chatStore = ChatStore(this)
        setContent {
            MobdysseusTheme {
                MainScreen(providerStore, notesStore, tasksStore, chatStore)
            }
        }
    }
}
