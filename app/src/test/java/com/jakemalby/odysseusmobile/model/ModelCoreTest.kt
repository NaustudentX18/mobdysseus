package com.jakemalby.odysseusmobile.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModelCoreTest {
    @Test
    fun compatibilityRejectsInsufficientRamAndStorage() {
        val entry = entry(bytes = 2_000, ram = 4_000)
        val result = ModelCompatibilityChecker.check(
            entry,
            DeviceProfile(36, availableRamBytes = 3_000, availableStorageBytes = 1_000, availableBackends = setOf(ModelBackend.GPU)),
        )
        assertTrue(result is ModelCompatibility.Incompatible)
        val reasons = (result as ModelCompatibility.Incompatible).reasons
        assertTrue(ModelCompatibility.Reason.INSUFFICIENT_RAM in reasons)
        assertTrue(ModelCompatibility.Reason.INSUFFICIENT_STORAGE in reasons)
    }

    @Test
    fun compatibilityChoosesPreferredMutualBackend() {
        val entry = entry(backends = setOf(ModelBackend.GPU, ModelBackend.CPU))
        val result = ModelCompatibilityChecker.check(
            entry,
            DeviceProfile(36, Long.MAX_VALUE, Long.MAX_VALUE, setOf(ModelBackend.GPU, ModelBackend.CPU)),
        )
        assertEquals(ModelCompatibility.Compatible(ModelBackend.GPU), result)
    }

    @Test
    fun stageValidateAndActivateUsesVerifiedBytes() {
        withTempDirectory { directory ->
            val bytes = "verified local model".toByteArray()
            val hash = Sha256.of(bytes.inputStream())
            val entry = entry(bytes = bytes.size.toLong(), hash = hash)
            val store = ModelArtifactStore(directory)

            store.stage(entry, bytes.inputStream())
            assertEquals(ArtifactValidation.Valid, store.validateStaged(entry))
            val active = store.activate(entry)

            assertTrue(active.isFile)
            assertTrue(store.isActiveValid(entry))
            assertFalse(store.stagingFile(entry).exists())
        }
    }

    @Test(expected = IllegalStateException::class)
    fun invalidStagedArtifactCannotActivate() {
        withTempDirectory { directory ->
            val entry = entry(bytes = 50, hash = "0".repeat(64))
            val store = ModelArtifactStore(directory)
            store.stage(entry, "wrong".byteInputStream())
            store.activate(entry)
        }
    }

    @Test
    fun importPolicyKeepsStorageReserveAndRejectsWrongFiles() {
        assertEquals(
            ModelImportPolicy.Decision.Rejected(ModelImportPolicy.Reason.WRONG_EXTENSION),
            ModelImportPolicy.evaluate("model.bin", 2_000_000, Long.MAX_VALUE),
        )
        assertEquals(
            ModelImportPolicy.Decision.Rejected(ModelImportPolicy.Reason.INSUFFICIENT_STORAGE),
            ModelImportPolicy.evaluate(
                "model.litertlm",
                2_000_000,
                ModelImportPolicy.STORAGE_RESERVE_BYTES + 1_000_000,
            ),
        )
        val allowed = ModelImportPolicy.evaluate(
            "MODEL.LITERTLM",
            2_000_000,
            ModelImportPolicy.STORAGE_RESERVE_BYTES + 3_000_000,
        )
        assertEquals(ModelImportPolicy.Decision.Allowed(3_000_000), allowed)
    }

    @Test
    fun s25ProfileCompatibilityExplainsEveryLimit() {
        val profile = BuiltinS25ModelCatalog.value.profiles.last()
        val result = profile.compatibility(
            DeviceProfile(25, 1, 1, setOf(ModelBackend.CPU)),
        ) as S25ProfileCompatibility.Limited
        assertEquals(
            listOf(
                S25ProfileCompatibility.Reason.API_LEVEL,
                S25ProfileCompatibility.Reason.RAM,
                S25ProfileCompatibility.Reason.STORAGE,
                S25ProfileCompatibility.Reason.BACKEND,
            ),
            result.reasons,
        )
    }

    private fun entry(
        bytes: Long = 16,
        ram: Long = 16,
        hash: String = "a".repeat(64),
        backends: Set<ModelBackend> = setOf(ModelBackend.GPU),
    ) = ModelCatalogEntry(
        id = "test-model",
        version = "1",
        displayName = "Test model",
        artifactUri = "https://models.example/test.litertlm",
        sha256 = hash,
        byteSize = bytes,
        minRamBytes = ram,
        minApiLevel = 26,
        format = ModelFormat.LITERT_LM,
        supportedBackends = backends,
        capabilities = setOf(ModelCapability.TEXT),
    )

    private fun withTempDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("mobdysseus-model-test").toFile()
        try {
            block(directory)
        } finally {
            directory.walkBottomUp().forEach(File::delete)
        }
    }
}
