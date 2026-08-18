package com.jakemalby.odysseusmobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jakemalby.odysseusmobile.core.Workspace
import com.jakemalby.odysseusmobile.core.diagnostics.AppPermission
import com.jakemalby.odysseusmobile.core.diagnostics.CleanupAvailabilityReason
import com.jakemalby.odysseusmobile.core.diagnostics.ComponentHealth
import com.jakemalby.odysseusmobile.core.diagnostics.DiagnosticComponent
import com.jakemalby.odysseusmobile.core.diagnostics.FeatureAvailability
import com.jakemalby.odysseusmobile.core.diagnostics.FeatureHealth
import com.jakemalby.odysseusmobile.core.diagnostics.FeatureUnavailableReason
import com.jakemalby.odysseusmobile.core.diagnostics.HealthLevel
import com.jakemalby.odysseusmobile.core.diagnostics.HealthReason
import com.jakemalby.odysseusmobile.core.diagnostics.LocalFeature
import com.jakemalby.odysseusmobile.core.diagnostics.PermissionGrantState
import com.jakemalby.odysseusmobile.core.diagnostics.PermissionHealth
import com.jakemalby.odysseusmobile.core.diagnostics.RedactedDiagnosticsExporter
import com.jakemalby.odysseusmobile.core.diagnostics.RedactedDiagnosticsSnapshot
import com.jakemalby.odysseusmobile.core.diagnostics.StorageCleanupCandidate
import com.jakemalby.odysseusmobile.core.diagnostics.StorageCleanupTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
internal fun DiagnosticsPanel(workspace: Workspace) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<DiagnosticsUiState?>(null) }
    var pendingCleanup by remember { mutableStateOf<CleanupPlan?>(null) }
    var status by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf("") }

    val diagnosticsWriter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { destination ->
        if (destination != null && pendingExport.isNotEmpty()) {
            status = runCatching {
                context.contentResolver.openOutputStream(destination, "wt")?.bufferedWriter()?.use {
                    it.write(pendingExport)
                } ?: error("Destination unavailable")
                "Redacted diagnostics exported"
            }.getOrElse { "Diagnostics export failed" }
        }
        pendingExport = ""
    }

    LaunchedEffect(workspace, refresh) {
        state = withContext(Dispatchers.IO) { collectDiagnostics(context, workspace) }
    }

    pendingCleanup?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingCleanup = null },
            title = { Text("Clean ${plan.target.displayName()}?") },
            text = {
                Text(
                    "Delete ${plan.files.size} confirmed temporary or orphaned item(s) " +
                        "(${formatBytes(plan.files.sumOf(File::length))}) from app-private storage? " +
                        "Chats, notes, models and gallery items still in your workspace are not selected.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingCleanup = null
                    scope.launch {
                        val removed = withContext(Dispatchers.IO) { safelyDelete(plan) }
                        status = if (removed == plan.files.size) {
                            "Cleaned $removed item(s)"
                        } else {
                            "Cleaned $removed of ${plan.files.size} item(s)"
                        }
                        refresh += 1
                    }
                }) { Text("Clean", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingCleanup = null }) { Text("Cancel") } },
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Device diagnostics", fontWeight = FontWeight.Bold)
            Text(
                "Content-free health, permission and app-private storage checks. Nothing is sent anywhere.",
                color = Muted,
            )
            val diagnostics = state
            if (diagnostics == null) {
                Text("Checking local services…", color = Muted)
            } else {
                Text("Private storage", fontWeight = FontWeight.SemiBold)
                StorageRows(diagnostics.storage)

                Text("Permissions", fontWeight = FontWeight.SemiBold)
                diagnostics.snapshot.permissionHealth.forEach { permission ->
                    DiagnosticRow(permission.permission.displayName(), permission.state.displayName())
                }

                Text("Local-only features", fontWeight = FontWeight.SemiBold)
                diagnostics.snapshot.featureHealth.forEach { feature ->
                    val value = when (val availability = feature.availability) {
                        FeatureAvailability.Available -> "Available"
                        is FeatureAvailability.Unavailable -> availability.reason.displayName()
                    }
                    DiagnosticRow(feature.feature.displayName(), value)
                }

                Text("Safe cleanup", fontWeight = FontWeight.SemiBold)
                diagnostics.snapshot.cleanupCandidates.forEach { candidate ->
                    val plan = diagnostics.cleanupPlans[candidate.target]
                    Row(
                        Modifier.fillMaxWidth().background(PanelRaised, RoundedCornerShape(10.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(candidate.target.displayName(), fontSize = 13.sp)
                            Text(
                                if (candidate.canClean) {
                                    "${candidate.itemCount} item(s) · ${formatBytes(candidate.estimatedBytes)}"
                                } else {
                                    candidate.availabilityReason.displayName()
                                },
                                color = Muted,
                                fontSize = 11.sp,
                            )
                        }
                        if (candidate.canClean && plan != null) {
                            TextButton(onClick = { pendingCleanup = plan }) { Text("Review") }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        pendingExport = RedactedDiagnosticsExporter.export(diagnostics.snapshot)
                        diagnosticsWriter.launch("mobdysseus-redacted-diagnostics.json")
                    }) { Text("Export redacted") }
                    OutlinedButton(onClick = { refresh += 1 }) { Text("Refresh") }
                }
            }
            if (status.isNotBlank()) Text(status, color = Success, fontSize = 12.sp)
            Text(
                "Exports include only allow-listed states, counts and byte totals—never text, filenames, paths, prompts or model output.",
                color = Muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun StorageRows(storage: StorageBreakdown) {
    DiagnosticRow("Encrypted database", formatBytes(storage.databaseBytes))
    DiagnosticRow("Installed models", formatBytes(storage.modelBytes))
    DiagnosticRow("Documents / notes", formatBytes(storage.documentBytes))
    DiagnosticRow("Private gallery", formatBytes(storage.galleryBytes))
    DiagnosticRow("All app-private data", formatBytes(storage.appPrivateBytes))
    DiagnosticRow("Device space available", formatBytes(storage.availableBytes))
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().background(PanelRaised, RoundedCornerShape(10.dp)).padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp)
        Text(value, color = Muted, fontSize = 12.sp)
    }
}

