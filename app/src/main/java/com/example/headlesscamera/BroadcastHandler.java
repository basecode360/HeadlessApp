package com.example.headlesscamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastHandler extends BroadcastReceiver {

    private static final String TAG = "BroadcastHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Broadcast received: " + action);

        Intent serviceIntent = new Intent(context, CameraService.class);
        serviceIntent.setAction(action);

        // FIXED: Updated action names to match what you're sending
        if ("com.example.headlesscamera.START_RECORDING".equals(action)) {
            serviceIntent.putExtra("command", "start");
            Log.d(TAG, "Starting recording service");
        } else if ("com.example.headlesscamera.STOP_RECORDING".equals(action)) {
            serviceIntent.putExtra("command", "stop");
            Log.d(TAG, "Stopping recording service");
        } else if ("com.example.headlesscamera.OPEN_CAMERA".equals(action)) {
            serviceIntent.putExtra("command", "open");
            Log.d(TAG, "Opening camera service");
        } else if ("com.example.headlesscamera.ENABLE_LOOPING".equals(action)) {
            serviceIntent.putExtra("command", "loop");
            Log.d(TAG, "Enabling looping mode");
        }

        context.startForegroundService(serviceIntent);
    }
}