package com.mobdysseus.app.cookbook

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs

data class DeviceHardware(
    val socModel: String,
    val model: String,
    val totalRamGb: Float,
    val usableRamGb: Float,
    val hasNpu: Boolean,
    val npuTops: Float,
    val freeStorageGb: Float,
)

/**
 * Detects the running device's AI-relevant hardware. On the Galaxy S25 this is
 * fixed (Snapdragon 8 Elite, 12 GB RAM, Hexagon NPU), so detection is trivial
 * and deterministic — no subprocess probing like the desktop Cookbook needs.
 */
object HardwareDetector {
    fun detect(context: Context): DeviceHardware {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val totalRamGb = mem.totalMem / (1024.0 * 1024.0 * 1024.0)

        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        } ?: "Unknown"

        val hasNpu = soc.contains("Snapdragon", ignoreCase = true) ||
            soc.contains("Elite", ignoreCase = true) ||
            soc.contains("Hexagon", ignoreCase = true)
        val npuTops = if (hasNpu) 45f else 0f

        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val freeGb = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)

        return DeviceHardware(
            socModel = soc,
            model = Build.MODEL ?: "Unknown",
            totalRamGb = totalRamGb.toFloat(),
            // Reserve ~3.5 GB for OS + One UI + app overhead
            usableRamGb = (totalRamGb - 3.5).toFloat().coerceAtLeast(2f),
            hasNpu = hasNpu,
            npuTops = npuTops,
            freeStorageGb = freeGb.toFloat(),
        )
    }
}
