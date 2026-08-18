package com.jakemalby.odysseusmobile.model

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import android.content.pm.PackageManager

/** Android facts used to build a [DeviceProfile], gathered without any network access. */
data class AndroidDeviceFacts(
    val manufacturer: String,
    val model: String,
    val apiLevel: Int,
    val availableRamBytes: Long,
    val totalRamBytes: Long,
    val availableStorageBytes: Long,
    val isLowRamDevice: Boolean,
    val confirmedBackends: Set<ModelBackend>,
) {
    init {
        require(apiLevel > 0)
        require(availableRamBytes >= 0)
        require(totalRamBytes >= 0)
        require(availableStorageBytes >= 0)
        require(confirmedBackends.isNotEmpty()) { "At least one confirmed backend is required" }
    }

    fun asDeviceProfile(): DeviceProfile = DeviceProfile(
        apiLevel = apiLevel,
        availableRamBytes = availableRamBytes,
        availableStorageBytes = availableStorageBytes,
        availableBackends = confirmedBackends,
    )

    /** Samsung model names vary by region, so this is deliberately advisory only. */
    val looksLikeGalaxyS25: Boolean
        get() = manufacturer.equals("samsung", ignoreCase = true) &&
            model.uppercase().let { it.startsWith("SM-S93") || it.contains("S25") }
}

/**
 * Probes RAM, storage and API level from Android. Backend detection is injected:
 * Android has no portable API that proves LiteRT-LM GPU/NPU loading will succeed.
 * The default is CPU-only rather than a false positive.
 */
class AndroidDeviceProfileProbe(
    private val context: Context,
    private val confirmedBackends: (() -> Set<ModelBackend>)? = null,
) {
    fun probe(): AndroidDeviceFacts {
        val activityManager = context.getSystemService(ActivityManager::class.java)
            ?: error("ActivityManager is unavailable")
        val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val storage = StatFs(context.filesDir.absolutePath)
        return AndroidDeviceFacts(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            apiLevel = Build.VERSION.SDK_INT,
            availableRamBytes = memory.availMem.coerceAtLeast(0),
            totalRamBytes = memory.totalMem.coerceAtLeast(0),
            availableStorageBytes = storage.availableBytes.coerceAtLeast(0),
            isLowRamDevice = activityManager.isLowRamDevice,
            confirmedBackends = (confirmedBackends?.invoke() ?: detectBackends()).ifEmpty { setOf(ModelBackend.CPU) },
        )
    }

    private fun detectBackends(): Set<ModelBackend> = buildSet {
        add(ModelBackend.CPU)
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)) {
            add(ModelBackend.GPU)
        }
    }
}
