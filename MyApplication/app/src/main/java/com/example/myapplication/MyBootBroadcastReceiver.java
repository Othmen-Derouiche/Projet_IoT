package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class MyBootBroadcastReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "AppPreferences";
    private static final String KEY_START_AT_BOOT = "startAtBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Log to verify the receiver works
        Log.d("MyBootReceiver", "Device booted, received BOOT_COMPLETED intent");

        // Check SharedPreferences for the checkbox setting
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isAutoStartEnabled = preferences.getBoolean(KEY_START_AT_BOOT, false);

        // If auto-start is enabled, start the service
        if (isAutoStartEnabled) {
            Intent serviceIntent = new Intent(context, MainService.class);
            context.startService(serviceIntent);
            Log.d("MyBootReceiver", "Service started because auto-start is enabled");
        }
    }
}
