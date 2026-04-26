package com.marvinos.actions

import com.marvinos.model.ActionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles: DEVICE_INFO, GAME_COMPAT.
 *
 * Sprint 2: stubs returning realistic placeholder output.
 * Sprint 6: replace with real DeviceProfiler data + Gemini prompt results.
 */
@Singleton
class DeviceIntelligenceActions @Inject constructor() {

    /**
     * Returns plain-English hardware info for this device.
     *
     * Real impl (Sprint 6):
     *  - DeviceProfiler collects RAM, storage, chipset, GPU, refresh rate
     *  - DeviceContextBuilder wraps it in a Gemini prompt
     *  - GeminiApiClient returns a plain-English summary
     */
    suspend fun getDeviceInfo(): ActionResult {
        // TODO Sprint 6: replace with DeviceProfiler + GeminiApiClient call
        return ActionResult.Success(
            """
            📱 Device Overview (sample data)
            • Chipset: Snapdragon 778G (upper-mid tier, 2021)
            • RAM: 6 GB total · ~2.1 GB free right now
            • Storage: 128 GB total · ~4.2 GB free
            • GPU: Adreno 642L (OpenGL ES 3.2)
            • CPU: 8 cores
            • Display: 120Hz
            • Android: 14
            """.trimIndent()
        )
    }

    /**
     * Checks whether this device can run [gameName] and at what quality.
     *
     * Real impl (Sprint 6):
     *  - Load game requirements from assets/game_db.json
     *  - DeviceProfiler collects current hardware
     *  - DeviceContextBuilder + GameCompatChecker builds the Gemini prompt
     *  - GeminiApiClient returns a plain-English compatibility verdict
     *
     * @param gameName The game title as understood from the user's message.
     */
    suspend fun checkGameCompat(gameName: String): ActionResult {
        if (gameName.isBlank()) {
            return ActionResult.Failed(
                reason = "Which game would you like to check? Try: \"Can I run Genshin Impact?\"",
                isRetryable = false
            )
        }
        // TODO Sprint 6: replace with real game DB lookup + Gemini verdict
        return ActionResult.Success(
            """
            🎮 $gameName compatibility (sample verdict)
            ✅ Your device should run $gameName well.
            • Expected performance: Good (medium–high settings)
            • RAM: Sufficient (6 GB vs 4 GB recommended)
            • GPU: Compatible (OpenGL ES 3.2)
            💡 Tip: Close background apps before launching for best performance.
            """.trimIndent()
        )
    }
}
