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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.mikepenz.markdown.m3.Markdown
import com.mobdysseus.app.data.Document
import com.mobdysseus.app.data.DocumentsStore

@Composable
fun DocumentsScreen(store: DocumentsStore) {
    var documents by remember { mutableStateOf(store.load()) }
    var editing by remember { mutableStateOf<Document?>(null) }
    var showNew by remember { mutableStateOf(false) }

    fun persist(list: List<Document>) {
        documents = list
        store.save(list)
    }

    if (showNew || editing != null) {
        DocumentEditor(
            initial = editing ?: Document(store.newId(), "", "", System.currentTimeMillis()),
            onSave = { d ->
                if (editing != null) {
                    persist(documents.map { if (it.id == d.id) d else it })
                } else {
                    persist(listOf(d) + documents)
                }
                editing = null
                showNew = false
            },
            onCancel = {
                editing = null
                showNew = false
            },
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showNew = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New document")
            }
        },
    ) { padding ->
        if (documents.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No documents yet.\nTap + to start writing.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(documents, key = { it.id }) { doc ->
                    DocumentRow(
                        doc = doc,
                        onClick = { editing = doc },
                        onDelete = { persist(documents.filter { it.id != doc.id }) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(doc: Document, onClick: () -> Unit, onDelete: () -> Unit) {
    androidx.compose.material3.Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (doc.title.isBlank()) "(untitled)" else doc.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (doc.body.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        doc.body.lines().firstOrNull() ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun DocumentEditor(initial: Document, onSave: (Document) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var body by remember { mutableStateOf(initial.body) }
    var preview by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Text(
                "Document",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                onSave(initial.copy(title = title, body = body, updatedAt = System.currentTimeMillis()))
            }) { Text("Save") }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(
                selected = !preview,
                onClick = { preview = false },
                label = { Text("Edit") },
            )
            Spacer(Modifier.padding(4.dp))
            FilterChip(
                selected = preview,
                onClick = { preview = true },
                label = { Text("Preview") },
            )
        }

        Spacer(Modifier.height(8.dp))
        if (preview) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Markdown(content = body.ifBlank { "*Nothing to preview yet.*" })
            }
        } else {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxSize(),
                placeholder = { Text("Write in Markdown…") },
            )
        }
    }
}
