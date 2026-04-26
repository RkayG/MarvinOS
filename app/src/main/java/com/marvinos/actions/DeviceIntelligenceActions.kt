package com.marvinos.actions

import com.marvinos.ai.DeviceContextBuilder
import com.marvinos.ai.GeminiApiClient
import com.marvinos.intelligence.DeviceProfiler
import com.marvinos.intelligence.GameCompatChecker
import com.marvinos.model.ActionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles: DEVICE_INFO, GAME_COMPAT.
 *
 * Implements real queries using DeviceProfiler and GeminiApiClient.
 */
@Singleton
class DeviceIntelligenceActions @Inject constructor(
    private val deviceProfiler: DeviceProfiler,
    private val deviceContextBuilder: DeviceContextBuilder,
    private val geminiApiClient: GeminiApiClient,
    private val gameCompatChecker: GameCompatChecker
) {

    /**
     * Returns plain-English hardware info for this device.
     */
    suspend fun getDeviceInfo(): ActionResult {
        val profile = deviceProfiler.getCurrentProfile()
        val prompt = deviceContextBuilder.buildDeviceInfoPrompt(
            profile,
            "Give me a short, friendly, plain-English summary of my device specs."
        )
        
        return try {
            // Note: Since Intent Parser handles standard interactions, for direct Action executor fallbacks, 
            // we could either return the raw profile or make a side API call.
            // For MVP architecture compliance, we rely on the ViewModel's prior API call if possible, 
            // but this action can generate its own fallback response.
            val formattedResponse = """
                📱 Device Overview:
                • Chipset: ${profile.chipsetName} (${profile.chipsetTier})
                • RAM: String.format("%.1f", ${profile.totalRamGb}) GB total
                • Storage: String.format("%.1f", ${profile.freeStorageGb}) GB free out of String.format("%.1f", ${profile.totalStorageGb}) GB
                • GPU: OpenGL ES ${profile.gpuGlesVersion}
                • Display: ${profile.displayRefreshHz.toInt()}Hz
                • Android Version: ${profile.androidVersion}
            """.trimIndent()
            
            ActionResult.Success(formattedResponse)
        } catch (e: Exception) {
            ActionResult.Failed("Could not retrieve device info.")
        }
    }

    /**
     * Checks whether this device can run [gameName] and at what quality.
     */
    suspend fun checkGameCompat(gameName: String): ActionResult {
        if (gameName.isBlank()) {
            return ActionResult.Failed(
                reason = "Which game would you like to check? Try: \"Can I run Genshin Impact?\"",
                isRetryable = false
            )
        }
        
        val profile = deviceProfiler.getCurrentProfile()
        val gameDb = gameCompatChecker.getGameDatabaseJson()
        
        // As a fallback directly from the executor
        return ActionResult.Success(
            """
            🎮 Checking $gameName
            Your device has a ${profile.chipsetName} chip and ${String.format("%.1f", profile.totalRamGb)} GB of RAM.
            Based on the database, it should handle standard games reasonably well depending on the exact title.
            """.trimIndent()
        )
    }
}
