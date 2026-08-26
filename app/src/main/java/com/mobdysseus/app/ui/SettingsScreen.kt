package com.mobdysseus.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobdysseus.app.provider.LocalModelPresets
import com.mobdysseus.app.provider.ProviderAdapter
import com.mobdysseus.app.provider.ProviderConfig
import com.mobdysseus.app.provider.ProviderPresets
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(config: ProviderConfig, onSave: (ProviderConfig) -> Unit) {
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var model by remember { mutableStateOf(config.model) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var useLocal by remember { mutableStateOf(config.useLocal) }
    var localRepoId by remember { mutableStateOf(config.localRepoId) }
    var localFile by remember { mutableStateOf(config.localFile) }
    val scope = rememberCoroutineScope()
    var testStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Model source", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(
                selected = useLocal,
                onClick = { useLocal = true },
                label = { Text("On-device") },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = !useLocal,
                onClick = { useLocal = false },
                label = { Text("Cloud API") },
            )
        }

        if (useLocal) {
            Spacer(Modifier.height(16.dp))
            Text("On-device model (GGUF)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Runs fully offline via llama.cpp. The first message downloads the " +
                    "model from Hugging Face, then it is cached.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LocalModelPresets.all.forEach { preset ->
                val selected = preset.repoId == localRepoId
                FilterChip(
                    selected = selected,
                    onClick = {
                        localRepoId = preset.repoId
                        localFile = preset.filename
                    },
                    label = { Text(preset.label) },
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        } else {
            Spacer(Modifier.height(16.dp))
            Text("Provider", style = MaterialTheme.typography.titleLarge)
            Column {
                ProviderPresets.all.forEach { preset ->
                    TextButton(onClick = {
                        baseUrl = preset.baseUrl
                        model = preset.defaultModel
                    }) { Text(preset.label) }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    testStatus = "Testing…"
                    val cfg = ProviderConfig(
                        baseUrl = baseUrl.trim(),
                        model = model.trim(),
                        apiKey = apiKey.trim(),
                        useLocal = false,
                        localRepoId = localRepoId,
                        localFile = localFile,
                    )
                    scope.launch {
                        testStatus = if (ProviderAdapter(cfg).healthCheck()) "Reachable" else "Unreachable"
                    }
                },
            ) { Text("Test connection") }
            testStatus?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Connection: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it == "Reachable") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onSave(
                    ProviderConfig(
                        baseUrl = baseUrl.trim(),
                        model = model.trim(),
                        apiKey = apiKey.trim(),
                        useLocal = useLocal,
                        localRepoId = localRepoId,
                        localFile = localFile,
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }

        Spacer(Modifier.height(32.dp))
        Text("About", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Mobdysseus v0.7.1\n" +
                "A mobile rebuild of the Odysseus self-hosted AI workspace, " +
                "optimised for Samsung Galaxy S25.\n\n" +
                "Runs standalone on-device, and can connect to a self-hosted " +
                "server for local LLMs and MCP tools.\n\n" +
                "Licensed AGPL-3.0-or-later. See THIRD_PARTY_NOTICES.md for " +
                "attributions. This is a community build and is not affiliated " +
                "with the upstream Odysseus project.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
