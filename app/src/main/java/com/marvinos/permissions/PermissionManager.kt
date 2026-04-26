package com.marvinos.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.marvinos.model.ActionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages runtime permissions for the Action Engine.
 *
 * Rules:
 * - Just-in-time requests only.
 * - Map each action to its required runtime permission.
 * - Note: Special permissions (WRITE_SETTINGS, PACKAGE_USAGE_STATS, BIND_ACCESSIBILITY_SERVICE)
 *   are handled separately by [SpecialPermissionHelper] as they require intent-based flows.
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val specialPermissionHelper: SpecialPermissionHelper
) {

    /**
     * Checks if the required permission for [action] is granted.
     *
     * @return null if granted or no permission needed, otherwise the Android permission string.
     */
    fun getMissingPermissionFor(action: ActionType): String? {
        val permission = getRequiredPermissionFor(action) ?: return null
        
        // Handle special permissions via helper
        if (specialPermissionHelper.isSpecialPermission(permission)) {
            return if (specialPermissionHelper.hasSpecialPermission(permission)) null else permission
        }
        
        // Standard runtime permissions
        return if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            null
        } else {
            permission
        }
    }

    /**
     * Maps an [ActionType] to its primary required Android permission.
     */
    private fun getRequiredPermissionFor(action: ActionType): String? {
        return when (action) {
            ActionType.TOGGLE_BLUETOOTH -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_CONNECT
                } else {
                    Manifest.permission.BLUETOOTH_ADMIN
                }
            }
            ActionType.TOGGLE_FLASHLIGHT -> Manifest.permission.CAMERA
            ActionType.SET_BRIGHTNESS -> SpecialPermissionHelper.PERMISSION_WRITE_SETTINGS
            ActionType.OPTIMIZE -> SpecialPermissionHelper.PERMISSION_USAGE_STATS
            // Wifi toggle on API 29+ uses Settings.Panel (no runtime perm needed for the intent).
            // Pre-29 it uses CHANGE_WIFI_STATE which is normal protection level.
            else -> null
        }
    }

    /**
     * Returns a plain-English rationale for why a permission is needed.
     */
    fun getRationaleFor(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN -> "MarvinOS needs Bluetooth access to turn it on or off for you."
            Manifest.permission.CAMERA -> "MarvinOS needs camera access to control the flashlight."
            SpecialPermissionHelper.PERMISSION_WRITE_SETTINGS -> "MarvinOS needs permission to modify system settings to change the screen brightness."
            SpecialPermissionHelper.PERMISSION_USAGE_STATS -> "MarvinOS needs usage access to find apps you haven't used recently."
            SpecialPermissionHelper.PERMISSION_ACCESSIBILITY -> "MarvinOS uses the Accessibility Service to navigate system settings menus automatically."
            else -> "MarvinOS needs this permission to complete the action."
        }
    }
}
