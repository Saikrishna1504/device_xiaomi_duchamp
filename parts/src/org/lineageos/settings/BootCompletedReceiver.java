/*
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Display.HdrCapabilities;
import vendor.xiaomi.hw.touchfeature.ITouchFeature;

import org.lineageos.settings.doze.DozeUtils;
import org.lineageos.settings.thermal.ThermalUtils;
import org.lineageos.settings.refreshrate.RefreshUtils;
import org.lineageos.settings.dirac.DiracUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "XiaomiParts";
    private static final boolean DEBUG = true; // Set to true for debugging purposes
    private static final int DOUBLE_TAP_TO_WAKE_MODE = 14;

    private ITouchFeature xiaomiTouchFeatureAidl;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }
        if (DEBUG)
            Log.d(TAG, "Received boot completed intent");
        try {
            DiracUtils.getInstance(context);
        } catch (Exception e) {
            Log.d(TAG, "Dirac is not present in system");
        }

        // Call LineageOS functionalities
        DozeUtils.onBootCompleted(context);
        ThermalUtils.startService(context);
        RefreshUtils.startService(context);

        // Override HDR types to enable Dolby Vision
        final DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        displayManager.overrideHdrTypes(Display.DEFAULT_DISPLAY,
                new int[] {HdrCapabilities.HDR_TYPE_DOLBY_VISION, HdrCapabilities.HDR_TYPE_HDR10,
                        HdrCapabilities.HDR_TYPE_HLG, HdrCapabilities.HDR_TYPE_HDR10_PLUS});

        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateTapToWakeStatus(context);
            }
        };

        context.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DOUBLE_TAP_TO_WAKE), true, observer);

        updateTapToWakeStatus(context);
    }

    private void updateTapToWakeStatus(Context context) {
        try {
            if (xiaomiTouchFeatureAidl == null) {
                try {
                    var name = "default";
                    var fqName =
                            vendor.xiaomi.hw.touchfeature.ITouchFeature.DESCRIPTOR + "/" + name;
                    var binder = android.os.Binder.allowBlocking(
                            android.os.ServiceManager.waitForDeclaredService(fqName));
                    xiaomiTouchFeatureAidl =
                            vendor.xiaomi.hw.touchfeature.ITouchFeature.Stub.asInterface(binder);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to initialize Touch Feature service", e);
                }
            }

            boolean enabled = Settings.Secure.getInt(context.getContentResolver(),
                                      Settings.Secure.DOUBLE_TAP_TO_WAKE, 0)
                    == 1;
            xiaomiTouchFeatureAidl.setTouchMode(0, DOUBLE_TAP_TO_WAKE_MODE, enabled ? 1 : 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update Tap to Wake status", e);
        }
    }
}
