package com.genos.accessibility

import android.content.Context
import android.content.Intent
import android.provider.Settings

object PermissionChecker {
    
    private const val ACCESSIBILITY_SERVICE_NAME = "com.genos.accessibility/.GenosAccessibilityService"

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            settingValue.split(':').any { service ->
                service.trim() in listOf(
                    ACCESSIBILITY_SERVICE_NAME,
                    "com.genos.accessibility/com.genos.accessibility.GenosAccessibilityService"
                )
            }
        } catch (e: Exception) {
            false
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return try {
            Settings.canDrawOverlays(context)
        } catch (e: Exception) {
            false
        }
    }
}