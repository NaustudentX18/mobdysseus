package com.jakemalby.odysseusmobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jakemalby.odysseusmobile.capability.CapabilityCatalog
import com.jakemalby.odysseusmobile.capability.CapabilityDescriptor
import com.jakemalby.odysseusmobile.capability.CapabilityExecutionPolicy
import com.jakemalby.odysseusmobile.capability.CapabilityId
import com.jakemalby.odysseusmobile.capability.CreateTaskCall
import com.jakemalby.odysseusmobile.capability.PolicyOutcome
import com.jakemalby.odysseusmobile.capability.PolicyRecord

/**
 * Read-only policy inspection plus a deliberately non-executing approval demo.
 *
 * This component owns no Android adapters. Approving its sample proposal only
 * exercises the policy state machine, so it cannot create a task or trigger a
 * device/external side effect.
 */
@Composable
internal fun CapabilityPanel(modifier: Modifier = Modifier) {
    val policy = remember { CapabilityExecutionPolicy() }
    val allowlisted = remember {
        CapabilityId.entries.mapNotNull(CapabilityCatalog::descriptor)
    }
    var pending by remember { mutableStateOf<PolicyRecord?>(null) }
    var auditRecords by remember { mutableStateOf(emptyList<PolicyRecord>()) }
    var resultMessage by remember { mutableStateOf("No capability has been proposed in this demo.") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Capability permissions", fontWeight = FontWeight.Bold)
            Text(
                "Every sensitive action must match this fixed native allowlist and pass its approval policy.",
                color = Muted,
            )
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    "Always forbidden: shell/subprocess commands, arbitrary filesystem access, raw sockets, unrestricted fetches, and MCP execution.",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            allowlisted.forEach { descriptor -> CapabilityDescriptorRow(descriptor) }

            HorizontalDivider(color = Border)
            Text("Safe approval preview", fontWeight = FontWeight.SemiBold)
            Text(
                "This sample can enter approved or cancelled policy state, but it has no executor and never performs the action.",
                color = Muted,
                fontSize = 12.sp,
            )
            if (pending == null) {
                Button(onClick = {
                    val outcome = policy.request(CreateTaskCall("Private demonstration task"))
                    pending = (outcome as? PolicyOutcome.RequiresApproval)?.record
                    auditRecords = policy.auditTrail()
                    resultMessage = if (pending != null) {
                        "Proposal is waiting for your explicit choice. Nothing has executed."
                    } else {
                        "The policy rejected the demonstration proposal."
                    }
                }) { Text("Propose local task demo") }
            } else {
                Text("Requested: create one app-private task", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        pending?.let { policy.approve(it.request.requestId, approved = true) }
                        pending = null
                        auditRecords = policy.auditTrail()
                        resultMessage = "Approved in the policy preview. No task was created and no action executed."
                    }) { Text("Approve demo") }
                    OutlinedButton(onClick = {
                        pending?.let { policy.cancel(it.request.requestId) }
                        pending = null
                        auditRecords = policy.auditTrail()
                        resultMessage = "Proposal cancelled before execution."
                    }) { Text("Cancel") }
                }
            }
            Text(resultMessage, color = Muted, fontSize = 12.sp)

            if (auditRecords.isNotEmpty()) {
                HorizontalDivider(color = Border)
                Text("Local redacted audit", fontWeight = FontWeight.SemiBold)
                Text(
                    "Payload values are intentionally hidden; only policy metadata is shown.",
                    color = Muted,
                    fontSize = 12.sp,
                )
                auditRecords.takeLast(6).asReversed().forEach { record ->
                    Text(
                        "${record.request.call.capability.displayName()} · ${record.state.displayName()} · ${record.descriptor?.risk?.displayName() ?: "denied"}",
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityDescriptorRow(descriptor: CapabilityDescriptor) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(descriptor.id.displayName(), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(descriptor.risk.displayName(), color = riskColor(descriptor), fontSize = 12.sp)
        }
        Text(descriptor.rationale, color = Muted, fontSize = 12.sp)
        Text(
            "Scope: ${descriptor.dataScopes.joinToString { it.displayName() }} · ${if (descriptor.requiresApproval) "approval required" else "read-only auto-allow"}",
            color = Muted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun riskColor(descriptor: CapabilityDescriptor) = when (descriptor.risk.name) {
    "HIGH" -> MaterialTheme.colorScheme.error
    "MODERATE" -> Coral
    else -> Success
}

private fun Enum<*>.displayName(): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
