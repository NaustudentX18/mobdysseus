package com.jakemalby.odysseusmobile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.jakemalby.odysseusmobile.core.Memory
import com.jakemalby.odysseusmobile.core.Note
import com.jakemalby.odysseusmobile.core.Task
import com.jakemalby.odysseusmobile.core.Workspace
import com.jakemalby.odysseusmobile.core.memory.MemoryFeatureSupport
import com.jakemalby.odysseusmobile.core.memory.MemoryGovernance
import com.jakemalby.odysseusmobile.core.memory.MemoryPersistenceGate
import com.jakemalby.odysseusmobile.core.task.TaskRecurrence
import com.jakemalby.odysseusmobile.core.task.TaskSchedule
import com.jakemalby.odysseusmobile.platform.task.AndroidTaskReminderScheduler as PlatformTaskReminderScheduler
import java.util.UUID

/** Brain, Notes, and Tasks own their rendering and only receive the Workspace contract. */
@Composable
internal fun BrainScreen(workspace: Workspace, update: ((Workspace) -> Workspace) -> Unit) {
    var entry by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editText by rememberSaveable { mutableStateOf("") }
    var deleteCandidateId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val latestWorkspace by rememberUpdatedState(workspace)
    val visibleMemories = remember(workspace.memories, searchQuery) {
        MemoryFeatureSupport.search(workspace.memories, searchQuery)
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val succeeded = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(MemoryFeatureSupport.exportJson(latestWorkspace.memories).toByteArray(Charsets.UTF_8))
                } ?: error("The selected destination could not be opened")
            }.isSuccess
            Toast.makeText(context, if (succeeded) "Memories exported" else "Could not export memories", Toast.LENGTH_SHORT).show()
        }
    }

    deleteCandidateId?.let { candidateId ->
        val candidate = workspace.memories.firstOrNull { it.id == candidateId }
        if (candidate == null) {
            deleteCandidateId = null
        } else {
            AlertDialog(
                onDismissRequest = { deleteCandidateId = null },
                title = { Text("Delete memory?") },
                text = { Text("This memory will be permanently removed from this device.") },
                confirmButton = {
                    TextButton(onClick = {
                        update { state -> state.copy(memories = state.memories.filterNot { it.id == candidateId }) }
                        if (editingId == candidateId) editingId = null
                        deleteCandidateId = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { deleteCandidateId = null }) { Text("Cancel") } },
            )
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Brain", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Private memories stay on this device and are recalled locally.", color = Muted, modifier = Modifier.padding(top = 4.dp))
                }
                TextButton(
                    onClick = { exportLauncher.launch(MemoryFeatureSupport.exportFilename(System.currentTimeMillis())) },
                    enabled = workspace.memories.isNotEmpty(),
                ) { Text("Export all") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(entry, { entry = it }, Modifier.weight(1f), placeholder = { Text("Remember something…") }, maxLines = 3)
                Button(onClick = {
                    val clean = entry.trim()
                    if (clean.isNotEmpty()) {
                        val approved = MemoryGovernance.createManual(
                            id = UUID.randomUUID().toString(),
                            text = clean,
                            createdAt = System.currentTimeMillis(),
                        )
                        update { state -> state.copy(memories = listOf(MemoryPersistenceGate.workspaceMemory(approved)) + state.memories) }
                        entry = ""
                    }
                }) { Text("Add") }
            }
        }
        item {
            OutlinedTextField(
                searchQuery,
                { searchQuery = it },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Search memories…") },
                singleLine = true,
                supportingText = { Text("${visibleMemories.size} of ${workspace.memories.size} memories") },
            )
        }
        if (workspace.memories.isEmpty()) {
            item { EmptyCard("Your brain is empty", "Add facts, preferences, or useful context for future local sessions.") }
        } else if (visibleMemories.isEmpty()) {
            item { EmptyCard("No matching memories", "Try a different search.") }
        }
        items(visibleMemories, key = { it.id }) { memory ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (editingId == memory.id) {
                        OutlinedTextField(editText, { editText = it }, Modifier.fillMaxWidth(), label = { Text("Memory") }, minLines = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val clean = editText.trim()
                                    if (clean.isNotEmpty()) {
                                        update { state -> state.copy(memories = state.memories.map { current ->
                                            if (current.id == memory.id) current.copy(text = clean) else current
                                        }) }
                                        editingId = null
                                    }
                                },
                                enabled = editText.isNotBlank(),
                            ) { Text("Save changes") }
                            TextButton(onClick = { editingId = null }) { Text("Cancel") }
                        }
                    } else {
                        Text(memory.text, color = Ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                editingId = memory.id
                                editText = memory.text
                            }) { Text("Edit") }
                            TextButton(onClick = { deleteCandidateId = memory.id }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
internal fun NotesScreen(workspace: Workspace, update: ((Workspace) -> Workspace) -> Unit) {
    var newTitle by rememberSaveable { mutableStateOf("") }
    var newBody by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editTitle by rememberSaveable { mutableStateOf("") }
    var editBody by rememberSaveable { mutableStateOf("") }
    var deleteCandidateId by rememberSaveable { mutableStateOf<String?>(null) }
    var exportCandidateId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val latestWorkspace by rememberUpdatedState(workspace)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        val note = latestWorkspace.notes.firstOrNull { it.id == exportCandidateId }
        if (uri != null && note != null) {
            val succeeded = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(NoteFeatureSupport.exportMarkdown(note).toByteArray(Charsets.UTF_8))
                } ?: error("The selected destination could not be opened")
            }.isSuccess
            Toast.makeText(context, if (succeeded) "Note exported" else "Could not export note", Toast.LENGTH_SHORT).show()
        }
        exportCandidateId = null
    }
    val visibleNotes = remember(workspace.notes, searchQuery) {
        NoteFeatureSupport.search(workspace.notes, searchQuery)
    }

    deleteCandidateId?.let { candidateId ->
        val candidate = workspace.notes.firstOrNull { it.id == candidateId }
        if (candidate == null) {
            deleteCandidateId = null
        } else {
            AlertDialog(
                onDismissRequest = { deleteCandidateId = null },
                title = { Text("Delete note?") },
                text = { Text("“${candidate.title}” will be permanently removed from this device.") },
                confirmButton = {
                    TextButton(onClick = {
                        update { it.copy(notes = it.notes.filterNot { note -> note.id == candidateId }) }
                        if (editingId == candidateId) editingId = null
                        deleteCandidateId = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { deleteCandidateId = null }) { Text("Cancel") } },
            )
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Personal notes stored in your local workspace.", color = Muted, modifier = Modifier.padding(top = 4.dp)) }
        item { Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(newTitle, { newTitle = it }, Modifier.fillMaxWidth(), placeholder = { Text("Note title") }, singleLine = true)
            OutlinedTextField(newBody, { newBody = it }, Modifier.fillMaxWidth(), placeholder = { Text("Write a thought…") }, minLines = 2, maxLines = 5)
            Button(onClick = { if (newTitle.isNotBlank() || newBody.isNotBlank()) { update { it.copy(notes = listOf(Note(UUID.randomUUID().toString(), newTitle.ifBlank { "Untitled note" }, newBody, System.currentTimeMillis())) + it.notes) }; newTitle = ""; newBody = "" } }) { Text("Save note") }
        } } }
        item {
            OutlinedTextField(
                searchQuery,
                { searchQuery = it },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Search notes…") },
                singleLine = true,
                supportingText = { Text("${visibleNotes.size} of ${workspace.notes.size} notes") },
            )
        }
        if (workspace.notes.isEmpty()) item { EmptyCard("No notes yet", "Create a private note above.") }
        else if (visibleNotes.isEmpty()) item { EmptyCard("No matching notes", "Try a different search.") }
        items(visibleNotes, key = { it.id }) { note ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingId == note.id) {
                        OutlinedTextField(editTitle, { editTitle = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
                        OutlinedTextField(editBody, { editBody = it }, Modifier.fillMaxWidth(), label = { Text("Note") }, minLines = 4)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                update { state -> state.copy(notes = state.notes.map { current ->
                                    if (current.id == note.id) current.copy(
                                        title = editTitle.trim().ifBlank { "Untitled note" },
                                        body = editBody,
                                        updatedAt = System.currentTimeMillis(),
                                    ) else current
                                }) }
                                editingId = null
                            }) { Text("Save changes") }
                            TextButton(onClick = { editingId = null }) { Text("Cancel") }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(note.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { deleteCandidateId = note.id }) {
                                Icon(Icons.Outlined.Delete, "Delete ${note.title}", tint = Muted)
                            }
                        }
                        if (note.body.isNotBlank()) NotesMarkdownBody(note.body)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                editingId = note.id
                                editTitle = note.title
                                editBody = note.body
                            }) { Text("Edit") }
                            TextButton(onClick = {
                                exportCandidateId = note.id
                                exportLauncher.launch(NoteFeatureSupport.exportFilename(note))
                            }) { Text("Export / share") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesMarkdownBody(body: String) {
    val blocks = remember(body) { SafeMarkdownParser.parse(body) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is SafeMarkdownBlock.Plain -> Text(block.text, color = Muted, lineHeight = 21.sp)
                is SafeMarkdownBlock.Code -> Column(
                    Modifier.fillMaxWidth().background(Obsidian, RoundedCornerShape(10.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    block.language?.let { Text(it.uppercase(), color = Coral, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                    Text(block.text, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
internal fun TasksScreen(workspace: Workspace, update: ((Workspace) -> Workspace) -> Unit) {
    val context = LocalContext.current
    val reminderScheduler = remember(context) { PlatformTaskReminderScheduler(context) }
    var newTask by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(TaskListFilter.OPEN.name) }
    var deleteCandidateId by rememberSaveable { mutableStateOf<String?>(null) }
    var dueAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var recurrence by rememberSaveable { mutableStateOf(TaskRecurrence.NONE) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val filter = TaskListFilter.entries.firstOrNull { it.name == filterName } ?: TaskListFilter.OPEN
    val visibleTasks = remember(workspace.tasks, searchQuery, filter) {
        filterTasks(workspace.tasks, searchQuery, filter)
    }

    fun scheduleReminder(task: Task) {
        val due = task.dueAt ?: return
        reminderScheduler.schedule(
            TaskSchedule(
                taskId = task.id,
                dueAtEpochMillis = due,
                zoneId = java.util.TimeZone.getDefault().id,
                recurrence = task.recurrence,
                remindBeforeMillis = task.remindBeforeMillis,
            ),
            task.title,
        )
    }

    deleteCandidateId?.let { candidateId ->
        val candidate = workspace.tasks.firstOrNull { it.id == candidateId }
        if (candidate == null) {
            deleteCandidateId = null
        } else {
            AlertDialog(
                onDismissRequest = { deleteCandidateId = null },
                title = { Text("Delete task?") },
                text = { Text("“${candidate.title}” will be permanently removed from this device.") },
                confirmButton = {
                    TextButton(onClick = {
                        reminderScheduler.cancel(candidateId)
                        update { it.copy(tasks = it.tasks.filterNot { task -> task.id == candidateId }) }
                        deleteCandidateId = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { deleteCandidateId = null }) { Text("Cancel") } },
            )
        }
    }

    if (showDatePicker) {
        val initial = dueAt ?: System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = initial }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = java.util.Calendar.getInstance().apply {
                    set(year, month, day, 9, 0, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                dueAt = picked
                showDatePicker = false
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH),
        ).show()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Private, local task tracking with optional reminders.",
            color = Muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                newTask,
                { newTask = it },
                Modifier.weight(1f),
                placeholder = { Text("Add a task…") },
                singleLine = true,
            )
            Button(onClick = {
                val clean = newTask.trim()
                if (clean.isNotEmpty()) {
                    val task = Task(UUID.randomUUID().toString(), clean, false, dueAt, recurrence)
                    if (task.dueAt != null) scheduleReminder(task)
                    update { it.copy(tasks = listOf(task) + it.tasks) }
                    newTask = ""
                    dueAt = null
                    recurrence = TaskRecurrence.NONE
                }
            }) { Text("Add") }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(if (dueAt == null) "Set due date" else "Due: ${java.text.DateFormat.getDateInstance().format(java.util.Date(dueAt!!))}")
            }
            TaskRecurrence.entries.forEach { option ->
                FilterChip(
                    selected = recurrence == option,
                    onClick = { recurrence = option },
                    label = { Text(option.name.lowercase()) },
                )
            }
        }
        OutlinedTextField(
            searchQuery,
            { searchQuery = it },
            Modifier.fillMaxWidth().padding(top = 10.dp),
            placeholder = { Text("Search tasks…") },
            singleLine = true,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TaskListFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filterName = option.name },
                    label = { Text(option.label) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            "${visibleTasks.size} of ${workspace.tasks.size} tasks",
            color = Muted,
            style = MaterialTheme.typography.labelMedium,
        )
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (workspace.tasks.isEmpty()) {
                item { EmptyCard("Nothing queued", "Tasks you add here remain on this phone.") }
            } else if (visibleTasks.isEmpty()) {
                item { EmptyCard("No matching tasks", "Try another search or filter.") }
            }
            items(visibleTasks, key = { it.id }) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(task.done, { checked ->
                                if (checked) reminderScheduler.cancel(task.id)
                                update { state -> state.copy(tasks = state.tasks.map { if (it.id == task.id) it.copy(done = checked) else it }) }
                            })
                            Text(task.title, Modifier.weight(1f), color = if (task.done) Muted else Ink)
                            IconButton(onClick = { deleteCandidateId = task.id }) {
                                Icon(Icons.Outlined.Delete, "Delete ${task.title}", tint = Muted)
                            }
                        }
                        if (task.dueAt != null) {
                            Text(
                                "Due ${java.text.DateFormat.getDateInstance().format(java.util.Date(task.dueAt!!))}" +
                                    if (task.recurrence != TaskRecurrence.NONE) " · repeats ${task.recurrence.name.lowercase()}" else "",
                                color = Muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class TaskListFilter(val label: String) {
    OPEN("Open"),
    ALL("All"),
    DONE("Done"),
}

private fun filterTasks(tasks: List<Task>, query: String, filter: TaskListFilter): List<Task> {
    val needle = query.trim()
    return tasks.filter { task ->
        val matchesState = when (filter) {
            TaskListFilter.OPEN -> !task.done
            TaskListFilter.ALL -> true
            TaskListFilter.DONE -> task.done
        }
        matchesState && (needle.isEmpty() || task.title.contains(needle, ignoreCase = true))
    }
}
