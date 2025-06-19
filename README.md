# HeadlessCamera - Android Background Camera Recording Service

A powerful Android application that provides background camera recording capabilities without requiring a visible UI. Perfect for surveillance, monitoring, or automated recording scenarios where the app needs to continue recording even when minimized or the device is locked.

## 🎯 Features

- **Truly Headless Operation**: Records video without requiring the app to stay open
- **Background Service**: Continues recording even when app is minimized or removed from recent apps
- **Foreground Service**: Ensures recording survives system memory cleanup
- **Boot Persistence**: Automatically starts service on device boot
- **Broadcast Control**: Control recording via ADB commands or intents
- **Wake Lock**: Prevents device from sleeping during recording
- **Multiple Control Methods**: Activity starts, broadcast intents, and service commands

## 📋 Requirements

- Android 10+ (API 30+)
- Camera and microphone permissions
- Storage access for saving recordings
- ADB access for installation and control

## 🚀 Installation & Setup

### 1. Build the Project

```bash
./gradlew assembleDebug
```

### 2. Install via ADB

```bash
# Install the APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Alternative: Push to device and install via shell
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/
adb shell pm install -r /data/local/tmp/app-debug.apk
```

### 3. Grant Required Permissions

```bash
# Grant critical permissions (required for Android 14+)
adb shell pm grant com.example.headlesscamera android.permission.CAMERA
adb shell pm grant com.example.headlesscamera android.permission.RECORD_AUDIO
adb shell pm grant com.example.headlesscamera android.permission.FOREGROUND_SERVICE
adb shell pm grant com.example.headlesscamera android.permission.FOREGROUND_SERVICE_CAMERA

# Set app operations
adb shell appops set com.example.headlesscamera CAMERA allow
adb shell appops set com.example.headlesscamera RECORD_AUDIO allow
```

## 📱 Usage

### Method 1: Direct Activity Start (Recommended)

**Start Recording:**
```bash
adb shell am start \
  -n com.example.headlesscamera/.MainActivity \
  --es auto_command start \
  --es auto_action com.example.headlesscamera.START_RECORDING
```

**Stop Recording:**
```bash
adb shell am start \
  -n com.example.headlesscamera/.MainActivity \
  --es auto_command stop \
  --es auto_action com.example.headlesscamera.STOP_RECORDING
```

### Method 2: Broadcast Intents

**Start Recording:**
```bash
adb shell am broadcast \
  -n com.example.headlesscamera/.BroadcastHandler \
  -a com.example.headlesscamera.START_RECORDING
```

**Stop Recording:**
```bash
adb shell am broadcast \
  -n com.example.headlesscamera/.BroadcastHandler \
  -a com.example.headlesscamera.STOP_RECORDING
```

**Enable Looping Mode:**
```bash
adb shell am broadcast \
  -n com.example.headlesscamera/.BroadcastHandler \
  -a com.example.headlesscamera.ENABLE_LOOPING
```

### Method 3: Shell Commands (On Device)

If you have shell access directly on the device:

```bash
# Start recording
am start \
  -n com.example.headlesscamera/.MainActivity \
  --es auto_command start \
  --es auto_action com.example.headlesscamera.START_RECORDING

# Wait for recording duration
sleep 30

# Stop recording
am start \
  -n com.example.headlesscamera/.MainActivity \
  --es auto_command stop \
  --es auto_action com.example.headlesscamera.STOP_RECORDING
```

## 📁 Retrieving Recordings

### Check for Recorded Files
```bash
adb shell find /storage/emulated/0/Android/data/com.example.headlesscamera/ -name "*.mp4" -o -name "latest_output.txt" 2>/dev/null
```

### Download Recordings to Computer
```bash
# Create local directory
mkdir recordings

# Pull all recordings
adb pull /storage/emulated/0/Android/data/com.example.headlesscamera/files/Movies/ ./recordings/

# Pull latest output info
adb pull /storage/emulated/0/Android/data/com.example.headlesscamera/files/latest_output.txt
```

## 🔧 Configuration

The app uses default recording settings:
- **Resolution**: 1920x1080 (1080p)
- **Frame Rate**: 30 FPS
- **Video Codec**: H.264
- **Audio Codec**: AAC
- **Bitrate**: 5 Mbps

To modify these settings, edit the `setupMediaRecorder()` method in `CameraService.java`.

## 🛠️ Maintenance Commands

### Uninstall App
```bash
adb shell pm uninstall com.example.headlesscamera
```

### Check Service Status
```bash
adb shell dumpsys activity services | grep HeadlessCamera
```

### View Logs
```bash
adb logcat -s CameraService
```

### Clear App Data
```bash
adb shell pm clear com.example.headlesscamera
```

## 🎯 Use Cases

- **Security Monitoring**: Discrete recording for surveillance
- **Automated Recording**: Triggered recording based on external events
- **Remote Recording**: Control via ADB from another device
- **Background Documentation**: Recording while using other apps
- **Testing**: Automated camera testing scenarios

## ⚠️ Important Notes

1. **Battery Optimization**: Disable battery optimization for the app to ensure continuous operation
2. **Storage Space**: Monitor available storage as recordings can be large
3. **Permissions**: All permissions must be granted for proper functionality
4. **Android Version**: Tested on Android 10+ (API 30+)
5. **Background Limits**: Some OEMs may have additional background restrictions

## 🔍 Troubleshooting

### App Won't Record
1. Check all permissions are granted
2. Verify camera isn't being used by another app
3. Check available storage space
4. Review logcat for error messages

### Service Stops Unexpectedly
1. Disable battery optimization for the app
2. Check if app is whitelisted from background restrictions
3. Verify wake lock is working (check logs)

### Files Not Found
1. Check app has storage permissions
2. Verify external storage is mounted
3. Look for files in the correct directory path

## 📄 License

This project is provided as-is for educational and development purposes. Please ensure you comply with all local laws and regulations regarding recording and privacy.

## 🤝 Contributing

Feel free to submit issues, fork the repository, and create pull requests for any improvements.

---

**Note**: This application is designed for legitimate use cases. Always respect privacy laws and obtain proper consent when recording audio or video.
