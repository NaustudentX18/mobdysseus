package com.mobdysseus.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobdysseus.app.cookbook.Catalog
import com.mobdysseus.app.cookbook.DeviceHardware
import com.mobdysseus.app.cookbook.ModelRanker
import com.mobdysseus.app.cookbook.RankedModel
import java.util.Locale

@Composable
fun CookbookScreen(hardware: DeviceHardware) {
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
                ModelRow(rm)
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

@Composable
private fun ModelRow(rm: RankedModel) {
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
        }
    }
}
