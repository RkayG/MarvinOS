package com.marvinos.actions

import com.marvinos.model.ActionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles: TOGGLE_WIFI, TOGGLE_BLUETOOTH, TOGGLE_FLASHLIGHT, SET_BRIGHTNESS, TOGGLE_DARK_MODE.
 *
 * Sprint 2: stub implementations that return realistic [ActionResult] values.
 * Sprint 5: replace each stub body with real Android API calls.
 */
@Singleton
class SystemToggleActions @Inject constructor() {

    /**
     * Toggle WiFi on/off.
     *
     * Real impl (Sprint 5):
     *  - API 29+: launch Settings.Panel.ACTION_WIFI (programmatic toggle removed)
     *  - API <29: WifiManager.setWifiEnabled()
     *
     * @param enable true = enable, false = disable, null = toggle current state.
     */
    suspend fun toggleWifi(enable: Boolean?): ActionResult {
        val state = if (enable == false) "off" else "on"
        // TODO Sprint 5: replace with real WiFi toggle
        return ActionResult.Success("WiFi turned $state ✓")
    }

    /**
     * Toggle Bluetooth on/off.
     *
     * Real impl (Sprint 5):
     *  - API 33+: BLUETOOTH_CONNECT permission + BluetoothAdapter
     *  - API <33: BLUETOOTH_ADMIN permission + BluetoothAdapter.enable()/disable()
     *
     * @param enable true = enable, false = disable, null = toggle current state.
     */
    suspend fun toggleBluetooth(enable: Boolean?): ActionResult {
        val state = if (enable == false) "off" else "on"
        // TODO Sprint 5: replace with real Bluetooth toggle
        return ActionResult.Success("Bluetooth turned $state ✓")
    }

    /**
     * Toggle the camera flashlight/torch.
     *
     * Real impl (Sprint 5): CameraManager.setTorchMode()
     * Requires CAMERA permission.
     *
     * @param enable true = on, false = off, null = toggle.
     */
    suspend fun toggleFlashlight(enable: Boolean?): ActionResult {
        val state = if (enable == false) "off" else "on"
        // TODO Sprint 5: replace with CameraManager.setTorchMode()
        return ActionResult.Success("Flashlight turned $state ✓")
    }

    /**
     * Set screen brightness to [level].
     *
     * Real impl (Sprint 5): Settings.System.putInt(SCREEN_BRIGHTNESS, value)
     * Requires WRITE_SETTINGS special permission.
     *
     * @param level Human-readable level: "max", "high", "medium", "low", "min",
     *              or a numeric string like "75".
     */
    suspend fun setBrightness(level: String?): ActionResult {
        val display = level ?: "medium"
        // TODO Sprint 5: replace with Settings.System.SCREEN_BRIGHTNESS write
        return ActionResult.Success("Brightness set to $display ✓")
    }

    /**
     * Toggle dark mode on/off.
     *
     * Real impl (Sprint 5): AppCompatDelegate.setDefaultNightMode()
     * No permission required — takes effect immediately.
     *
     * @param enable true = dark mode on, false = light mode, null = toggle.
     */
    suspend fun toggleDarkMode(enable: Boolean?): ActionResult {
        val state = if (enable == false) "off (Light mode)" else "on (Dark mode)"
        // TODO Sprint 5: replace with AppCompatDelegate.setDefaultNightMode()
        return ActionResult.Success("Dark mode $state ✓")
    }
}
