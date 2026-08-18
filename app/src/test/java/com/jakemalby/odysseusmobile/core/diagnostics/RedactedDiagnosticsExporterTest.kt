package com.jakemalby.odysseusmobile.core.diagnostics

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactedDiagnosticsExporterTest {
    @Test
    fun `export is deterministic regardless of input ordering`() {
        val first = snapshot()
        val second = first.copy(
            componentHealth = first.componentHealth.reversed(),
            featureHealth = first.featureHealth.reversed(),
            permissionHealth = first.permissionHealth.reversed(),
            cleanupCandidates = first.cleanupCandidates.reversed(),
        )

        assertEquals(
            RedactedDiagnosticsExporter.export(first),
            RedactedDiagnosticsExporter.export(second),
        )
    }

    @Test
    fun `export contains only allow-listed diagnostics and aggregates`() {
        val exported = RedactedDiagnosticsExporter.export(snapshot())

        assertEquals(
            "{\"schemaVersion\":1,\"generatedAtEpochSeconds\":1234," +
                "\"components\":[" +
                "{\"component\":\"DATABASE\",\"level\":\"HEALTHY\",\"reason\":\"NONE\"}," +
                "{\"component\":\"MODEL\",\"level\":\"DEGRADED\",\"reason\":\"MODEL_NOT_LOADED\"}]," +
                "\"features\":[" +
                "{\"feature\":\"CHAT\",\"available\":true}," +
                "{\"feature\":\"VOICE_INPUT\",\"available\":false,\"reason\":\"PERMISSION_REQUIRED\"}]," +
                "\"permissions\":[{\"permission\":\"MICROPHONE\",\"state\":\"DENIED\"}]," +
                "\"cleanup\":[{\"target\":\"MODEL_DOWNLOAD_TEMPORARIES\",\"estimatedBytes\":42," +
                "\"itemCount\":2,\"availabilityReason\":\"READY\",\"canClean\":true}]," +
                "\"aggregates\":{\"installedModelCount\":1,\"indexedDocumentCount\":3," +
                "\"databaseRecordCount\":8,\"appPrivateBytesUsed\":100,\"appPrivateBytesAvailable\":900}}",
            exported,
        )
    }

    @Test
    fun `export contract has no channel for arbitrary private text bytes or paths`() {
        val forbiddenTypes = setOf(
            String::class.java,
            CharSequence::class.java,
            CharArray::class.java,
            ByteArray::class.java,
            java.io.File::class.java,
            java.nio.file.Path::class.java,
            Throwable::class.java,
        )
        val contractClasses = listOf(
            RedactedDiagnosticsSnapshot::class.java,
            ComponentHealth::class.java,
            FeatureHealth::class.java,
            StorageCleanupCandidate::class.java,
            PermissionHealth::class.java,
        )

        contractClasses.flatMap { type ->
            type.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }
        }.forEach { field ->
            assertFalse("unsafe diagnostics field ${field.declaringClass.simpleName}.${field.name}", field.type in forbiddenTypes)
            assertFalse("unsafe diagnostics field ${field.declaringClass.simpleName}.${field.name}", Throwable::class.java.isAssignableFrom(field.type))
        }
    }

    @Test
    fun `adversarial secret and content vocabulary never appears in output schema`() {
        val exported = RedactedDiagnosticsExporter.export(snapshot()).lowercase()
        val hostilePrivateValues = listOf(
            "sk-live-super-secret-api-key",
            "bearer eyjhb.secret.signature",
            "my private prompt says launch at midnight",
            "note body: bank pin 1234",
            "chat message: confidential medical detail",
            "/data/user/0/com.jakemalby.odysseusmobile/files/private.txt",
            "c:\\users\\owner\\documents\\private.txt",
        )
        val forbiddenSchemaTerms = listOf(
            "\"prompt\":", "\"note\":", "\"chat\":", "\"message\":", "\"secret\":",
            "\"token\":", "\"api-key\":", "\"apikey\":", "\"exception\":",
            "\"stacktrace\":", "\"filename\":", "\"filepath\":", "\"path\":", "\"uri\":",
            "\"documenttitle\":",
        )

        (hostilePrivateValues + forbiddenSchemaTerms).forEach { forbidden ->
            assertFalse("diagnostics leaked or defined unsafe value: $forbidden", exported.contains(forbidden.lowercase()))
        }
    }

    @Test
    fun `invalid or contradictory health and cleanup aggregates are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ComponentHealth(DiagnosticComponent.MODEL, HealthLevel.HEALTHY, HealthReason.MODEL_NOT_LOADED)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ComponentHealth(DiagnosticComponent.MODEL, HealthLevel.DEGRADED, HealthReason.NONE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ComponentHealth(DiagnosticComponent.DATABASE, HealthLevel.DEGRADED, HealthReason.MODEL_NOT_LOADED)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StorageCleanupCandidate(
                StorageCleanupTarget.ORPHANED_PRIVATE_BLOBS,
                estimatedBytes = 1,
                itemCount = 0,
                CleanupAvailabilityReason.NOTHING_TO_CLEAN,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapshot().copy(componentHealth = snapshot().componentHealth + snapshot().componentHealth.first())
        }
    }

    @Test
    fun `cleanup readiness requires both ready state and discovered items`() {
        assertTrue(snapshot().cleanupCandidates.single().canClean)
        assertFalse(
            StorageCleanupCandidate(
                StorageCleanupTarget.ORPHANED_PRIVATE_BLOBS,
                estimatedBytes = 0,
                itemCount = 0,
                CleanupAvailabilityReason.SCAN_REQUIRED,
            ).canClean,
        )
    }

    private fun snapshot() = RedactedDiagnosticsSnapshot(
        generatedAtEpochSeconds = 1_234,
        componentHealth = listOf(
            ComponentHealth(DiagnosticComponent.MODEL, HealthLevel.DEGRADED, HealthReason.MODEL_NOT_LOADED),
            ComponentHealth.healthy(DiagnosticComponent.DATABASE),
        ),
        featureHealth = listOf(
            FeatureHealth(LocalFeature.VOICE_INPUT, FeatureAvailability.Unavailable(FeatureUnavailableReason.PERMISSION_REQUIRED)),
            FeatureHealth(LocalFeature.CHAT, FeatureAvailability.Available),
        ),
        permissionHealth = listOf(PermissionHealth(AppPermission.MICROPHONE, PermissionGrantState.DENIED)),
        cleanupCandidates = listOf(
            StorageCleanupCandidate(
                StorageCleanupTarget.MODEL_DOWNLOAD_TEMPORARIES,
                estimatedBytes = 42,
                itemCount = 2,
                CleanupAvailabilityReason.READY,
            ),
        ),
        installedModelCount = 1,
        indexedDocumentCount = 3,
        databaseRecordCount = 8,
        appPrivateBytesUsed = 100,
        appPrivateBytesAvailable = 900,
    )
}
