package com.nextgis.maplibui.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nextgis.maplibui.util.ConstantsUI;

public class TrackerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.ACTION_TRACKER_MESSAGE".equals(intent.getAction())) {
            String message = intent.getStringExtra(ConstantsUI.KEY_MESSAGE);
            // Передай в Activity через LiveData, EventBus, или напрямую
            Log.e("TrackerReceiver", "Got message: " + message);
        }
    }
}
