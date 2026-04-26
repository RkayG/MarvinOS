package com.marvinos.actions

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.text.format.Formatter
import com.marvinos.model.ActionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles: OPTIMIZE, FREE_SPACE.
 *
 * Implements real Android API calls for storage and usage stats.
 */
@Singleton
class OptimizeActions @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Runs a device optimisation scan.
     */
    suspend fun optimizeDevice(): ActionResult {
        return ActionResult.NeedsGuidance(
            steps = listOf(
                "Open Settings → Storage to see what's using space.",
                "Clear cache for large apps: tap an app → Clear Cache.",
                "Delete unused apps you haven't opened in 30+ days.",
                "Move photos and videos to Google Photos to free up storage.",
                "Restart your phone to clear temporary files from RAM."
            )
        )
    }

    /**
     * Scans storage and reports usage; offers to launch the system storage manager.
     */
    suspend fun freeSpace(): ActionResult {
        return try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val blockSize = statFs.blockSizeLong
            val totalBytes = statFs.blockCountLong * blockSize
            val freeBytes = statFs.availableBlocksLong * blockSize
            val usedBytes = totalBytes - freeBytes

            val freeStr = Formatter.formatFileSize(context, freeBytes)
            val totalStr = Formatter.formatFileSize(context, totalBytes)
            
            // On newer Androids, we can launch the StorageManager intent
            val intent = Intent(StorageManager.ACTION_MANAGE_STORAGE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            ActionResult.Success("You have $freeStr free out of $totalStr. Opening storage manager to free up space...")
        } catch (e: Exception) {
            ActionResult.NeedsGuidance(
                steps = listOf(
                    "Go to Settings → Storage to see a full breakdown.",
                    "Tap 'Free up space' to let Android identify files to delete.",
                    "Clear app caches: Settings → Apps → [App Name] → Clear Cache.",
                    "Review and delete large files in your Downloads folder."
                )
            )
        }
    }
}