private data class DiagnosticsUiState(
    val snapshot: RedactedDiagnosticsSnapshot,
    val storage: StorageBreakdown,
    val cleanupPlans: Map<StorageCleanupTarget, CleanupPlan>,
)

private data class StorageBreakdown(
    val databaseBytes: Long,
    val modelBytes: Long,
    val documentBytes: Long,
    val galleryBytes: Long,
    val appPrivateBytes: Long,
    val availableBytes: Long,
)

private data class CleanupPlan(
    val target: StorageCleanupTarget,
    val allowedParent: File,
    val files: List<File>,
)

private fun collectDiagnostics(context: Context, workspace: Workspace): DiagnosticsUiState {
    val models = File(context.filesDir, "models")
    val gallery = File(context.filesDir, "gallery")
    val database = context.getDatabasePath("mobdysseus-encrypted.db")
    val databaseFiles = listOf(database, File(database.path + "-wal"), File(database.path + "-shm"))
    val installedModels = models.directFiles { it.extension.equals("litertlm", ignoreCase = true) }
    val modelTemps = models.directFiles { it.name.startsWith(".import-") && it.name.endsWith(".part") || it.name.endsWith(".writing") }
    val referencedGallery = workspace.gallery.mapNotNull { runCatching { File(it.path).canonicalPath }.getOrNull() }.toSet()
    val orphanedGallery = gallery.directFiles { file ->
        runCatching { file.canonicalPath !in referencedGallery }.getOrDefault(false)
    }
    val cleanupPlans = listOf(
        CleanupPlan(StorageCleanupTarget.MODEL_DOWNLOAD_TEMPORARIES, models, modelTemps),
        CleanupPlan(StorageCleanupTarget.ORPHANED_PRIVATE_BLOBS, gallery, orphanedGallery),
    ).filter { it.files.isNotEmpty() }.associateBy(CleanupPlan::target)

    val databaseBytes = databaseFiles.sumOf { it.length().coerceAtLeast(0L) }
    val modelBytes = installedModels.sumOf { it.length().coerceAtLeast(0L) }
    val galleryBytes = gallery.directFiles { true }.sumOf { it.length().coerceAtLeast(0L) }
    val documentBytes = workspace.notes.sumOf {
        it.title.toByteArray(Charsets.UTF_8).size.toLong() + it.body.toByteArray(Charsets.UTF_8).size
    }
    val availableBytes = runCatching { StatFs(context.filesDir.path).availableBytes }.getOrDefault(0L).coerceAtLeast(0L)
    val appPrivateBytes = listOf(context.filesDir, context.cacheDir).sumOf(::directoryBytes) + databaseBytes
    val storage = StorageBreakdown(databaseBytes, modelBytes, documentBytes, galleryBytes, appPrivateBytes, availableBytes)
    val permissions = permissionHealth(context)
    val noModel = installedModels.isEmpty()
    val storageHealth = when {
        availableBytes < 256L * 1024 * 1024 -> ComponentHealth(DiagnosticComponent.STORAGE, HealthLevel.UNAVAILABLE, HealthReason.STORAGE_FULL)
        availableBytes < 1024L * 1024 * 1024 -> ComponentHealth(DiagnosticComponent.STORAGE, HealthLevel.DEGRADED, HealthReason.STORAGE_LOW)
        else -> ComponentHealth.healthy(DiagnosticComponent.STORAGE)
    }
    val componentHealth = listOf(
        ComponentHealth.healthy(DiagnosticComponent.DATABASE),
        if (noModel) ComponentHealth(DiagnosticComponent.MODEL, HealthLevel.UNAVAILABLE, HealthReason.NO_MODEL_INSTALLED) else ComponentHealth.healthy(DiagnosticComponent.MODEL),
        if (workspace.notes.isEmpty()) ComponentHealth(DiagnosticComponent.RETRIEVAL_INDEX, HealthLevel.DEGRADED, HealthReason.INDEX_EMPTY) else ComponentHealth.healthy(DiagnosticComponent.RETRIEVAL_INDEX),
        if (permissions.any { it.state == PermissionGrantState.DENIED }) ComponentHealth(DiagnosticComponent.PERMISSIONS, HealthLevel.DEGRADED, HealthReason.PERMISSION_DENIED) else ComponentHealth.healthy(DiagnosticComponent.PERMISSIONS),
        storageHealth,
    )
    val permissionMap = permissions.associate { it.permission to it.state }
    fun permissionFeature(feature: LocalFeature, permission: AppPermission) = FeatureHealth(
        feature,
        if (permissionMap[permission] == PermissionGrantState.GRANTED) FeatureAvailability.Available
        else FeatureAvailability.Unavailable(FeatureUnavailableReason.PERMISSION_REQUIRED),
    )
    val features = listOf(
        FeatureHealth(LocalFeature.CHAT, FeatureAvailability.Available),
        FeatureHealth(LocalFeature.MODEL_INFERENCE, if (noModel) FeatureAvailability.Unavailable(FeatureUnavailableReason.MODEL_REQUIRED) else FeatureAvailability.Available),
        FeatureHealth(LocalFeature.DOCUMENT_IMPORT, FeatureAvailability.Available),
        FeatureHealth(LocalFeature.RETRIEVAL, if (workspace.notes.isEmpty()) FeatureAvailability.Unavailable(FeatureUnavailableReason.COMPONENT_UNAVAILABLE) else FeatureAvailability.Available),
        permissionFeature(LocalFeature.VOICE_INPUT, AppPermission.MICROPHONE),
        FeatureHealth(LocalFeature.TEXT_TO_SPEECH, FeatureAvailability.Available),
        permissionFeature(LocalFeature.CAMERA_IMPORT, AppPermission.CAMERA),
        permissionFeature(LocalFeature.CALENDAR_READ, AppPermission.CALENDAR_READ),
        permissionFeature(LocalFeature.CALENDAR_WRITE, AppPermission.CALENDAR_WRITE),
        FeatureHealth(LocalFeature.CONTACT_PICKER, FeatureAvailability.Available),
        FeatureHealth(LocalFeature.BACKUP_EXPORT, FeatureAvailability.Available),
        FeatureHealth(LocalFeature.BACKUP_RESTORE, FeatureAvailability.Available),
    )
    val cleanup = StorageCleanupTarget.entries.map { target ->
        val files = cleanupPlans[target]?.files.orEmpty()
        StorageCleanupCandidate(
            target,
            files.sumOf { it.length().coerceAtLeast(0L) },
            files.size,
            if (files.isEmpty()) CleanupAvailabilityReason.NOTHING_TO_CLEAN else CleanupAvailabilityReason.READY,
        )
    }
    val databaseRecords = workspace.conversations.size.toLong() +
        workspace.conversations.sumOf { it.messages.size }.toLong() + workspace.notes.size +
        workspace.tasks.size + workspace.memories.size + workspace.gallery.size + 1L
    return DiagnosticsUiState(
        RedactedDiagnosticsSnapshot(
            generatedAtEpochSeconds = System.currentTimeMillis() / 1000,
            componentHealth = componentHealth,
            featureHealth = features,
            permissionHealth = permissions,
            cleanupCandidates = cleanup,
            installedModelCount = installedModels.size,
            indexedDocumentCount = workspace.notes.size,
            databaseRecordCount = databaseRecords,
            appPrivateBytesUsed = appPrivateBytes,
            appPrivateBytesAvailable = availableBytes,
        ),
        storage,
        cleanupPlans,
    )
}

