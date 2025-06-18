package com.example.headlesscamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE = 1001;
    private static final String TAG = "MainActivity";

    private String pendingCommand = null;
    private String pendingAction = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🏗️ MainActivity onCreate");

        // Check if this was started by BroadcastHandler with auto command
        Intent intent = getIntent();
        if (intent != null) {
            pendingCommand = intent.getStringExtra("auto_command");
            pendingAction = intent.getStringExtra("auto_action");
            Log.d(TAG, "🎯 Auto command received: " + pendingCommand + ", action: " + pendingAction);
        }

        // Don't set content view - keep it headless
        // setContentView(R.layout.activity_main);

        checkAndRequestPermissions();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "🔄 onNewIntent called");

        if (intent != null) {
            String command = intent.getStringExtra("auto_command");
            String action = intent.getStringExtra("auto_action");

            if (command != null) {
                Log.d(TAG, "🎯 New auto command: " + command);
                pendingCommand = command;
                pendingAction = action;

                // If permissions are already granted, process immediately
                if (checkAllPermissions()) {
                    processPendingCommand();
                }
            }
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.FOREGROUND_SERVICE_CAMERA,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28+
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        boolean allGranted = checkAllPermissions();

        if (!allGranted) {
            Log.d(TAG, "📋 Requesting permissions");
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_CODE);
        } else {
            Log.d(TAG, "✅ All permissions granted");
            startCameraServiceAndProcessCommand();
        }
    }

    private boolean checkAllPermissions() {
        boolean cameraOk = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean audioOk = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, "📷 Camera permission: " + cameraOk);
        Log.d(TAG, "🎤 Audio permission: " + audioOk);

        return cameraOk && audioOk;
    }

    private void startCameraServiceAndProcessCommand() {
        Log.d(TAG, "🚀 Starting camera service from foreground context");

        // Start the service from foreground context (this bypasses background restrictions)
        Intent serviceIntent = new Intent(this, CameraService.class);

        // Add any pending command
        if (pendingAction != null) {
            serviceIntent.setAction(pendingAction);
            serviceIntent.putExtra("command", pendingCommand);
            Log.d(TAG, "📦 Adding pending command to service: " + pendingCommand);
        }

        try {
            ContextCompat.startForegroundService(this, serviceIntent);
            Log.d(TAG, "✅ Service started successfully from foreground");

            // Process any additional commands after a short delay
            if (pendingCommand != null) {
                new Handler().postDelayed(this::processPendingCommand, 1000);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start service: " + e.getMessage());
        }

        // Finish activity after starting service (keep it headless)
        finish();
    }

    private void processPendingCommand() {
        if (pendingCommand == null) return;

        Log.d(TAG, "⚡ Processing pending command: " + pendingCommand);

        Intent serviceIntent = new Intent(this, CameraService.class);
        serviceIntent.setAction(pendingAction);
        serviceIntent.putExtra("command", pendingCommand);

        try {
            ContextCompat.startForegroundService(this, serviceIntent);
            Log.d(TAG, "✅ Pending command sent to service");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to send pending command: " + e.getMessage());
        }

        // Clear pending command
        pendingCommand = null;
        pendingAction = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "📋 Permission result received");

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            Log.d(TAG, "📋 All permissions granted: " + allGranted);

            if (allGranted) {
                startCameraServiceAndProcessCommand();
            } else {
                Log.w(TAG, "⚠️ Some permissions denied, finishing");
                finish();
            }
        }
    }
}