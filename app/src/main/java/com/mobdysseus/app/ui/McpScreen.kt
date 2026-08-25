package com.mobdysseus.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobdysseus.app.data.McpServerStore
import com.mobdysseus.app.mcp.McpClient
import com.mobdysseus.app.mcp.McpServerConfig
import com.mobdysseus.app.mcp.McpTool
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun McpScreen(store: McpServerStore) {
    var servers by remember { mutableStateOf(store.load()) }
    var selected by remember { mutableStateOf<McpServerConfig?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<McpServerConfig?>(null) }

    fun persist(list: List<McpServerConfig>) {
        servers = list
        store.save(list)
    }

    if (showAdd || editing != null) {
        ServerEditorDialog(
            initial = editing ?: McpServerConfig(store.newId(), "", ""),
            onSave = { cfg ->
                if (editing != null) persist(servers.map { if (it.id == cfg.id) cfg else it })
                else persist(servers + cfg)
                editing = null
                showAdd = false
            },
            onDismiss = { editing = null; showAdd = false },
        )
    }

    if (selected != null) {
        ServerDetail(server = selected!!, onBack = { selected = null })
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add server")
                }
            },
        ) { padding ->
            if (servers.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "No MCP servers yet.\nAdd a server to use its tools.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(servers, key = { it.id }) { server ->
                        ServerRow(
                            server = server,
                            onClick = { selected = server },
                            onEdit = { editing = server },
                            onDelete = { persist(servers.filter { it.id != server.id }) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerRow(server: McpServerConfig, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(server.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    server.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun ServerEditorDialog(initial: McpServerConfig, onSave: (McpServerConfig) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var url by remember { mutableStateOf(initial.url) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.name.isBlank()) "Add MCP server" else "Edit MCP server") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(initial.copy(name = name.trim(), url = url.trim())) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ServerDetail(server: McpServerConfig, onBack: () -> Unit) {
    val client = remember(server) { McpClient(server) }
    var tools by remember { mutableStateOf<List<McpTool>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(server) {
        tools = null
        error = null
        try {
            tools = client.listTools()
        } catch (e: Exception) {
            error = e.message ?: "Failed to list tools"
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column {
                Text(server.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    server.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            tools == null -> CircularProgressIndicator(Modifier.padding(16.dp))
            tools!!.isEmpty() -> Text("No tools exposed by this server.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tools!!) { tool -> ToolRow(client, tool) }
            }
        }
    }
}

@Composable
private fun ToolRow(client: McpClient, tool: McpTool) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var runError by remember { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tool.name, style = MaterialTheme.typography.titleMedium)
                    if (tool.description.isNotBlank()) {
                        Text(
                            tool.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = {
                    expanded = true
                    running = true
                    result = ""
                    runError = null
                    scope.launch {
                        val sb = StringBuilder()
                        try {
                            client.callTool(tool.name, JSONObject()).collect { sb.append(it); result = sb.toString() }
                        } catch (e: Exception) {
                            runError = e.message ?: "Tool call failed"
                        } finally {
                            running = false
                        }
                    }
                }) { Text("Run") }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                when {
                    running -> CircularProgressIndicator(Modifier.padding(4.dp))
                    runError != null -> Text("Error: $runError", color = MaterialTheme.colorScheme.error)
                    result.isNotBlank() -> Text(result, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
