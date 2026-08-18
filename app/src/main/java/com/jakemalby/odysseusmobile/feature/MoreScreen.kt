package com.jakemalby.odysseusmobile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jakemalby.odysseusmobile.core.GalleryItem
import com.jakemalby.odysseusmobile.core.Workspace
import com.jakemalby.odysseusmobile.core.WorkspaceStore
import com.jakemalby.odysseusmobile.core.EncryptedBackupCodec
import com.jakemalby.odysseusmobile.core.image.ImageEditRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

@Composable
internal fun MoreScreen(workspace: Workspace, update: ((Workspace) -> Workspace) -> Unit, reset: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { WorkspaceStore(context.applicationContext) }
    val backupCodec = remember { EncryptedBackupCodec() }
    val scope = rememberCoroutineScope()
    var backupStatus by remember { mutableStateOf("") }
    var backupPassphrase by remember { mutableStateOf("") }
    var pendingRestore by remember { mutableStateOf<Workspace?>(null) }
    var pendingReset by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var galleryStatus by remember { mutableStateOf("") }
    var previewItem by remember { mutableStateOf<GalleryItem?>(null) }
    var exportItem by remember { mutableStateOf<GalleryItem?>(null) }
    var calendarEvents by remember { mutableStateOf<List<PhoneCalendarEvent>>(emptyList()) }
    var calendarStatus by remember { mutableStateOf("") }
    var contactStatus by remember { mutableStateOf("") }
    val backupWriter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { destination ->
            scope.launch {
                backupStatus = "Encrypting backup…"
                backupStatus = withContext(Dispatchers.Default) {
                    runCatching {
                        val plain = store.encode(workspace).toByteArray(Charsets.UTF_8)
                        val encrypted = try { backupCodec.encrypt(plain, backupPassphrase.toCharArray()) } finally { plain.fill(0) }
                        context.contentResolver.openOutputStream(destination, "wt")?.use { it.write(encrypted) }
                            ?: error("Android could not open the destination.")
                        encrypted.fill(0)
                        "Encrypted workspace backup exported"
                    }.getOrElse { "Backup failed: ${it.message}" }
                }
            }
        }
    }
    val backupReader = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { source ->
            scope.launch {
                backupStatus = "Authenticating backup…"
                val result = withContext(Dispatchers.Default) { runCatching {
                    val envelope = context.contentResolver.openInputStream(source)?.use { it.readBoundedBackup() }
                        ?: error("Android could not read the backup.")
                    val plain = try { backupCodec.decrypt(envelope, backupPassphrase.toCharArray()) } finally { envelope.fill(0) }
                    try { store.decode(String(plain, Charsets.UTF_8)) } finally { plain.fill(0) }
                } }
                result.onSuccess { restored -> pendingRestore = restored; backupStatus = "Backup authenticated; confirm replacement" }
                    .onFailure { backupStatus = "Restore failed: wrong passphrase or invalid backup" }
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selected ->
            importGalleryImage(context, selected).fold(
                onSuccess = { item -> update { it.copy(gallery = listOf(item) + it.gallery) }; galleryStatus = "Imported ${item.name}" },
                onFailure = { galleryStatus = "Photo import failed: ${it.message}" },
            )
        }
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { captured ->
            saveCapturedImage(context, captured).fold(
                onSuccess = { item -> update { it.copy(gallery = listOf(item) + it.gallery) }; galleryStatus = "Camera image saved" },
                onFailure = { galleryStatus = "Camera save failed: ${it.message}" },
            )
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraCapture.launch(null) else galleryStatus = "Camera permission was not granted"
    }
    val galleryExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { uri ->
        val item = exportItem
        if (uri != null && item != null) {
            galleryStatus = runCatching {
                java.io.File(item.path).inputStream().use { input ->
                    context.contentResolver.openOutputStream(uri, "wt")?.use(input::copyTo)
                        ?: error("Android could not open the destination.")
                }
                "Exported a copy; the private original is unchanged"
            }.getOrElse { "Image export failed: ${it.message}" }
        }
        exportItem = null
    }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) runCatching { upcomingPhoneEvents(context) }.onSuccess { calendarEvents = it; calendarStatus = "${it.size} upcoming event(s)" }.onFailure { calendarStatus = "Calendar read failed: ${it.message}" }
        else calendarStatus = "Calendar permission was not granted"
    }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri -> contactStatus = runCatching { "Selected ${readPickedContact(context, uri).name}" }.getOrElse { "Contact read failed: ${it.message}" } }
    }
    val rotateCopy: (GalleryItem, Boolean) -> Unit = { item, showPreview ->
        scope.launch {
            galleryStatus = "Creating a rotated copy…"
            val result = withContext(Dispatchers.Default) {
                createEditedGalleryCopy(context.applicationContext, item, ImageEditRecipe(quarterTurnsClockwise = 1))
            }
            result.fold(
                onSuccess = { edited ->
                    update { state -> state.copy(gallery = listOf(edited) + state.gallery.filterNot { it.path == edited.path }) }
                    if (showPreview) previewItem = edited
                    galleryStatus = "Rotated copy saved; ${item.name} is unchanged"
                },
                onFailure = { galleryStatus = "Rotate failed: ${it.message}" },
            )
        }
    }
    pendingRestore?.let { restored ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Replace this workspace?") },
            text = { Text("The authenticated backup will replace current chats, notes, tasks, memories, settings and gallery metadata.") },
            confirmButton = {
                TextButton(onClick = {
                    update { restored }
                    pendingRestore = null
                    backupStatus = "Encrypted workspace backup restored"
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null; backupStatus = "Restore cancelled" }) { Text("Cancel") } },
        )
    }
    if (pendingReset) {
        AlertDialog(
            onDismissRequest = { pendingReset = false },
            title = { Text("Reset local workspace?") },
            text = { Text("This permanently removes the current workspace from Mobdysseus. Export an encrypted backup first if needed.") },
            confirmButton = { TextButton(onClick = { pendingReset = false; reset() }) { Text("Reset", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingReset = false }) { Text("Cancel") } },
        )
    }
    previewItem?.let { item ->
        GalleryPreviewDialog(
            item = item,
            close = { previewItem = null },
            rotate = { rotateCopy(item, true) },
            export = { exportItem = item; galleryExporter.launch(item.name.ifBlank { "mobdysseus-image.jpg" }) },
        )
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("More", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Native controls and mobile equivalents for the rest of Mobdysseus.", color = Muted, modifier = Modifier.padding(top = 4.dp)) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Settings, null, tint = Coral); Text("Privacy & runtime", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp)) }; SettingToggle("Local-only mode", "No server connection is required for the native workspace.", workspace.settings.localOnly) { checked -> update { it.copy(settings = it.settings.copy(localOnly = checked)) } }; HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Border); SettingToggle("Compact density", "More workspace content on the S25 display.", workspace.settings.compactDensity) { checked -> update { it.copy(settings = it.settings.copy(compactDensity = checked)) } } } }
        }
        item { DiagnosticsPanel(workspace) }
        item { ProviderPanel(localOnly = workspace.settings.localOnly) }
        item { FeatureCard("Phone tools", "Camera, microphone, file import, sharing, notifications, and calendar access are added as permissioned Android actions—not desktop shell commands.") }
        item { CapabilityPanel() }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Gallery", fontWeight = FontWeight.Bold)
                    Text("Capture or import images into private Mobdysseus storage.", color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { photoPicker.launch("image/*") }) { Text("Import photo") }
                        OutlinedButton(onClick = {
                            if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraCapture.launch(null)
                            else cameraPermission.launch(Manifest.permission.CAMERA)
                        }) { Text("Camera") }
                    }
                    if (galleryStatus.isNotBlank()) Text(galleryStatus, color = Success, fontSize = 12.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Contacts", fontWeight = FontWeight.Bold)
                    Text("Choose a single contact with Android’s private picker, or create a contact in your phone’s contacts app.", color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { runCatching { contactPicker.launch(pickContactIntent()) }.onFailure { contactStatus = "No contacts app is available" } }) { Text("Choose contact") }
                        OutlinedButton(onClick = { runCatching { context.startActivity(newContactIntent()) }.onFailure { contactStatus = "No contacts app is available" } }) { Text("New contact") }
                    }
                    if (contactStatus.isNotBlank()) Text(contactStatus, color = Success, fontSize = 12.sp)
                }
            }
        }
        if (workspace.gallery.isNotEmpty()) {
            items(workspace.gallery.take(12), key = { it.id }) { item -> GalleryRow(
                item = item,
                preview = { previewItem = item },
                rotate = { rotateCopy(item, false) },
                export = { exportItem = item; galleryExporter.launch(item.name.ifBlank { "mobdysseus-image.jpg" }) },
                remove = {
                    runCatching { java.io.File(item.path).delete() }
                    update { it.copy(gallery = it.gallery.filterNot { candidate -> candidate.id == item.id }) }
                },
            ) }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Calendar", fontWeight = FontWeight.Bold)
                    Text("View upcoming phone events or create one using your chosen Android calendar account.", color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) runCatching { upcomingPhoneEvents(context) }.onSuccess { calendarEvents = it; calendarStatus = "${it.size} upcoming event(s)" }.onFailure { calendarStatus = "Calendar read failed: ${it.message}" }
                            else calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                        }) { Text("Upcoming") }
                        OutlinedButton(onClick = { runCatching { context.startActivity(newCalendarEventIntent()) }.onFailure { calendarStatus = "No calendar app is available" } }) { Text("New event") }
                    }
                    if (calendarStatus.isNotBlank()) Text(calendarStatus, color = Success, fontSize = 12.sp)
                    calendarEvents.take(6).forEach { event ->
                        Column(Modifier.fillMaxWidth().background(PanelRaised, RoundedCornerShape(10.dp)).padding(10.dp)) { Text(event.title, fontWeight = FontWeight.SemiBold); Text(event.displayTime(), color = Muted, fontSize = 12.sp) }
                    }
                }
            }
        }
        item { DocumentsPanel() }
        item { FeatureCard("Local intelligence", "The model runtime is isolated from workspace data. Downloaded models, embeddings, and documents remain app-private.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Search everything", fontWeight = FontWeight.Bold)
                    OutlinedTextField(searchQuery, { searchQuery = it }, Modifier.fillMaxWidth(), placeholder = { Text("Chats, notes, memories, tasks…") }, singleLine = true)
                    if (searchQuery.isNotBlank()) {
                        val results = workspaceSearch(workspace, searchQuery)
                        if (results.isEmpty()) Text("No local matches", color = Muted)
                        results.take(12).forEach { result -> Text(result, color = Ink, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().background(PanelRaised, RoundedCornerShape(10.dp)).padding(10.dp)) }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Backup & restore", fontWeight = FontWeight.Bold)
                    Text("Export or restore a passphrase-encrypted Mobdysseus backup. The passphrase never leaves this phone and cannot be recovered.", color = Muted)
                    OutlinedTextField(
                        backupPassphrase,
                        { backupPassphrase = it.take(256) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Backup passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (backupPassphrase.length >= 8) backupWriter.launch("mobdysseus-workspace.mobdbak") else backupStatus = "Use at least 8 passphrase characters" }) { Text("Export") }
                        OutlinedButton(onClick = { if (backupPassphrase.length >= 8) backupReader.launch(arrayOf("application/octet-stream", "*/*")) else backupStatus = "Enter the backup passphrase first" }) { Text("Restore") }
                    }
                    if (backupStatus.isNotBlank()) Text(backupStatus, color = Success, fontSize = 12.sp)
                }
            }
        }
        item { OutlinedButton(onClick = { pendingReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset local workspace", color = Coral) } }
    }
}

@Composable
private fun GalleryRow(item: GalleryItem, preview: () -> Unit, rotate: () -> Unit, export: () -> Unit, remove: () -> Unit) {
    val bitmap = remember(item.path) { BitmapFactory.decodeFile(item.path)?.asImageBitmap() }
    Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            bitmap?.let { Image(it, item.name, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = preview) { Text("Preview") }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = rotate) { Text("Rotate copy") }
                    TextButton(onClick = export) { Text("Export") }
                }
            }
            IconButton(onClick = remove) { Icon(Icons.Outlined.Delete, "Delete image", tint = Muted) }
        }
    }
}

