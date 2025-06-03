package com.example.headlesscamera;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class CameraService extends Service {

    private static final String CHANNEL_ID = "CameraServiceChannel";
    private static final String TAG = "CameraService";

    public static final String ACTION_OPEN_CAMERA = "com.example.headlesscamera.OPEN_CAMERA";
    public static final String ACTION_START_RECORDING = "com.example.headlesscamera.START_RECORDING";
    public static final String ACTION_STOP_RECORDING = "com.example.headlesscamera.STOP_RECORDING";
    public static final String ACTION_ENABLE_LOOPING = "com.example.headlesscamera.ENABLE_LOOPING";

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private MediaRecorder mediaRecorder;
    private String cameraId;
    private boolean isRecording = false;
    private boolean isLooping = false;

    // Add wake lock to keep service alive
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called - Creating persistent service");

        // Acquire wake lock to prevent service from being killed
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HeadlessCamera::CameraWakeLock");
        wakeLock.acquire();

        createNotificationChannel();
        startForegroundService();

        Log.d(TAG, "Persistent foreground service started - will survive app close");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");

        String action = intent != null ? intent.getAction() : null;
        String command = intent != null ? intent.getStringExtra("command") : null;

        Log.d(TAG, "Received action: " + action + ", command: " + command);

        if (ACTION_START_RECORDING.equals(action) || "start".equals(command)) {
            startCameraAndRecord();
        } else if (ACTION_OPEN_CAMERA.equals(action)) {
            openCameraOnly();
        } else if (ACTION_STOP_RECORDING.equals(action) || "stop".equals(command)) {
            stopRecording();
        } else if (ACTION_ENABLE_LOOPING.equals(action)) {
            enableLooping();
        }

        // CRITICAL: Return START_STICKY to restart service if killed
        return START_STICKY;
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("📹 Headless Camera Service")
                .setContentText("🔴 Ready to record (runs in background)")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true) // Prevent user from dismissing
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(1, notification);
    }

    private void updateNotification(String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("📹 Headless Camera Service")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(1, notification);
        }
    }

    private void enableLooping() {
        isLooping = true;
        Log.d(TAG, "Looping enabled");
        updateNotification("🔄 Looping mode enabled");
    }

    private void startCameraAndRecord() {
        Log.d(TAG, "📹 Starting camera and record request");

        if (!checkPermissions()) {
            Log.e(TAG, "❌ Permissions not granted");
            updateNotification("❌ Permissions missing");
            return;
        }

        if (isRecording) {
            Log.w(TAG, "⚠️ Already recording");
            updateNotification("⚠️ Already recording");
            return;
        }

        updateNotification("🔄 Starting camera...");

        openCamera(() -> {
            startRecording();
            updateNotification("🔴 RECORDING... (background)");
        });
    }

    private void openCameraOnly() {
        Log.d(TAG, "📷 Opening camera only");

        if (!checkPermissions()) {
            Log.e(TAG, "❌ Permissions not granted");
            return;
        }

        openCamera(() -> {
            Log.d(TAG, "✅ Camera opened successfully");
            updateNotification("📷 Camera ready (background)");
        });
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void openCamera(Runnable onSuccess) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0]; // Back camera

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "❌ Camera permission not granted");
                return;
            }

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    Log.d(TAG, "✅ Camera opened successfully");
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "📷 Camera disconnected");
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "❌ Camera error: " + error);
                    camera.close();
                    cameraDevice = null;
                    updateNotification("❌ Camera error: " + error);
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "❌ CameraAccessException: " + e.getMessage());
            updateNotification("❌ Camera access failed");
        }
    }

    private void startRecording() {
        if (cameraDevice == null) {
            Log.e(TAG, "❌ Camera is not opened");
            updateNotification("❌ Camera not ready");
            return;
        }

        if (isRecording) {
            Log.w(TAG, "⚠️ Already recording");
            return;
        }

        Log.d(TAG, "🔴 Setting up recording...");
        setupMediaRecorder();

        try {
            cameraDevice.createCaptureSession(
                    Collections.singletonList(mediaRecorder.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                                builder.addTarget(mediaRecorder.getSurface());
                                session.setRepeatingRequest(builder.build(), null, null);

                                mediaRecorder.start();
                                isRecording = true;
                                Log.d(TAG, "🎬 RECORDING STARTED (BACKGROUND MODE)");
                                updateNotification("🔴 RECORDING... (minimized app OK)");

                            } catch (CameraAccessException e) {
                                Log.e(TAG, "❌ CameraAccessException: " + e.getMessage());
                                updateNotification("❌ Recording failed");
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "❌ Camera configuration failed");
                            updateNotification("❌ Camera config failed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "❌ CameraAccessException on session: " + e.getMessage());
        }
    }

    private void setupMediaRecorder() {
        mediaRecorder = new MediaRecorder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder = new MediaRecorder(this);
        }

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        // Create output file with timestamp
        File outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File outputFile = new File(outputDir, "recording_" + timestamp + ".mp4");
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncodingBitRate(5000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1920, 1080);

        try {
            mediaRecorder.prepare();
            Log.d(TAG, "🎬 MediaRecorder prepared, output: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "❌ MediaRecorder prepare failed: " + e.getMessage());
        }
    }

    private void stopRecording() {
        Log.d(TAG, "⏹️ Stop recording requested");

        if (!isRecording) {
            Log.w(TAG, "⚠️ Not currently recording");
            updateNotification("⚠️ Not recording");
            return;
        }

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }

            isRecording = false;
            Log.d(TAG, "✅ RECORDING STOPPED");
            updateNotification("⏹️ Recording stopped (ready for next)");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error stopping recording: " + e.getMessage());
            updateNotification("❌ Error stopping recording");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🔥 Service destroyed - cleaning up");

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        stopRecording();

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "📱 App removed from recent apps - service continues running");
        updateNotification("📱 App closed - service still running");

        // Restart service if app is swiped away
        Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.setPackage(getPackageName());
        startService(restartService);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Headless Camera Service",
                    NotificationManager.IMPORTANCE_HIGH // High importance to prevent killing
            );
            serviceChannel.setDescription("Background camera recording service");
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}