@Suppress("DEPRECATION")
private fun permissionHealth(context: Context): List<PermissionHealth> {
    val declared = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        .requestedPermissions.orEmpty().toSet()
    fun state(name: String): PermissionGrantState = when {
        name !in declared -> PermissionGrantState.NOT_APPLICABLE
        context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED -> PermissionGrantState.GRANTED
        else -> PermissionGrantState.DENIED
    }
    return listOf(
        PermissionHealth(AppPermission.CAMERA, state(Manifest.permission.CAMERA)),
        PermissionHealth(AppPermission.MICROPHONE, state(Manifest.permission.RECORD_AUDIO)),
        PermissionHealth(AppPermission.NOTIFICATIONS, state("android.permission.POST_NOTIFICATIONS")),
        PermissionHealth(AppPermission.CALENDAR_READ, state(Manifest.permission.READ_CALENDAR)),
        PermissionHealth(AppPermission.CALENDAR_WRITE, state(Manifest.permission.WRITE_CALENDAR)),
    )
}

private fun File.directFiles(predicate: (File) -> Boolean): List<File> =
    listFiles()?.filter { it.isFile && !java.nio.file.Files.isSymbolicLink(it.toPath()) && predicate(it) }.orEmpty()

private fun directoryBytes(root: File): Long = runCatching {
    if (!root.exists()) 0L else root.walkTopDown().filter { it.isFile && !java.nio.file.Files.isSymbolicLink(it.toPath()) }
        .sumOf { it.length().coerceAtLeast(0L) }
}.getOrDefault(0L)

private fun safelyDelete(plan: CleanupPlan): Int {
    val parent = runCatching { plan.allowedParent.canonicalFile }.getOrNull() ?: return 0
    return plan.files.count { candidate ->
        runCatching {
            val file = candidate.canonicalFile
            file.parentFile == parent && file.isFile && !java.nio.file.Files.isSymbolicLink(candidate.toPath()) && file.delete()
        }.getOrDefault(false)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index += 1
    }
    return String.format(Locale.US, "%.1f %s", value, units[index])
}

private fun Enum<*>.displayName(): String = name.lowercase(Locale.US).replace('_', ' ')
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
