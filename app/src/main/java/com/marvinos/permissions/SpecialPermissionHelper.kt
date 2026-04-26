package com.marvinos.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles "Special App Access" permissions which cannot be requested via standard
 * runtime permission dialogs, requiring intent navigation to Settings.
 */
@Singleton
class SpecialPermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val PERMISSION_WRITE_SETTINGS = "android.permission.WRITE_SETTINGS"
        const val PERMISSION_USAGE_STATS = "android.permission.PACKAGE_USAGE_STATS"
        const val PERMISSION_ACCESSIBILITY = "android.permission.BIND_ACCESSIBILITY_SERVICE"
    }

    private val specialPermissions = setOf(
        PERMISSION_WRITE_SETTINGS,
        PERMISSION_USAGE_STATS,
        PERMISSION_ACCESSIBILITY
    )

    fun isSpecialPermission(permission: String): Boolean = permission in specialPermissions

    fun hasSpecialPermission(permission: String): Boolean {
        return when (permission) {
            PERMISSION_WRITE_SETTINGS -> Settings.System.canWrite(context)
            PERMISSION_USAGE_STATS -> hasUsageStatsPermission()
            PERMISSION_ACCESSIBILITY -> isAccessibilityServiceEnabled()
            else -> false
        }
    }

    fun getSettingsActionFor(permission: String): String {
        return when (permission) {
            PERMISSION_WRITE_SETTINGS -> Settings.ACTION_MANAGE_WRITE_SETTINGS
            PERMISSION_USAGE_STATS -> Settings.ACTION_USAGE_ACCESS_SETTINGS
            PERMISSION_ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            // Assume disabled
        }
        
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (services != null) {
                return services.contains(context.packageName)
            }
        }
        return false
    }
}
