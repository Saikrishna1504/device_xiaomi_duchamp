/*
 * Copyright (C) 2026 Luxured
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.thermal

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.preference.PreferenceManager

/**
 * Long-running service that registers ThermalAutoModeReceiver for
 * ACTION_SCREEN_OFF / ACTION_SCREEN_ON (these cannot be declared in the
 * manifest — they must be registered in code).
 *
 * Started by BootCompletedReceiver and also toggled by the Auto Mode switch
 * in ThermalProfileFragment.
 */
class ThermalAutoModeService : Service() {

    private val mReceiver = ThermalAutoModeReceiver()
    private lateinit var mPrefs: SharedPreferences
    private val mPrefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == ThermalProfileFragment.PREF_AUTO_MODE) {
            // If auto mode was just disabled, stop self
            if (!prefs.getBoolean(ThermalProfileFragment.PREF_AUTO_MODE, false)) {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(mReceiver, ThermalAutoModeReceiver.buildIntentFilter())
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}