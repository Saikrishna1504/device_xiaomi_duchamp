/*
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

 package org.lineageos.settings.display;

 import android.os.IBinder;
 import android.os.RemoteException;
 import android.os.ServiceManager;
 import android.util.Log;
 
 import vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature;
 
 public class DfWrapper {
 
     private static final String TAG = "LineagePartsDisplayFeatureWrapper";
 
     private static IDisplayFeature mDisplayFeature;
 
     private static final IBinder.DeathRecipient mDeathRecipient = () -> {
         Log.d(TAG, "serviceDied");
         mDisplayFeature = null;
     };
 
     public static IDisplayFeature getDisplayFeature() {
         if (mDisplayFeature == null) {
             Log.d(TAG, "getDisplayFeature: mDisplayFeature=null");
             try {
                 var name = "default";
                 var fqName = IDisplayFeature.DESCRIPTOR + "/" + name;
                 var binder = android.os.Binder.allowBlocking(ServiceManager.waitForDeclaredService(fqName));
                 mDisplayFeature = IDisplayFeature.Stub.asInterface(binder);
 
                 // Link to death
                 binder.linkToDeath(mDeathRecipient, 0);
 
                 Log.d(TAG, "Binded DisplayFeature");
             } catch (Throwable t) {
                 Log.e(TAG, "getDisplayFeature failed!", t);
             }
         }
         return mDisplayFeature;
     }
 
     public static void setDisplayFeature(DfParams params) {
         final IDisplayFeature displayFeature = getDisplayFeature();
         if (displayFeature == null) {
             Log.e(TAG, "setDisplayFeatureParams: displayFeature is null!");
             return;
         }
         Log.d(TAG, "setDisplayFeatureParams: " + params);
         try {
             displayFeature.setFeature(0, params.mode, params.value, params.cookie);
         } catch (RemoteException e) {
             Log.e(TAG, "setDisplayFeatureParams failed!", e);
         }
     }
 
     public static class DfParams {
         /* displayfeature parameters */
         final int mode, value, cookie;
 
         public DfParams(int mode, int value, int cookie) {
             this.mode = mode;
             this.value = value;
             this.cookie = cookie;
         }
 
         public String toString() {
             return "DisplayFeatureParams(" + mode + ", " + value + ", " + cookie + ")";
         }
     }
 }