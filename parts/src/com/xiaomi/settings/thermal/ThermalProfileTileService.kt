/*
 * Copyright (C) 2024 Paranoid Android
 * Copyright (C) 2026 Luxured
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xiaomi.settings.thermal

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager
import com.xiaomi.settings.R
import com.xiaomi.settings.utils.FileUtils

class ThermalProfileTileService : TileService() {
    companion object {
        private const val THEMRAL_PROFILE_PATH = "/sys/class/thermal/thermal_message/sconfig"
        private const val THEMRAL_PROFILE_DEFAULT = 50
        private const val THEMRAL_PROFILE_MPERFORMANCE = 18
        private const val THEMRAL_PROFILE_MBATTERY = 700
        private const val THEMRAL_PROFILE_MGAME = 19
    }

    private fun updateUI(profile: Int) {
        val tile = qsTile
        tile.label = getString(R.string.thermalprofile_title)
        tile.subtitle = when (profile) {
            THEMRAL_PROFILE_DEFAULT -> getString(R.string.thermalprofile_default)
            THEMRAL_PROFILE_MPERFORMANCE -> getString(R.string.thermalprofile_performance)
            THEMRAL_PROFILE_MBATTERY -> getString(R.string.thermalprofile_battery)
            THEMRAL_PROFILE_MGAME -> getString(R.string.thermalprofile_game)
            else -> getString(R.string.thermalprofile_unknown)
        }
        tile.icon = Icon.createWithResource(this, when (profile) {
            THEMRAL_PROFILE_DEFAULT -> R.drawable.ic_thermal_default
            THEMRAL_PROFILE_MPERFORMANCE -> R.drawable.ic_thermal_performance
            THEMRAL_PROFILE_MBATTERY -> R.drawable.ic_thermal_battery
            THEMRAL_PROFILE_MGAME -> R.drawable.ic_thermal_gaming
            else -> R.drawable.ic_thermal_default
        })
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    private fun applyProfile(profile: Int) {
        FileUtils.writeLine(THEMRAL_PROFILE_PATH, profile)
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putString(ThermalProfileFragment.PREF_THERMAL_PROFILE, profile.toString())
            .apply()
    }

    override fun onStartListening() {
        super.onStartListening()
        val current = FileUtils.readLineInt(THEMRAL_PROFILE_PATH)
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putString(ThermalProfileFragment.PREF_THERMAL_PROFILE, current.toString())
            .apply()
        updateUI(current)
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val currentThermalProfile = FileUtils.readLineInt(THEMRAL_PROFILE_PATH)
        val newThermalProfile = when (currentThermalProfile) {
            THEMRAL_PROFILE_DEFAULT -> THEMRAL_PROFILE_MPERFORMANCE
            THEMRAL_PROFILE_MPERFORMANCE -> THEMRAL_PROFILE_MBATTERY
            THEMRAL_PROFILE_MBATTERY -> THEMRAL_PROFILE_MGAME
            THEMRAL_PROFILE_MGAME -> THEMRAL_PROFILE_DEFAULT
            else -> THEMRAL_PROFILE_DEFAULT
        }
        applyProfile(newThermalProfile)
        updateUI(newThermalProfile)
    }
}