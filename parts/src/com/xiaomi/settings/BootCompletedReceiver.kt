/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder;
import android.hardware.display.DisplayManager;
import android.util.Log
import android.view.Display;
import android.view.Display.HdrCapabilities;
import androidx.preference.PreferenceManager
import com.xiaomi.settings.display.ColorService
import com.xiaomi.settings.thermal.ThermalAutoModeService
import com.xiaomi.settings.thermal.ThermalProfileFragment
import com.xiaomi.settings.utils.FileUtils

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.d(TAG, "Received boot completed intent: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> onBootCompleted(context)
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> onLockedBootCompleted(context)
        }

        // Override HDR types
        val displayManager = context.getSystemService(DisplayManager::class.java)
        displayManager.overrideHdrTypes(
            Display.DEFAULT_DISPLAY,
            intArrayOf(
                HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                HdrCapabilities.HDR_TYPE_HDR10,
                HdrCapabilities.HDR_TYPE_HLG,
                HdrCapabilities.HDR_TYPE_HDR10_PLUS
            )
        )

        // Restore thermal profile from SharedPreferences
        restoreThermalProfile(context)

        // Re-start the auto mode service if it was enabled before reboot
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(ThermalProfileFragment.PREF_AUTO_MODE, false)) {
            context.startService(Intent(context, ThermalAutoModeService::class.java))
        }
    }

    private fun onBootCompleted(context: Context) {
    }

    private fun onLockedBootCompleted(context: Context) {
        // Display
        ColorService.startService(context)
    }

    private fun restoreThermalProfile(context: Context) {
        val storedValue = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(
                ThermalProfileFragment.PREF_THERMAL_PROFILE,
                ThermalProfileFragment.THERMAL_PROFILE_DEFAULT.toString()
            )
        val profile = storedValue?.toIntOrNull() ?: ThermalProfileFragment.THERMAL_PROFILE_DEFAULT
        FileUtils.writeLine(ThermalProfileFragment.THERMAL_PROFILE_PATH, profile)
        if (DEBUG) Log.d(TAG, "Restored thermal profile: $profile")
    }

    companion object {
        private const val TAG = "BootReceiver"
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }
}
