package com.mobdysseus.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobdysseus.app.data.ChatStore
import com.mobdysseus.app.provider.ChatEngine
import com.mobdysseus.app.provider.ChatMessage
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(engine: ChatEngine, chatStore: ChatStore) {
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply { addAll(chatStore.load()) }
    }
    var input by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamingContent by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun send() {
        if (input.isBlank() || isStreaming) return
        val text = input.trim()
        messages.add(ChatMessage("user", text))
        chatStore.save(messages.toList())
        val history = messages.toList()
        input = ""
        isStreaming = true
        streamingContent = ""
        scope.launch {
            val sb = StringBuilder()
            try {
                engine.stream(history).collect { delta ->
                    sb.append(delta)
                    streamingContent = sb.toString()
                }
                // llmedge failures are emitted as a single error chunk, so they
                // surface here as a normal assistant message. Only persist a
                // message when something was actually produced.
                if (sb.isNotEmpty()) {
                    messages.add(ChatMessage("assistant", sb.toString()))
                    chatStore.save(messages.toList())
                }
            } catch (e: Exception) {
                messages.add(ChatMessage("assistant", "Error: " + (e.message ?: "unknown")))
                chatStore.save(messages.toList())
            } finally {
                isStreaming = false
                streamingContent = ""
            }
        }
    }

    LaunchedEffect(messages.size, streamingContent) {
        val total = messages.size + (if (streamingContent.isNotEmpty()) 1 else 0)
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Chat", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (messages.isNotEmpty()) {
                IconButton(onClick = {
                    messages.clear()
                    chatStore.clear()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear chat")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (messages.isEmpty() && streamingContent.isEmpty()) {
                item {
                    Text(
                        "Start a conversation.\nConfigure your provider in Settings first.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            items(messages) { msg ->
                MessageBubble(msg)
            }
            if (streamingContent.isNotEmpty()) {
                item { MessageBubble(ChatMessage("assistant", streamingContent)) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message…") },
                maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { send() }, enabled = input.isNotBlank() && !isStreaming) {
                if (isStreaming) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            if (isUser) {
                Text(
                    msg.content,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Markdown(
                    content = msg.content,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}
