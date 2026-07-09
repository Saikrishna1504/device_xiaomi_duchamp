/*
 * Copyright (C) 2026 Luxured
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.thermal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.xiaomi.settings.utils.FileUtils

class ThermalAutoModeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (!prefs.getBoolean(ThermalProfileFragment.PREF_AUTO_MODE, false)) return

        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                val current = FileUtils.readLineInt(ThermalProfileFragment.THERMAL_PROFILE_PATH)
                prefs.edit()
                    .putString(PREF_PROFILE_BEFORE_SLEEP, current.toString())
                    .apply()
                applyProfile(prefs, ThermalProfileFragment.THERMAL_PROFILE_MBATTERY)
            }
            Intent.ACTION_SCREEN_ON -> {
                val restored = prefs.getString(
                    PREF_PROFILE_BEFORE_SLEEP,
                    ThermalProfileFragment.THERMAL_PROFILE_DEFAULT.toString()
                )?.toIntOrNull() ?: ThermalProfileFragment.THERMAL_PROFILE_DEFAULT
                applyProfile(prefs, restored)
            }
        }
    }

    private fun applyProfile(prefs: SharedPreferences, profile: Int) {
        FileUtils.writeLine(ThermalProfileFragment.THERMAL_PROFILE_PATH, profile)
        prefs.edit()
            .putString(ThermalProfileFragment.PREF_THERMAL_PROFILE, profile.toString())
            .apply()
    }

    companion object {
        const val PREF_PROFILE_BEFORE_SLEEP = "thermal_profile_before_sleep"

        fun buildIntentFilter() = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
    }
}