/*
 * Copyright (C) 2024 Paranoid Android
 * Copyright (C) 2026 Luxured
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.thermal

import android.app.Fragment
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.xiaomi.settings.R
import com.xiaomi.settings.utils.FileUtils

class ThermalProfileFragment : Fragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mSharedPrefs: SharedPreferences

    private lateinit var mCardBg: View
    private lateinit var mTvProfileName: TextView
    private lateinit var mIvCardIcon: ImageView

    private lateinit var mRadioDefault: ImageView
    private lateinit var mRadioPerformance: ImageView
    private lateinit var mRadioBattery: ImageView
    private lateinit var mRadioGame: ImageView

    private lateinit var mIconDefault: ImageView
    private lateinit var mIconPerformance: ImageView
    private lateinit var mIconBattery: ImageView
    private lateinit var mIconGame: ImageView

    private lateinit var mSwitchAutoMode: Switch

    private val profileColors = mapOf(
        THERMAL_PROFILE_DEFAULT      to Color.parseColor("#2E7D32"),
        THERMAL_PROFILE_MPERFORMANCE to Color.parseColor("#B71C1C"),
        THERMAL_PROFILE_MBATTERY     to Color.parseColor("#1565C0"),
        THERMAL_PROFILE_MGAME        to Color.parseColor("#6A1B9A")
    )

    private val profileNames = mapOf(
        THERMAL_PROFILE_DEFAULT      to R.string.thermalprofile_default,
        THERMAL_PROFILE_MPERFORMANCE to R.string.thermalprofile_performance,
        THERMAL_PROFILE_MBATTERY     to R.string.thermalprofile_battery,
        THERMAL_PROFILE_MGAME        to R.string.thermalprofile_game
    )

    private val profileIcons = mapOf(
        THERMAL_PROFILE_DEFAULT      to R.drawable.ic_thermal_default,
        THERMAL_PROFILE_MPERFORMANCE to R.drawable.ic_thermal_performance,
        THERMAL_PROFILE_MBATTERY     to R.drawable.ic_thermal_battery,
        THERMAL_PROFILE_MGAME        to R.drawable.ic_thermal_gaming
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_thermal_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)

        mCardBg        = view.findViewById(R.id.card_current_mode_bg)
        mTvProfileName = view.findViewById(R.id.tv_current_profile_name)
        mIvCardIcon    = view.findViewById(R.id.iv_current_mode_icon)

        mRadioDefault     = view.findViewById(R.id.radio_default)
        mRadioPerformance = view.findViewById(R.id.radio_performance)
        mRadioBattery     = view.findViewById(R.id.radio_battery)
        mRadioGame        = view.findViewById(R.id.radio_game)

        mIconDefault     = view.findViewById(R.id.icon_default)
        mIconPerformance = view.findViewById(R.id.icon_performance)
        mIconBattery     = view.findViewById(R.id.icon_battery)
        mIconGame        = view.findViewById(R.id.icon_game)

        mSwitchAutoMode = view.findViewById(R.id.switch_auto_mode)
        mSwitchAutoMode.isChecked = mSharedPrefs.getBoolean(PREF_AUTO_MODE, false)
        mSwitchAutoMode.setOnCheckedChangeListener { _, checked ->
            mSharedPrefs.edit().putBoolean(PREF_AUTO_MODE, checked).apply()
            val serviceIntent = Intent(activity, ThermalAutoModeService::class.java)
            if (checked) activity.startService(serviceIntent)
            else         activity.stopService(serviceIntent)
        }

        view.findViewById<View>(R.id.row_default)    .setOnClickListener { selectProfile(THERMAL_PROFILE_DEFAULT) }
        view.findViewById<View>(R.id.row_performance).setOnClickListener { selectProfile(THERMAL_PROFILE_MPERFORMANCE) }
        view.findViewById<View>(R.id.row_battery)    .setOnClickListener { selectProfile(THERMAL_PROFILE_MBATTERY) }
        view.findViewById<View>(R.id.row_game)       .setOnClickListener { selectProfile(THERMAL_PROFILE_MGAME) }

        syncFromSysfs()
    }

    override fun onResume() {
        super.onResume()
        syncFromSysfs()
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key == PREF_THERMAL_PROFILE) {
            val profile = prefs.getString(PREF_THERMAL_PROFILE, THERMAL_PROFILE_DEFAULT.toString())
                ?.toIntOrNull() ?: THERMAL_PROFILE_DEFAULT
            updateUi(profile)
        }
    }

    private fun selectProfile(profile: Int) {
        FileUtils.writeLine(THERMAL_PROFILE_PATH, profile)
        mSharedPrefs.edit().putString(PREF_THERMAL_PROFILE, profile.toString()).apply()
        updateUi(profile)
    }

    private fun syncFromSysfs() {
        val current = FileUtils.readLineInt(THERMAL_PROFILE_PATH)
        if (mSharedPrefs.getString(PREF_THERMAL_PROFILE, null) != current.toString()) {
            mSharedPrefs.edit().putString(PREF_THERMAL_PROFILE, current.toString()).apply()
        }
        updateUi(current)
    }

    private fun updateUi(activeProfile: Int) {
        val color = profileColors[activeProfile] ?: profileColors[THERMAL_PROFILE_DEFAULT]!!
        mCardBg.setBackgroundColor(color)

        val darkColor = blendColors(color, Color.BLACK, 0.30f)
        mCardBg.background = buildSplitGradient(color, darkColor)

        mTvProfileName.text = getString(profileNames[activeProfile] ?: R.string.thermalprofile_unknown)
        mIvCardIcon.setImageResource(profileIcons[activeProfile] ?: R.drawable.ic_thermal_default)

        val radios = mapOf(
            THERMAL_PROFILE_DEFAULT      to mRadioDefault,
            THERMAL_PROFILE_MPERFORMANCE to mRadioPerformance,
            THERMAL_PROFILE_MBATTERY     to mRadioBattery,
            THERMAL_PROFILE_MGAME        to mRadioGame
        )
        radios.forEach { (profile, radio) ->
            radio.setImageResource(
                if (profile == activeProfile) android.R.drawable.radiobutton_on_background
                else android.R.drawable.radiobutton_off_background
            )
        }

        val icons = mapOf(
            THERMAL_PROFILE_DEFAULT      to mIconDefault,
            THERMAL_PROFILE_MPERFORMANCE to mIconPerformance,
            THERMAL_PROFILE_MBATTERY     to mIconBattery,
            THERMAL_PROFILE_MGAME        to mIconGame
        )
        icons.forEach { (profile, icon) ->
            if (profile == activeProfile) {
                icon.setColorFilter(color)
            } else {
                icon.clearColorFilter()
            }
        }
    }

    private fun buildSplitGradient(startColor: Int, endColor: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        ).also {
            it.cornerRadius = 0f
        }
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val r = (Color.red(color1)   * (1 - ratio) + Color.red(color2)   * ratio).toInt()
        val g = (Color.green(color1) * (1 - ratio) + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1)  * (1 - ratio) + Color.blue(color2)  * ratio).toInt()
        return Color.rgb(r, g, b)
    }

    companion object {
        const val THERMAL_PROFILE_PATH         = "/sys/class/thermal/thermal_message/sconfig"
        const val PREF_THERMAL_PROFILE         = "thermal_profile"
        const val PREF_AUTO_MODE               = "thermal_auto_mode"
        const val THERMAL_PROFILE_DEFAULT      = 50
        const val THERMAL_PROFILE_MPERFORMANCE = 18
        const val THERMAL_PROFILE_MBATTERY     = 700
        const val THERMAL_PROFILE_MGAME        = 19
    }
}