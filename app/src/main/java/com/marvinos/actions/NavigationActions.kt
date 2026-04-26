package com.marvinos.actions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.marvinos.model.ActionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles: OPEN_SETTINGS.
 *
 * Implements real Intent launches for standard Android settings screens.
 */
@Singleton
class NavigationActions @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Opens the appropriate settings screen for [target].
     */
    suspend fun openSettings(target: String?): ActionResult {
        val intentAction = when (target?.lowercase()?.trim()) {
            "wifi", "wi-fi", "wi fi"     -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth"                   -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "screen"          -> Settings.ACTION_DISPLAY_SETTINGS
            "battery"                    -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "storage"                    -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            "accessibility"              -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "apps", "applications"       -> Settings.ACTION_APPLICATION_SETTINGS
            "sound", "audio", "volume"   -> Settings.ACTION_SOUND_SETTINGS
            "location"                   -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            else                         -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(intentAction).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            
            val screenName = target?.takeIf { it.isNotBlank() } ?: "Settings"
            ActionResult.Success("Opening $screenName ✓")
        } catch (e: Exception) {
            ActionResult.Failed("Could not open settings screen.")
        }
    }
}