@Composable
private fun GalleryPreviewDialog(item: GalleryItem, close: () -> Unit, rotate: () -> Unit, export: () -> Unit) {
    val bitmap = remember(item.path) { BitmapFactory.decodeFile(item.path)?.asImageBitmap() }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Panel) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = close) { Text("Close") }
                }
                bitmap?.let { Image(it, item.name, Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Fit) }
                    ?: Text("This image could not be decoded.", Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                Text("Edits and exports create copies. The private original is never overwritten.", color = Muted, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = rotate, modifier = Modifier.weight(1f)) { Text("Rotate copy") }
                    OutlinedButton(onClick = export, modifier = Modifier.weight(1f)) { Text("Export copy") }
                }
            }
        }
    }
}

private fun importGalleryImage(context: Context, uri: Uri): Result<GalleryItem> = runCatching {
    val directory = java.io.File(context.filesDir, "gallery").apply { mkdirs() }
    val original = displayName(context, uri) ?: "imported-image"
    val extension = original.substringAfterLast('.', "jpg").take(8).replace(Regex("[^A-Za-z0-9]"), "")
    val id = UUID.randomUUID().toString()
    val target = java.io.File(directory, "$id.${extension.ifBlank { "jpg" }}")
    context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) } ?: error("Android could not open that image.")
    GalleryItem(id, original, target.absolutePath, System.currentTimeMillis())
}

