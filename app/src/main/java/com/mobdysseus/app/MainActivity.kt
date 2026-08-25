package com.mobdysseus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mobdysseus.app.data.NotesStore
import com.mobdysseus.app.provider.ProviderStore
import com.mobdysseus.app.theme.MobdysseusTheme
import com.mobdysseus.app.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val providerStore = ProviderStore(this)
        val notesStore = NotesStore(this)
        setContent {
            MobdysseusTheme {
                MainScreen(providerStore, notesStore)
            }
        }
    }
}
