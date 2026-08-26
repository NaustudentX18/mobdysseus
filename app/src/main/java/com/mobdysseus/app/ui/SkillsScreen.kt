package com.mobdysseus.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobdysseus.app.data.Skill
import com.mobdysseus.app.data.SkillsStore

private val CATALOG = listOf(
    Skill(
        id = "ask-your-data",
        name = "Ask Your Data",
        description = "On-device RAG over all your notes, docs and photos.",
        category = "Intelligence",
        status = "available",
    ),
    Skill(
        id = "privacy-verdict",
        name = "Privacy Verdict",
        description = "Audit trail proving what stays on-device.",
        category = "Privacy",
        status = "available",
    ),
    Skill(
        id = "one-tap-capture",
        name = "One-Tap Capture",
        description = "Voice, clipboard or photo straight into a note.",
        category = "Capture",
        status = "available",
    ),
    Skill(
        id = "photo-ocr",
        name = "Photo OCR Search",
        description = "Search text inside your photos, fully offline.",
        category = "Search",
        status = "available",
    ),
    Skill(
        id = "knowledge-graph",
        name = "Mnemosyne",
        description = "A browsable graph of your life.",
        category = "Memory",
        status = "coming_soon",
    ),
    Skill(
        id = "email-triage",
        name = "Private Inbox",
        description = "Offline email and meeting triage.",
        category = "Productivity",
        status = "coming_soon",
    ),
    Skill(
        id = "voice-assistant",
        name = "Private Voice",
        description = "Wake-word on-device voice assistant.",
        category = "Voice",
        status = "coming_soon",
    ),
    Skill(
        id = "offline-translate",
        name = "Offline Translation",
        description = "Translate text without a network connection.",
        category = "Language",
        status = "coming_soon",
    ),
    Skill(
        id = "home-automation",
        name = "Home Automation",
        description = "Natural-language SmartThings control.",
        category = "Home",
        status = "coming_soon",
    ),
    Skill(
        id = "code-assistant",
        name = "Codex-lite",
        description = "On-device code assistant.",
        category = "Developer",
        status = "coming_soon",
    ),
    Skill(
        id = "daily-brief",
        name = "Daily Brief",
        description = "A private morning summary.",
        category = "Productivity",
        status = "coming_soon",
    ),
    Skill(
        id = "spaced-repetition",
        name = "Spaced Repetition",
        description = "Remember what matters.",
        category = "Memory",
        status = "coming_soon",
    ),
)

@Composable
fun SkillsScreen(skillsStore: SkillsStore) {
    var installed by remember { mutableStateOf(skillsStore.loadInstalled()) }

    fun install(skill: Skill) {
        skillsStore.setInstalled(skill.id, true)
        installed = installed + skill.id
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Skills",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                "Capabilities you can enable. Everything runs on-device and stays private.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(CATALOG, key = { it.id }) { skill ->
            SkillCard(
                skill = skill,
                isInstalled = skill.id in installed,
                onInstall = { install(skill) },
            )
        }
    }
}

@Composable
private fun SkillCard(skill: Skill, isInstalled: Boolean, onInstall: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        skill.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    CategoryTag(skill.category)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    skill.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            when {
                isInstalled -> {
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text("Installed")
                    }
                }
                skill.status == "coming_soon" -> {
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text("Roadmap")
                    }
                }
                else -> {
                    Button(onClick = onInstall) {
                        Text("Install")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTag(category: String) {
    Text(
        category,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