private fun saveCapturedImage(context: Context, bitmap: Bitmap): Result<GalleryItem> = runCatching {
    val directory = java.io.File(context.filesDir, "gallery").apply { mkdirs() }
    val id = UUID.randomUUID().toString()
    val target = java.io.File(directory, "$id.jpg")
    target.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it)) { "JPEG encoding failed." } }
    GalleryItem(id, "Camera ${java.text.SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.getDefault()).format(java.util.Date())}", target.absolutePath, System.currentTimeMillis())
}

private fun createEditedGalleryCopy(context: Context, original: GalleryItem, recipe: ImageEditRecipe): Result<GalleryItem> = runCatching {
    val source = BitmapFactory.decodeFile(original.path) ?: error("The original image could not be decoded.")
    val matrix = Matrix().apply { postRotate(recipe.quarterTurnsClockwise * 90f) }
    val edited = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    val directory = java.io.File(context.filesDir, "gallery").apply { mkdirs() }
    val name = recipe.deterministicOutputName(original.name, sourceIdentity = original.id, extension = "jpg")
    val target = java.io.File(directory, name)
    target.outputStream().use { require(edited.compress(Bitmap.CompressFormat.JPEG, 94, it)) { "JPEG encoding failed." } }
    if (edited !== source) edited.recycle()
    source.recycle()
    GalleryItem(UUID.randomUUID().toString(), name, target.absolutePath, System.currentTimeMillis())
}

