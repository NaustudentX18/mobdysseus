package com.mobdysseus.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.mobdysseus.app.cookbook.Catalog
import com.mobdysseus.app.cookbook.DeviceHardware
import com.mobdysseus.app.cookbook.ModelRanker
import com.mobdysseus.app.cookbook.RankedModel
import com.mobdysseus.app.local.ModelDownloadManager
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CookbookScreen(
    hardware: DeviceHardware,
    downloadManager: ModelDownloadManager,
    onModelSelected: (repoId: String, filename: String) -> Unit,
) {
    var ranked by remember { mutableStateOf<List<RankedModel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val candidates = Catalog.latest()
        ranked = ModelRanker.rank(candidates, hardware)
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Device", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        HardwareRow("SoC", hardware.socModel)
        HardwareRow("Model", hardware.model)
        HardwareRow(
            "RAM",
            String.format(Locale.US, "%.1f GB total · %.1f GB usable", hardware.totalRamGb, hardware.usableRamGb),
        )
        HardwareRow("NPU", if (hardware.hasNpu) "Hexagon (~45 TOPS)" else "None detected")
        HardwareRow("Storage free", String.format(Locale.US, "%.1f GB", hardware.freeStorageGb))

        Spacer(Modifier.height(24.dp))
        Text("Recommended models", style = MaterialTheme.typography.titleLarge)
        Text(
            "Best fit for this device, ranked by RAM fit and quality.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        if (loading) {
            CircularProgressIndicator(Modifier.padding(16.dp))
        } else {
            ranked.take(15).forEach { rm ->
                ModelRow(rm, downloadManager, onModelSelected)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HardwareRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            modifier = Modifier.weight(0.4f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(0.6f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Per-model download lifecycle. */
private sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    object Installed : DownloadState
    data class Failed(val message: String) : DownloadState
}

@Composable
private fun ModelRow(
    rm: RankedModel,
    downloadManager: ModelDownloadManager,
    onModelSelected: (repoId: String, filename: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var state by remember(rm.model.repoId) { mutableStateOf<DownloadState>(DownloadState.Idle) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rm.model.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (rm.fits) "✓ fits" else "too big",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (rm.fits) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                String.format(
                    Locale.US,
                    "%s · %.1fB params · %s · ~%.1f GB RAM",
                    rm.model.quant,
                    rm.model.paramsB,
                    rm.model.license.ifBlank { "?" },
                    rm.ramGb,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            when (val s = state) {
                DownloadState.Idle -> Button(
                    onClick = {
                        scope.launch {
                            state = DownloadState.Downloading(0f)
                            try {
                                downloadManager.prefetch(
                                    repoId = rm.model.repoId,
                                    filename = rm.model.filename,
                                    onProgress = { p -> state = DownloadState.Downloading(p) },
                                )
                                state = DownloadState.Installed
                                onModelSelected(rm.model.repoId, rm.model.filename)
                            } catch (t: Throwable) {
                                state = DownloadState.Failed(t.message ?: "Unknown error")
                            }
                        }
                    },
                ) {
                    Text("Download")
                }

                is DownloadState.Downloading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { s.progress },
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${(s.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                DownloadState.Installed -> Text(
                    "✓ Installed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                is DownloadState.Failed -> Text(
                    "Download failed: ${s.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
