package com.jakemalby.odysseusmobile.core.diagnostics

/** Deterministic JSON export over the strictly typed, content-free diagnostics contract. */
object RedactedDiagnosticsExporter {
    fun export(snapshot: RedactedDiagnosticsSnapshot): String = buildString {
        append('{')
        field("schemaVersion", snapshot.schemaVersion)
        append(',')
        field("generatedAtEpochSeconds", snapshot.generatedAtEpochSeconds)
        append(",\"components\":[")
        snapshot.componentHealth.sortedBy { it.component.name }.forEachIndexed { index, health ->
            if (index > 0) append(',')
            append("{\"component\":\"").append(health.component.name)
            append("\",\"level\":\"").append(health.level.name)
            append("\",\"reason\":\"").append(health.reason.name).append("\"}")
        }
        append("],\"features\":[")
        snapshot.featureHealth.sortedBy { it.feature.name }.forEachIndexed { index, health ->
            if (index > 0) append(',')
            append("{\"feature\":\"").append(health.feature.name).append("\"")
            when (val availability = health.availability) {
                FeatureAvailability.Available -> append(",\"available\":true")
                is FeatureAvailability.Unavailable -> {
                    append(",\"available\":false,\"reason\":\"")
                    append(availability.reason.name).append('"')
                }
            }
            append('}')
        }
        append("],\"permissions\":[")
        snapshot.permissionHealth.sortedBy { it.permission.name }.forEachIndexed { index, health ->
            if (index > 0) append(',')
            append("{\"permission\":\"").append(health.permission.name)
            append("\",\"state\":\"").append(health.state.name).append("\"}")
        }
        append("],\"cleanup\":[")
        snapshot.cleanupCandidates.sortedBy { it.target.name }.forEachIndexed { index, candidate ->
            if (index > 0) append(',')
            append("{\"target\":\"").append(candidate.target.name).append("\"")
            append(",\"estimatedBytes\":").append(candidate.estimatedBytes)
            append(",\"itemCount\":").append(candidate.itemCount)
            append(",\"availabilityReason\":\"").append(candidate.availabilityReason.name)
            append("\",\"canClean\":").append(candidate.canClean).append('}')
        }
        append("],\"aggregates\":{")
        field("installedModelCount", snapshot.installedModelCount)
        append(',')
        field("indexedDocumentCount", snapshot.indexedDocumentCount)
        append(',')
        field("databaseRecordCount", snapshot.databaseRecordCount)
        append(',')
        field("appPrivateBytesUsed", snapshot.appPrivateBytesUsed)
        append(',')
        field("appPrivateBytesAvailable", snapshot.appPrivateBytesAvailable)
        append("}}")
    }

    private fun StringBuilder.field(name: String, value: Number) {
        append('"').append(name).append("\":").append(value)
    }
}
