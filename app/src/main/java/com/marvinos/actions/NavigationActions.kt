package com.marvinos.actions

import com.marvinos.model.ActionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles: OPEN_SETTINGS.
 *
 * Sprint 2: stub returning success.
 * Sprint 5: replace with real startActivity(Intent(Settings.ACTION_*)) calls.
 *
 * Only standard Android Settings intents are used — never OEM-specific deep links.
 */
@Singleton
class NavigationActions @Inject constructor() {

    /**
     * Opens the appropriate settings screen for [target].
     *
     * Supported targets and their real intents (Sprint 5):
     *  - "wifi"           → Settings.ACTION_WIFI_SETTINGS
     *  - "bluetooth"      → Settings.ACTION_BLUETOOTH_SETTINGS
     *  - "display"        → Settings.ACTION_DISPLAY_SETTINGS
     *  - "battery"        → Settings.ACTION_BATTERY_SAVER_SETTINGS
     *  - "storage"        → Settings.ACTION_INTERNAL_STORAGE_SETTINGS
     *  - "accessibility"  → Settings.ACTION_ACCESSIBILITY_SETTINGS
     *  - "apps"           → Settings.ACTION_APPLICATION_SETTINGS
     *  - "sound"          → Settings.ACTION_SOUND_SETTINGS
     *  - "location"       → Settings.ACTION_LOCATION_SOURCE_SETTINGS
     *  - null / other     → Settings.ACTION_SETTINGS (main settings)
     */
    suspend fun openSettings(target: String?): ActionResult {
        val screen = when (target?.lowercase()?.trim()) {
            "wifi", "wi-fi", "wi fi"     -> "Wi-Fi Settings"
            "bluetooth"                   -> "Bluetooth Settings"
            "display", "screen"          -> "Display Settings"
            "battery"                    -> "Battery Settings"
            "storage"                    -> "Storage Settings"
            "accessibility"              -> "Accessibility Settings"
            "apps", "applications"       -> "App Settings"
            "sound", "audio", "volume"   -> "Sound Settings"
            "location"                   -> "Location Settings"
            else                         -> "Settings"
        }
        // TODO Sprint 5: replace with context.startActivity(Intent(Settings.ACTION_*))
        return ActionResult.Success("Opening $screen ✓")
    }
}
