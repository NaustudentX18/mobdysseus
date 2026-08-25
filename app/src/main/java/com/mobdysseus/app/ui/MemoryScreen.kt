package com.mobdysseus.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobdysseus.app.data.Memory
import com.mobdysseus.app.data.MemoryStore

@Composable
fun MemoryScreen(store: MemoryStore) {
    var memories by remember { mutableStateOf(store.load()) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Memory?>(null) }
    var showNew by remember { mutableStateOf(false) }

    fun persist(list: List<Memory>) {
        memories = list
        store.save(list)
    }

    val filtered = remember(memories, query) {
        if (query.isBlank()) {
            memories
        } else {
            memories.filter { m ->
                m.content.contains(query, ignoreCase = true) ||
                    m.tags.contains(query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showNew = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New memory")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
            )
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (memories.isEmpty()) {
                            "No memories yet.\nTap + to create one."
                        } else {
                            "No matches."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { memory ->
                        MemoryRow(
                            memory = memory,
                            onClick = { editing = memory },
                            onDelete = { persist(memories.filter { it.id != memory.id }) },
                        )
                    }
                }
            }
        }
    }

    if (showNew) {
        MemoryEditorDialog(
            initial = Memory(store.newId(), "", "", System.currentTimeMillis()),
            onDismiss = { showNew = false },
            onSave = { m ->
                persist(listOf(m) + memories)
                showNew = false
            },
        )
    }
    editing?.let { memory ->
        MemoryEditorDialog(
            initial = memory,
            onDismiss = { editing = null },
            onSave = { m ->
                persist(memories.map { if (it.id == m.id) m else it })
                editing = null
            },
        )
    }
}

@Composable
private fun MemoryRow(memory: Memory, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (memory.content.isBlank()) "(empty)" else memory.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                )
                if (memory.tags.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        memory.tags,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun MemoryEditorDialog(initial: Memory, onDismiss: () -> Unit, onSave: (Memory) -> Unit) {
    var content by remember { mutableStateOf(initial.content) }
    var tags by remember { mutableStateOf(initial.tags) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Memory") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(initial.copy(content = content, tags = tags))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
