package com.marvinos.model

/**
 * Snapshot of the device's hardware telemetry collected at app launch
 * and refreshed on demand by [com.marvinos.intelligence.DeviceProfiler].
 *
 * This is the payload injected into Gemini prompts for DEVICE_INFO
 * and GAME_COMPAT queries.
 *
 * @property totalRamBytes    Total physical RAM in bytes.
 * @property availRamBytes    Currently available RAM in bytes (refreshed per query).
 * @property freeStorageBytes Free storage on the data partition in bytes.
 * @property totalStorageBytes Total storage capacity in bytes.
 * @property chipsetRaw       Raw value from Build.SOC_MODEL (API 31+) or Build.BOARD.
 * @property chipsetName      Human-readable marketing name from the lookup table,
 *                            or [chipsetRaw] if not found.
 * @property chipsetTier      Capability tier: "flagship" | "upper-mid" | "mid" | "entry" | "unknown".
 * @property chipsetYear      Year of the chipset release, or null if unknown.
 * @property cpuCores         Number of logical CPU cores.
 * @property gpuGlesVersion   OpenGL ES version string, e.g. "3.2".
 * @property displayRefreshHz Screen refresh rate in Hz.
 * @property androidVersion   Android OS version float, e.g. 14.0.
 */
data class DeviceProfile(
    val totalRamBytes: Long = 0L,
    val availRamBytes: Long = 0L,
    val freeStorageBytes: Long = 0L,
    val totalStorageBytes: Long = 0L,
    val chipsetRaw: String = "unknown",
    val chipsetName: String = "unknown",
    val chipsetTier: String = "unknown",
    val chipsetYear: Int? = null,
    val cpuCores: Int = 0,
    val gpuGlesVersion: String = "unknown",
    val displayRefreshHz: Float = 60f,
    val androidVersion: Float = 0f,
    val brand: String = "unknown"
) {
    // Convenience helpers for prompt building

    val totalRamGb: Float get() = totalRamBytes / (1024f * 1024f * 1024f)
    val availRamGb: Float get() = availRamBytes / (1024f * 1024f * 1024f)
    val freeStorageGb: Float get() = freeStorageBytes / (1024f * 1024f * 1024f)
    val totalStorageGb: Float get() = totalStorageBytes / (1024f * 1024f * 1024f)

    /** Serialises the profile to a compact JSON string for Gemini prompt injection. */
    fun toPromptJson(): String = buildString {
        append("{")
        append("\"chipset\":\"$chipsetName\",")
        append("\"chipsetTier\":\"$chipsetTier\",")
        append("\"totalRamGB\":${String.format("%.1f", totalRamGb)},")
        append("\"availRamGB\":${String.format("%.1f", availRamGb)},")
        append("\"storageFreeGB\":${String.format("%.1f", freeStorageGb)},")
        append("\"gpuGles\":\"$gpuGlesVersion\",")
        append("\"cores\":$cpuCores,")
        append("\"displayHz\":${displayRefreshHz.toInt()},")
        append("\"androidVersion\":$androidVersion")
        append("}")
    }

    companion object {
        /** Returned when telemetry could not be collected. */
        val EMPTY = DeviceProfile()
    }
}
