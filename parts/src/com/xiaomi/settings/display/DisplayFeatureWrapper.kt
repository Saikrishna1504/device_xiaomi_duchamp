/*
 * SPDX-FileCopyrightText: 2023-2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.display

import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature

object DisplayFeatureWrapper {

    private const val TAG = "DisplayFeatureWrapper"
    private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

    @Volatile private var displayFeature: IDisplayFeature? = null

    private val deathRecipient = IBinder.DeathRecipient {
        if (DEBUG) Log.d(TAG, "serviceDied")
        displayFeature = null
    }

    @Synchronized
    private fun getDisplayFeature(): IDisplayFeature? {
        displayFeature?.let {
            if (it.asBinder().isBinderAlive) return it
        }
        return try {
            val binder = ServiceManager.waitForService("vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature/default")
            val service = IDisplayFeature.Stub.asInterface(binder)
            service?.asBinder()?.linkToDeath(deathRecipient, 0)
            displayFeature = service
            if (DEBUG) Log.d(TAG, "Connected to DisplayFeature service")
            service
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get DisplayFeature service", e)
            null
        }
    }

    fun setFeature(mode: Int, value: Int, cookie: Int) {
        val displayFeature = getDisplayFeature() ?: run {
            Log.e(TAG, "displayFeature is null!")
            return
        }
        if (DEBUG) Log.d(TAG, "setFeature: mode=$mode, value=$value, cookie=$cookie")
        runCatching { displayFeature.setFeature(/*displayId*/ 0, mode, value, cookie) }
            .onFailure { e -> Log.e(TAG, "setFeature failed!", e) }
    }
}
