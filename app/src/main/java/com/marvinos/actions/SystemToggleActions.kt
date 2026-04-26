package com.marvinos.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import com.marvinos.model.ActionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles: TOGGLE_WIFI, TOGGLE_BLUETOOTH, TOGGLE_FLASHLIGHT, SET_BRIGHTNESS, TOGGLE_DARK_MODE.
 *
 * Implements real Android API calls for state modification.
 */
@Singleton
class SystemToggleActions @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Toggle WiFi on/off.
     *
     * - API 29+: launch Settings.Panel.ACTION_WIFI (programmatic toggle removed)
     * - API <29: WifiManager.setWifiEnabled()
     */
    suspend fun toggleWifi(enable: Boolean?): ActionResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return ActionResult.Success("Opening WiFi settings panel...")
        } else {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val currentState = wifiManager.isWifiEnabled
            val newState = enable ?: !currentState
            
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = newState
            
            val stateStr = if (newState) "on" else "off"
            return ActionResult.Success("WiFi turned $stateStr ✓")
        }
    }

    /**
     * Toggle Bluetooth on/off.
     * Note: Programmatically toggling BT is heavily restricted or deprecated in modern Android.
     * We open the BT settings panel as the most reliable fallback.
     */
    suspend fun toggleBluetooth(enable: Boolean?): ActionResult {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ActionResult.Success("Opening Bluetooth settings...")
    }

    /**
     * Toggle the camera flashlight/torch.
     */
    suspend fun toggleFlashlight(enable: Boolean?): ActionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return ActionResult.Failed("No camera found.")
            
            // Note: Since we don't track state easily without a callback, if 'enable' is null, 
            // we default to turning it ON for the 'toggle' command.
            val targetState = enable ?: true
            
            cameraManager.setTorchMode(cameraId, targetState)
            val stateStr = if (targetState) "on" else "off"
            ActionResult.Success("Flashlight turned $stateStr ✓")
        } catch (e: Exception) {
            ActionResult.Failed("Failed to access flashlight: ${e.message}")
        }
    }

    /**
     * Set screen brightness.
     * Requires WRITE_SETTINGS permission (handled by confirmation/permission gate).
     */
    suspend fun setBrightness(level: String?): ActionResult {
        val brightnessValue = when (level?.lowercase()) {
            "min", "lowest" -> 10
            "low" -> 64
            "medium", "half" -> 128
            "high" -> 192
            "max", "full", "highest" -> 255
            else -> {
                // Try parse numeric
                val parsed = level?.replace("%", "")?.toIntOrNull()
                if (parsed != null) {
                    (parsed * 255) / 100
                } else {
                    128 // Default medium
                }
            }
        }.coerceIn(0, 255)

        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
            ActionResult.Success("Brightness adjusted ✓")
        } catch (e: SecurityException) {
            ActionResult.Failed("Missing permission to change brightness.")
        } catch (e: Exception) {
            ActionResult.Failed("Failed to change brightness.")
        }
    }

    /**
     * Toggle dark mode on/off.
     */
    suspend fun toggleDarkMode(enable: Boolean?): ActionResult {
        val mode = when (enable) {
            true -> AppCompatDelegate.MODE_NIGHT_YES
            false -> AppCompatDelegate.MODE_NIGHT_NO
            null -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        // This must run on the main thread ideally, but AppDelegate handles it safely.
        androidx.compose.ui.platform.AndroidUiDispatcher.Main.dispatch(kotlin.coroutines.EmptyCoroutineContext, Runnable {
            AppCompatDelegate.setDefaultNightMode(mode)
        })
        
        val stateStr = when(enable) {
            true -> "on (Dark mode)"
            false -> "off (Light mode)"
            else -> "to system default"
        }
        return ActionResult.Success("Theme set $stateStr ✓")
    }
}
