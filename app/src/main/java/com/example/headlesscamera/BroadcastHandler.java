package com.example.headlesscamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BroadcastHandler extends BroadcastReceiver {

    private static final String TAG = "BroadcastHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "🔔 Broadcast received: " + action);

        if (action == null) {
            Log.e(TAG, "❌ Received null action");
            return;
        }

        // SOLUTION: Start MainActivity first to get app into foreground state
        // This bypasses Android 14+ background service restrictions
        try {
            Log.d(TAG, "🚀 Starting MainActivity first to bypass background restrictions");
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activityIntent.putExtra("auto_command", getCommandFromAction(action));
            activityIntent.putExtra("auto_action", action);

            // Start activity first - this gets app into foreground
            context.startActivity(activityIntent);
            Log.d(TAG, "✅ MainActivity started, service will start from there");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start activity: " + e.getMessage());

            // Fallback: Try direct service start (will likely fail on Android 14+)
            try {
                Log.d(TAG, "🔄 Attempting fallback direct service start");
                Intent serviceIntent = new Intent(context, CameraService.class);
                serviceIntent.setAction(action);
                serviceIntent.putExtra("command", getCommandFromAction(action));

                context.startForegroundService(serviceIntent);
                Log.d(TAG, "📱 Fallback service start attempted");
            } catch (Exception e2) {
                Log.e(TAG, "❌ Fallback also failed: " + e2.getMessage());
            }
        }
    }

    private String getCommandFromAction(String action) {
        if ("com.example.headlesscamera.START_RECORDING".equals(action)) {
            return "start";
        } else if ("com.example.headlesscamera.STOP_RECORDING".equals(action)) {
            return "stop";
        } else if ("com.example.headlesscamera.OPEN_CAMERA".equals(action)) {
            return "open";
        } else if ("com.example.headlesscamera.ENABLE_LOOPING".equals(action)) {
            return "loop";
        }
        return "unknown";
    }
}