package com.marvinos.intelligence

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import com.marvinos.model.DeviceProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects hardware telemetry to populate [DeviceProfile].
 * Values like total RAM and CPU cores are collected once; volatile stats like
 * available RAM and storage are refreshed dynamically.
 */
@Singleton
class DeviceProfiler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chipsetLookup: ChipsetLookup
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Static stats collected once
    private val totalRamBytes: Long by lazy {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        memoryInfo.totalMem
    }

    private val cpuCores: Int by lazy {
        Runtime.getRuntime().availableProcessors()
    }

    private val gpuGlesVersion: String by lazy {
        activityManager.deviceConfigurationInfo.glEsVersion
    }

    private val displayRefreshHz: Float by lazy {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.refreshRate
    }

    private val chipsetRaw: String by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            Build.BOARD
        }
    }

    private val chipsetInfo: ChipsetLookup.ChipsetTier by lazy {
        chipsetLookup.lookup(chipsetRaw)
    }

    /**
     * Builds a fresh [DeviceProfile] with real-time available RAM and storage.
     */
    fun getCurrentProfile(): DeviceProfile {
        // Refresh available RAM
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // Refresh storage
        val statFs = StatFs(Environment.getDataDirectory().path)
        val blockSize = statFs.blockSizeLong
        val totalStorageBytes = statFs.blockCountLong * blockSize
        val freeStorageBytes = statFs.availableBlocksLong * blockSize

        return DeviceProfile(
            totalRamBytes = totalRamBytes,
            availRamBytes = memoryInfo.availMem,
            freeStorageBytes = freeStorageBytes,
            totalStorageBytes = totalStorageBytes,
            chipsetRaw = chipsetRaw,
            chipsetName = chipsetInfo.name,
            chipsetTier = chipsetInfo.tier,
            chipsetYear = chipsetInfo.year,
            cpuCores = cpuCores,
            gpuGlesVersion = gpuGlesVersion,
            displayRefreshHz = displayRefreshHz,
            androidVersion = Build.VERSION.RELEASE.toFloatOrNull() ?: 0f
        )
    }
}
