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

class ThermalAutoModeService : Service() {

    private val mReceiver = ThermalAutoModeReceiver()
    private lateinit var mPrefs: SharedPreferences
    private val mPrefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == ThermalProfileFragment.PREF_AUTO_MODE) {
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