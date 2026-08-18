package com.jakemalby.odysseusmobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jakemalby.odysseusmobile.core.Conversation
import com.jakemalby.odysseusmobile.core.Message
import com.jakemalby.odysseusmobile.core.Workspace
import com.jakemalby.odysseusmobile.core.voice.OfflineDictationAvailability
import com.jakemalby.odysseusmobile.core.voice.SafeTextSpeaker
import com.jakemalby.odysseusmobile.core.voice.SpeechPlaybackState
import com.jakemalby.odysseusmobile.core.voice.VoiceDraftPolicy
import com.jakemalby.odysseusmobile.core.voice.detectOfflineDictationAvailability
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ChatScreen(workspace: Workspace, update: ((Workspace) -> Workspace) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = LocalClipboardManager.current
    val runtime = remember { LocalModelRuntime(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var playbackState by remember { mutableStateOf(SpeechPlaybackState.INITIALIZING) }
    var spokenMessageId by remember { mutableStateOf<String?>(null) }
    val speaker = remember(context) {
        SafeTextSpeaker(context) { state ->
            playbackState = state
            if (state != SpeechPlaybackState.SPEAKING) spokenMessageId = null
        }
    }
    var generationJob by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(runtime) { onDispose { generationJob?.cancel(); runtime.close() } }
    DisposableEffect(lifecycleOwner, speaker) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) speaker.stop() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); speaker.close() }
    }
    var prompt by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var exportStatus by remember { mutableStateOf("") }
    val dictationAvailability = remember(context) { detectOfflineDictationAvailability(context) }
    var dictationStatus by remember(dictationAvailability) { mutableStateOf(dictationAvailability.label) }
    val dictationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { transcript ->
            prompt = VoiceDraftPolicy.appendTranscript(prompt, transcript)
            dictationStatus = "Transcript added to draft — review it before sending"
        }
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) launchDictation(dictationLauncher) }
    val active = workspace.conversations.firstOrNull { it.id == workspace.activeConversationId } ?: workspace.conversations.first()
    val visibleMessages = remember(active.messages, searchQuery) { filterChatMessages(active.messages, searchQuery) }
    val shareMessage: (Message) -> Unit = { message ->
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Mobdysseus conversation")
                    putExtra(Intent.EXTRA_TEXT, message.text)
                },
                "Share response",
            ),
        )
    }
    val generateReply: (String, String, String) -> Unit = { clean, conversationId, replyId ->
        generationJob?.cancel()
        generationJob = scope.launch {
            val assembler = StreamingTextAssembler()
            try {
                runtime.streamReply("${localRetrievalContext(clean, workspace)}\n\nUser request: $clean").collect { chunk ->
                    updateReply(update, conversationId, replyId, assembler.accept(chunk))
                }
                if (assembler.value().isBlank()) error("The local model returned an empty response.")
            } catch (cancelled: CancellationException) {
                if (assembler.value().isBlank()) updateReply(update, conversationId, replyId, "Generation stopped.")
                throw cancelled
            } catch (failure: Throwable) {
                updateReply(update, conversationId, replyId, nativeReply(clean, workspace.settings.selectedRecipe, failure.message))
            } finally {
                generationJob = null
            }
        }
    }
    val exportConversation = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        uri?.let { destination ->
            exportStatus = runCatching {
                context.contentResolver.openOutputStream(destination, "wt")?.bufferedWriter()?.use { writer -> writer.write(conversationMarkdown(active)) }
                    ?: error("Android could not open the destination.")
                "Conversation exported"
            }.getOrElse { "Export failed: ${it.message}" }
        }
    }
    pendingDeleteId?.let { conversationId ->
        val conversation = workspace.conversations.firstOrNull { it.id == conversationId }
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete conversation?") },
            text = { Text("${conversation?.title?.ifBlank { "Untitled" } ?: "This conversation"} and all of its messages will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    update { state ->
                        val remaining = state.conversations.filterNot { it.id == conversationId }
                        if (remaining.isEmpty()) state else state.copy(
                            conversations = remaining,
                            activeConversationId = if (state.activeConversationId == conversationId) remaining.first().id else state.activeConversationId,
                        )
                    }
                    pendingDeleteId = null
                }) { Text("Delete", color = Coral) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") } },
        )
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                OutlinedTextField(active.title, { title -> update { state -> state.copy(conversations = state.conversations.map { if (it.id == active.id) it.copy(title = title.take(80)) else it }) } }, Modifier.fillMaxWidth(), label = { Text("Conversation") }, singleLine = true)
                Text("${active.messages.size} messages · ${workspace.settings.selectedRecipe}", color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = { exportConversation.launch("${active.title.ifBlank { "mobdysseus-chat" }.replace(Regex("[^A-Za-z0-9._-]"), "-")}.md") }) { Icon(Icons.Outlined.Download, "Export conversation", tint = Muted) }
            if (workspace.conversations.size > 1) IconButton(onClick = { pendingDeleteId = active.id }) { Icon(Icons.Outlined.Delete, "Delete conversation", tint = Coral) }
            OutlinedButton(onClick = { val chat = Conversation(UUID.randomUUID().toString(), "New workspace", emptyList()); update { it.copy(conversations = listOf(chat) + it.conversations, activeConversationId = chat.id) } }) { Text("New") }
        }
        if (exportStatus.isNotBlank()) Text(exportStatus, color = Success, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { workspace.conversations.forEach { conversation -> val selected = conversation.id == active.id; OutlinedButton(onClick = { update { it.copy(activeConversationId = conversation.id) } }, border = BorderStroke(1.dp, if (selected) Coral else Border)) { Text(conversation.title.ifBlank { "Untitled" }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selected) Coral else Ink) } } }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("Search this chat") },
            supportingText = if (searchQuery.isBlank()) null else {{ Text("${visibleMessages.size} matching messages") }},
            singleLine = true,
        )
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 18.dp)) {
            items(visibleMessages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    isSpeaking = spokenMessageId == message.id && playbackState == SpeechPlaybackState.SPEAKING,
                    onSpeak = if (message.mine) null else {{ if (speaker.speak(message.text, message.id)) spokenMessageId = message.id }},
                    onStopSpeaking = speaker::stop,
                    onCopy = { clipboard.setText(AnnotatedString(message.text)) },
                    onShare = { shareMessage(message) },
                    onRetry = if (message.mine || generationJob?.isActive == true) null else retryPromptFor(active.messages, message.id)?.let { retryPrompt ->
                        {
                            updateReply(update, active.id, message.id, "Thinking locally…")
                            generateReply(retryPrompt, active.id, message.id)
                        }
                    },
                )
            }
            if (active.messages.isEmpty()) item { EmptyCard("Start a private conversation", "Choose a recipe in Cookbook then write your first message.") }
            else if (visibleMessages.isEmpty()) item { EmptyCard("No matching messages", "Try a different search phrase.") }
        }
        Text(
            dictationStatus,
            color = if (dictationAvailability == OfflineDictationAvailability.UNAVAILABLE) Coral else Muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(prompt, { prompt = it }, Modifier.weight(1f).heightIn(min = 58.dp), placeholder = { Text("Message Mobdysseus…") }, maxLines = 5)
            IconButton(
                onClick = { if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) launchDictation(dictationLauncher) else microphonePermission.launch(Manifest.permission.RECORD_AUDIO) },
                modifier = Modifier.size(56.dp).background(PanelRaised, RoundedCornerShape(18.dp)),
                enabled = dictationAvailability != OfflineDictationAvailability.UNAVAILABLE,
            ) { Icon(Icons.Outlined.Mic, "Dictate into draft for review", tint = Ink) }
            if (generationJob?.isActive == true) {
                OutlinedButton(
                    onClick = { generationJob?.cancel(); generationJob = null },
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { Text("Stop") }
            } else {
                IconButton(onClick = {
                    val clean = prompt.trim()
                    if (clean.isBlank()) return@IconButton
                    val conversationId = active.id
                    val user = Message(UUID.randomUUID().toString(), "You", clean, true, System.currentTimeMillis())
                    val replyId = UUID.randomUUID().toString()
                    val pendingReply = Message(replyId, "Mobdysseus", "Thinking locally…", false, System.currentTimeMillis())
                    update { state -> state.copy(conversations = state.conversations.map { conversation ->
                        if (conversation.id == conversationId) conversation.copy(
                            title = if (conversation.messages.isEmpty()) clean.take(34) else conversation.title,
                            messages = conversation.messages + user + pendingReply,
                        ) else conversation
                    }) }
                    prompt = ""
                    generateReply(clean, conversationId, replyId)
                }, Modifier.size(56.dp).background(Coral, RoundedCornerShape(18.dp))) { Icon(Icons.Outlined.Send, "Send", tint = Obsidian) }
            }
        }
    }
}