private fun workspaceSearch(workspace: Workspace, query: String): List<String> {
    val needle = query.trim()
    if (needle.isBlank()) return emptyList()
    val results = mutableListOf<String>()
    workspace.conversations.forEach { chat -> chat.messages.filter { it.text.contains(needle, true) }.forEach { results += "CHAT · ${chat.title}\n${it.author}: ${it.text}" } }
    workspace.notes.filter { it.title.contains(needle, true) || it.body.contains(needle, true) }.forEach { results += "NOTE · ${it.title}\n${it.body}" }
    workspace.memories.filter { it.text.contains(needle, true) }.forEach { results += "MEMORY\n${it.text}" }
    workspace.tasks.filter { it.title.contains(needle, true) }.forEach { results += "TASK${if (it.done) " · DONE" else ""}\n${it.title}" }
    return results
}

@Composable
private fun SettingToggle(title: String, detail: String, checked: Boolean, change: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(detail, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)) }; Switch(checked, change) } }

@Composable
private fun FeatureCard(title: String, detail: String) { Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Border)) { Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(detail, color = Muted, modifier = Modifier.padding(top = 7.dp)) } } }

private fun displayName(context: Context, uri: Uri): String? = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
}

private fun java.io.InputStream.readBoundedBackup(): ByteArray {
    val maximum = 64 * 1024 * 1024 + 128
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= maximum) { "Backup is too large" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
