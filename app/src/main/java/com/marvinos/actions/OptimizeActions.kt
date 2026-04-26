package com.marvinos.actions

import com.marvinos.model.ActionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles: OPTIMIZE, FREE_SPACE.
 *
 * Sprint 2: stubs with realistic guidance output.
 * Sprint 6: replace with real StatFs + StorageManager calls.
 */
@Singleton
class OptimizeActions @Inject constructor() {

    /**
     * Runs a device optimisation scan.
     *
     * Real impl (Sprint 6):
     *  - StatFs to measure free/used storage
     *  - PACKAGE_USAGE_STATS (optional) to identify unused apps
     *  - Launch StorageManager.ACTION_MANAGE_STORAGE if space is critically low
     *  - Returns NeedsGuidance steps if PACKAGE_USAGE_STATS not granted
     */
    suspend fun optimizeDevice(): ActionResult {
        // TODO Sprint 6: real storage scan + app usage analysis
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
     *
     * Real impl (Sprint 6):
     *  - StatFs(Environment.getDataDirectory()) for free/total bytes
     *  - Launch StorageManager.ACTION_MANAGE_STORAGE intent for system cleanup
     */
    suspend fun freeSpace(): ActionResult {
        // TODO Sprint 6: replace with real StatFs scan + StorageManager intent
        return ActionResult.NeedsGuidance(
            steps = listOf(
                "Go to Settings → Storage to see a full breakdown.",
                "Tap 'Free up space' to let Android identify files to delete.",
                "Clear app caches: Settings → Apps → [App Name] → Clear Cache.",
                "Review and delete large files in your Downloads folder.",
                "Back up and remove offline media (music, podcasts, videos)."
            )
        )
    }
}
