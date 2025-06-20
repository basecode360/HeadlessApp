````markdown
# HeadlessCamera – Android Background Camera Recording Service

The **HeadlessCamera** app provides powerful and customizable background camera recording for Android devices. The app works seamlessly even when the device is locked or the app is minimized, capturing video without requiring any visible UI. It is perfect for use cases such as surveillance, monitoring, or automated recording, where the device should continuously record footage without interaction.

---

## 🎯 Key Features

- **Full-Resolution Support**: Supports resolutions from **360p to 4K**, enabling flexible recording quality depending on your needs.
- **30 FPS Frame Rate**: Provides smooth video recording with support for **30 FPS** frame rate.
- **H.264 Encoding**: Utilizes **H.264 encoding** for both video and audio, ensuring efficient compression.
- **Audio Recording**: Supports **audio recording** during video capture for complete recordings.
- **Recording Duration Control**: Customize the duration of each recording session with adjustable settings.
- **Background Operation**: The app continues to record in the background even when minimized, or the screen is off.
- **Foreground Service**: Ensures that the app's recording continues even if the system performs a memory cleanup.
- **Boot Persistence**: Automatically restarts the recording service after the device boots, ensuring the app resumes without manual intervention.
- **Broadcast Control**: Start, stop, and enable looping through ADB commands or custom intents.
- **Wake Lock**: Prevents the device from going to sleep during recording, ensuring uninterrupted recording.
- **Multiple Control Methods**: Allows you to control the app using Activity starts, Broadcast Intents, or Service commands.

---

## 📋 Requirements

- **Android 10+ (API 29+)**, tested up through Android 14 (API 34)
- **Camera & Microphone** hardware
- **Storage Access** to save recordings
- **ADB or Shell Access** for advanced control
- **Permissions**: Ensure all necessary permissions (Camera, Audio, Foreground Services, etc.) are granted.

---

## 📱 Installation & Setup

### 1. Build the APK  
To build the app in debug mode, run:
```bash
./gradlew assembleDebug
````

### 2. Install on Device

To install the APK via ADB:

```bash
# Standard installation
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Alternatively, push and install via shell
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/
adb shell pm install -r /data/local/tmp/app-debug.apk
```

### 3. Grant Permissions

Grant the necessary permissions for the app to function:

```bash
# Grant core runtime permissions
adb shell pm grant com.example.headlesscamera android.permission.CAMERA
adb shell pm grant com.example.headlesscamera android.permission.RECORD_AUDIO
adb shell pm grant com.example.headlesscamera android.permission.FOREGROUND_SERVICE
adb shell pm grant com.example.headlesscamera android.permission.FOREGROUND_SERVICE_CAMERA

# Grant boot and wake-lock permissions
adb shell pm grant com.example.headlesscamera android.permission.RECEIVE_BOOT_COMPLETED
adb shell pm grant com.example.headlesscamera android.permission.WAKE_LOCK

# Allow background services on Android 14+ (API 34)
adb shell pm grant com.example.headlesscamera android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND

# System overlay (for permissions on Android 11+)
adb shell pm grant com.example.headlesscamera android.permission.SYSTEM_ALERT_WINDOW
adb shell pm grant com.example.headlesscamera android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
```

---

## 📝 Configuration

### Configuration File Location

The app reads its configuration settings from a **JSON file** (`camera_config.json`). This file should be saved in the following directory:

```
/storage/emulated/0/Android/data/com.example.headlesscamera/files/camera_config.json
```

For **first-time installation** or when installing on a new device, ensure the configuration file has the correct file permissions:

```bash
chmod 644 /storage/emulated/0/Android/data/com.example.headlesscamera/files/camera_config.json
```

### Example Configuration File

Below is an example of a configuration file that you can create to specify recording settings like resolution, frame rate, encoding, and duration.

```bash
# Create a configuration file
echo '{
  "resolution": "1080p",
  "frame_rate": 30,
  "encoding": "H.264", 
  "audio_enabled": true,
  "duration_seconds": 25,
  "loop_enabled": false,
  "interval_minutes": 5
}' > test_robust.json
```

You can push this configuration to your device as follows:

```bash
cp test_robust.json /storage/emulated/0/Android/data/com.example.headlesscamera/files/camera_config.json
```

### Creating a More Robust Test Configuration

If you need to create a more detailed configuration with additional settings like a longer recording duration, you can use the following example:

```bash
echo '{
  "resolution": "720p",
  "frame_rate": 30,
  "encoding": "H.264", 
  "audio_enabled": true,
  "duration_seconds": 30,
  "loop_enabled": false,
  "interval_minutes": 5
}' > test_robust.json

# Push to device
adb push test_robust.json /storage/emulated/0/Android/data/com.example.headlesscamera/files/camera_config.json
```

---

### Force Stop the App After Configuration Changes

If the app doesn't reflect the new configuration after pushing the file, force stop the app to ensure it loads the new settings:

```bash
am force-stop com.example.headlesscamera
sleep 2
```

Afterward, restart the app to reload the configuration:

```bash
am start -n com.example.headlesscamera/.MainActivity --es auto_command start
sleep 5
```

To monitor the app’s logs and verify that the new configuration is loaded properly, use the following command:

```bash
logcat -c
logcat -s "ConfigParser:*" "VideoConfig:*" &
logcat -d | grep -E "(Config|Resolution.*->|720p|24.*fps)"
```

---

## 🛠️ Maintenance & Debugging

### Uninstall the App

To uninstall the app from the device:

```bash
adb shell pm uninstall com.example.headlesscamera
```

### Check Service Status

To monitor the service and check if the app is running properly:

```bash
adb shell dumpsys activity services | grep HeadlessCamera
```

### View Logs

To view the logs and debug the service:

```bash
adb logcat -s CameraService
```

### Clear App Data

To clear app data (useful for resetting the app):

```bash
adb shell pm clear com.example.headlesscamera
```

---

## ⚠️ Troubleshooting

### App Won't Record

* Ensure **all permissions** are granted.
* Verify the **camera** is not being used by another app.
* Check if there is sufficient **storage space** for recordings.
* Use `adb logcat -s CameraService` to view any errors.

### Service Stops Unexpectedly

* Disable **battery optimization** for the app.
* Ensure the app is **whitelisted** from background restrictions, especially on certain OEM devices.
* Check if the **wake lock** is being properly acquired by the app.

### Files Not Found

* Verify **storage permissions** are granted.
* Ensure external storage is **mounted** properly.
* Use ADB to check the file directory `/storage/emulated/0/Android/data/...`.

---

## 🎯 Use Cases

* **Security Monitoring**: Capture footage for surveillance without the need for a visible UI.
* **Automated Recording**: Trigger recording based on external events or schedule.
* **Remote Control**: Control recording and settings via ADB from another device or over a network.
* **Background Documentation**: Record while using other apps or when the device is locked.
* **Testing**: Use for automated camera testing in CI environments.

---

## 📄 License

This project is provided as-is for educational and development purposes. Always comply with local privacy and recording laws. Ensure proper consent is obtained when recording audio or video.

---

## 🤝 Contributing

Feel free to submit **issues**, **fork** the repository, and create **pull requests** for any improvements. If you have suggestions for new features or found bugs, don’t hesitate to open an issue.

---

> **Disclaimer**: This app is designed for legitimate use cases only. Always respect privacy laws and obtain necessary permissions before recording audio or video.

```

---

This version of the README now includes **detailed configuration instructions**, **on-device mobile user commands**, and **specific guidance for first-time installations**. The changes reflect a more **professional** and **user-friendly** approach for clients or developers to easily understand the setup, troubleshooting, and usage of the app.
```
