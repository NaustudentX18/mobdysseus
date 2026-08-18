package com.jakemalby.odysseusmobile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jakemalby.odysseusmobile.core.Workspace
import java.io.File
import kotlinx.coroutines.launch

private data class Recipe(
    val name: String,
    val subtitle: String,
    val description: String,
    val profileId: String? = null,
)

private val recipes = listOf(
    Recipe("Private quick chat", "Gemma 3 1B-class", "Fast private chat and summaries.", "private-quick-chat"),
    Recipe("Private document and vision work", "Gemma 3n E2B-class", "Higher quality document, vision and audio work.", "private-multimodal"),
    Recipe("Deep private work", "Gemma 3n E4B-class", "The largest built-in S25 profile for complex local work.", "deep-private-work"),
    Recipe("Document companion", "Local RAG", "Ask questions over files stored in your workspace."),
    Recipe("Voice capture", "Speech → note/task", "Turn a spoken thought into a note, memory, task, or chat."),
)

@Composable
internal fun CookbookScreen(workspace: Workspace, update: ((Workspace) -> Workspace) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val runtime = remember { LocalModelRuntime(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var installedModels by remember { mutableStateOf(runtime.installedModels()) }
    var selectedModel by remember { mutableStateOf(runtime.selectedModel()) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }
    var modelStatus by remember {
        mutableStateOf(if (installedModels.isEmpty()) "No local model installed" else "${installedModels.size} local model(s) installed")
    }
    val deviceFacts = remember { runCatching(runtime::deviceSummary).getOrNull() }

    fun remove(model: File) {
        modelStatus = runtime.removeModel(model).fold(
            onSuccess = {
                installedModels = runtime.installedModels()
                selectedModel = runtime.selectedModel()
                "Removed ${model.name}"
            },
            onFailure = { "Could not remove model: ${it.message}" },
        )
    }

    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selected ->
            scope.launch {
                modelStatus = "Checking space and importing…"
                modelStatus = runtime.importModel(selected).fold(
                    onSuccess = { file ->
                        installedModels = runtime.installedModels()
                        selectedModel = runtime.selectedModel()
                        "Installed and selected ${file.name}"
                    },
                    onFailure = { "Model import failed: ${it.message}" },
                )
            }
        }
    }

    pendingDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove active model?") },
            text = { Text("${model.name} is the model Chat currently uses. Removing it cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null; remove(model) }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Cookbook", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Native recipes sized for your phone. Nothing is downloaded automatically or sent to a server.",
                color = Muted,
                modifier = Modifier.padding(top = 5.dp, bottom = 8.dp),
            )
            Text(modelStatus, color = Success, fontSize = 12.sp)
        }
        deviceFacts?.let { facts -> item { DeviceFitCard(facts) } }
        items(recipes) { recipe ->
            val selected = workspace.settings.selectedRecipe == recipe.name
            val profile = recipe.profileId?.let { profileId -> deviceFacts?.profiles?.get(profileId) }
            Card(
                Modifier.fillMaxWidth().clickable {
                    update { it.copy(settings = it.settings.copy(selectedRecipe = recipe.name)) }
                },
                colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF33242A) else Panel),
                border = BorderStroke(1.dp, if (selected) Coral else Border),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Book, null, tint = Coral)
                        Text(recipe.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f))
                        if (selected) Text("ACTIVE", color = Coral, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text(recipe.subtitle, color = Ink, modifier = Modifier.padding(top = 10.dp), fontWeight = FontWeight.SemiBold)
                    profile?.let { ProfileFit(it, deviceFacts) }
                    Text(recipe.description, color = Muted, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Local model library", fontWeight = FontWeight.Bold)
                    Text(
                        "Import a .litertlm file through Android’s picker. Mobdysseus checks space, stages the full copy, then atomically activates it in app-private storage.",
                        color = Muted,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                    Button(
                        onClick = { modelPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text("Import .litertlm model") }
                }
            }
        }
        if (installedModels.isNotEmpty()) {
            item { Text("Installed models · tap to select", color = Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
            items(installedModels, key = File::getAbsolutePath) { model ->
                val active = selectedModel?.absolutePath == model.absolutePath
                Card(
                    Modifier.fillMaxWidth().clickable {
                        modelStatus = runtime.selectModel(model).fold(
                            onSuccess = { selectedModel = runtime.selectedModel(); "Selected ${model.name}" },
                            onFailure = { "Could not select model: ${it.message}" },
                        )
                    },
                    colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFF33242A) else Panel),
                    border = BorderStroke(1.dp, if (active) Coral else Border),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(model.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (active) Text("ACTIVE", color = Coral, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            Text("${formatBytes(model.length())} · app-private", color = Muted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { if (active) pendingDelete = model else remove(model) }) {
                            Icon(Icons.Outlined.Delete, "Remove model", tint = Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceFitCard(facts: ModelDeviceSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(16.dp)) {
            Text("This device", fontWeight = FontWeight.Bold)
            Text("${facts.manufacturer} ${facts.model} · Android API ${facts.apiLevel}", color = Ink)
            Text(
                "RAM ${formatBytes(facts.availableRamBytes)} available / ${formatBytes(facts.totalRamBytes)} total",
                color = Muted,
                fontSize = 12.sp,
            )
            Text(
                "Storage ${formatBytes(facts.availableStorageBytes)} free · ${facts.backends} backend",
                color = Muted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ProfileFit(profile: ModelProfileSummary, facts: ModelDeviceSummary?) {
    val (label, color) = if (facts == null) {
        "DEVICE CHECK UNAVAILABLE" to Muted
    } else if (profile.limitations.isEmpty()) {
        "READY ON THIS DEVICE" to Success
    } else {
        "CHECK ${profile.limitations.joinToString().uppercase()}" to Coral
    }
    Text(label, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp))
    Text(
        "Needs ${formatBytes(profile.requiredRamBytes)} available RAM · ${formatBytes(profile.requiredStorageBytes)} free" +
            if (profile.chargingRecommended) " · charging recommended" else "",
        color = Muted,
        fontSize = 12.sp,
    )
}

private fun formatBytes(bytes: Long): String {
    val gib = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return if (gib >= 1.0) "%.1f GB".format(gib) else "${bytes / (1024 * 1024)} MB"
}
