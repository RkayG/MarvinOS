package com.marvinos.actions

import com.marvinos.model.ActionResult
import com.marvinos.model.ActionType
import com.marvinos.model.ParsedIntent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central router — maps every [ParsedIntent] to the correct action handler
 * and returns an [ActionResult].
 *
 * Pipeline:
 *  1. Check [ParsedIntent.isReadOnly] → skip confirmation gate if true.
 *  2. Route to the appropriate handler by [ActionType].
 *  3. Return [ActionResult] to the ViewModel for UI rendering.
 *
 * This class is pure Kotlin with zero Android dependencies in its own logic;
 * Android API calls are encapsulated inside each handler class.
 */
@Singleton
class ActionExecutor @Inject constructor(
    private val systemToggleActions: SystemToggleActions,
    private val navigationActions: NavigationActions,
    private val optimizeActions: OptimizeActions,
    private val deviceIntelligenceActions: DeviceIntelligenceActions
) {

    /**
     * Executes [intent] and returns the result.
     *
     * The confirmation gate is handled in the ViewModel layer — by the time
     * this method is called, the user has already confirmed state-changing actions.
     *
     * @param intent Validated, user-confirmed [ParsedIntent].
     * @return       [ActionResult] describing what happened.
     */
    suspend fun execute(intent: ParsedIntent): ActionResult {
        return when (intent.action) {

            // ── System Toggles ────────────────────────────────────────────────
            ActionType.TOGGLE_WIFI ->
                systemToggleActions.toggleWifi(intent.value)

            ActionType.TOGGLE_BLUETOOTH ->
                systemToggleActions.toggleBluetooth(intent.value)

            ActionType.TOGGLE_FLASHLIGHT ->
                systemToggleActions.toggleFlashlight(intent.value)

            ActionType.SET_BRIGHTNESS ->
                systemToggleActions.setBrightness(intent.target)

            ActionType.TOGGLE_DARK_MODE ->
                systemToggleActions.toggleDarkMode(intent.value)

            // ── Navigation ────────────────────────────────────────────────────
            ActionType.OPEN_SETTINGS ->
                navigationActions.openSettings(intent.target)

            // ── Optimize & Storage ────────────────────────────────────────────
            ActionType.OPTIMIZE ->
                optimizeActions.optimizeDevice()

            ActionType.FREE_SPACE ->
                optimizeActions.freeSpace()

            // ── Device Intelligence ───────────────────────────────────────────
            ActionType.DEVICE_INFO ->
                deviceIntelligenceActions.getDeviceInfo()

            ActionType.GAME_COMPAT ->
                deviceIntelligenceActions.checkGameCompat(intent.target ?: "")

            // ── Unknown / Fallthrough ─────────────────────────────────────────
            ActionType.UNKNOWN ->
                ActionResult.Failed(
                    reason = "I didn't recognise that command. Try something like: " +
                            "\"Turn off WiFi\", \"Flashlight on\", or \"Check my specs\".",
                    isRetryable = false
                )
        }
    }

    companion object {
        /**
         * Returns true for actions that require user confirmation before execution.
         * Read-only queries (DEVICE_INFO, GAME_COMPAT, FREE_SPACE) skip the gate.
         */
        fun requiresConfirmation(intent: ParsedIntent): Boolean = !intent.isReadOnly
    }
}
