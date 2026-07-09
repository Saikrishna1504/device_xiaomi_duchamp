/*
 * Copyright (C) 2021 Chaldeaprjkt
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

package com.xiaomi.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.xiaomi.settings.thermal.ThermalProfileActivity;

public class TileEntryActivity extends Activity {
    private static final String TAG = "TileEntryActivity";
    private static final String THERMAL_TILE = "com.xiaomi.settings.thermal.ThermalProfileTileService";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentName sourceComponent = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);
        if (sourceComponent == null) {
            Log.e(TAG, "ComponentName is null, finishing activity");
            finish();
            return;
        }

        String sourceClassName = sourceComponent.getClassName();
        Intent intent = null;

        if (THERMAL_TILE.equals(sourceClassName)) {
            intent = new Intent(this, ThermalProfileActivity.class);
        } else {
            Log.e(TAG, "Unknown tile: " + sourceClassName);
            finish();
            return;
        }

        openActivitySafely(intent);
    }

    private void openActivitySafely(Intent dest) {
        try {
            dest.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(dest);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found for " + dest, e);
        } finally {
            finish();
        }
    }
}