private fun updateReply(
    update: (((Workspace) -> Workspace) -> Unit),
    conversationId: String,
    replyId: String,
    text: String,
) = update { state ->
    state.copy(conversations = state.conversations.map { conversation ->
        if (conversation.id == conversationId) conversation.copy(messages = conversation.messages.map { message ->
            if (message.id == replyId) message.copy(text = text) else message
        }) else conversation
    })
}

private fun localRetrievalContext(prompt: String, workspace: Workspace): String {
    val terms = prompt.lowercase().split(Regex("[^a-z0-9]+"))
        .filter { it.length > 2 }.toSet()
    fun score(text: String) = terms.count { term -> text.contains(term, ignoreCase = true) }
    val notes = workspace.notes.map { it to score("${it.title} ${it.body}") }
        .filter { it.second > 0 }.sortedByDescending { it.second }.take(3)
        .map { "NOTE — ${it.first.title}: ${it.first.body.take(1200)}" }
    val memories = workspace.memories.map { it to score(it.text) }
        .filter { it.second > 0 }.sortedByDescending { it.second }.take(3)
        .map { "MEMORY — ${it.first.text}" }
    val tasks = workspace.tasks.filter { !it.done && score(it.title) > 0 }
        .take(3).map { "OPEN TASK — ${it.title}" }
    return (notes + memories + tasks).takeIf { it.isNotEmpty() }
        ?.joinToString("\n", prefix = "Private local context (use only if relevant):\n")
        ?: "No matching private workspace context was found."
}
private fun launchDictation(launcher: ActivityResultLauncher<Intent>) { launcher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Mobdysseus"); putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) }) }
private fun nativeReply(prompt: String, recipe: String, error: String? = null): String = when { !error.isNullOrBlank() -> "Local inference is not ready: $error"; prompt.contains("help", true) -> "I’m ready in $recipe mode. Your local modules are active; model execution will be added through the on-device runtime adapter."; prompt.contains("task", true) -> "I can turn that into a task from the Tasks module. Your workspace data stays on this phone."; else -> "Saved in your native Mobdysseus workspace. Select a downloadable on-device model in Cookbook to enable full local inference." }
@Composable
private fun ChatBubble(
    message: Message,
    isSpeaking: Boolean = false,
    onSpeak: (() -> Unit)? = null,
    onStopSpeaking: () -> Unit = {},
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    val blocks = remember(message.text) { SafeMarkdownParser.parse(message.text) }
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (message.mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Card(
            Modifier.widthIn(max = 360.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.mine) Color(0xFF422B32) else Panel,
            ),
            border = BorderStroke(1.dp, Border),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        message.author.uppercase(),
                        color = if (message.mine) Coral else Muted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.createdAt)),
                        color = Muted,
                        fontSize = 10.sp,
                    )
                    onSpeak?.let { speak ->
                        IconButton(onClick = if (isSpeaking) onStopSpeaking else speak, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (isSpeaking) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                if (isSpeaking) "Stop reading response" else "Read response aloud",
                                tint = Muted,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
                Column(
                    Modifier.padding(top = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    blocks.forEach { block ->
                        when (block) {
                            is SafeMarkdownBlock.Plain -> Text(block.text, lineHeight = 21.sp)
                            is SafeMarkdownBlock.Code -> SafeCodeBlock(block)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onCopy) { Text("Copy") }
                    TextButton(onClick = onShare) { Text("Share") }
                    onRetry?.let { retry -> TextButton(onClick = retry) { Text("Retry") } }
                }
            }
        }
    }
}

@Composable
private fun SafeCodeBlock(block: SafeMarkdownBlock.Code) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Obsidian, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        block.language?.let {
            Text(
                it.uppercase(),
                color = Muted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Text(
            block.text,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            color = Ink,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun conversationMarkdown(conversation: Conversation): String = buildString {
    append("# ").append(conversation.title.ifBlank { "Mobdysseus conversation" }).append("\n\n")
    conversation.messages.forEach { message ->
        append("## ").append(message.author).append(" · ")
        append(DateFormat.getDateTimeInstance().format(Date(message.createdAt))).append("\n\n")
        append(message.text).append("\n\n")
    }
}
