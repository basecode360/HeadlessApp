# HeadlessCamera - Android Background Camera Recording Service

````markdown
# HeadlessCamera – Android Background Camera Recording Service

A powerful Android application that provides truly headless background camera recording without any visible UI. Perfect for surveillance, monitoring, or automated recording scenarios where the app needs to continue recording even when minimized, the screen is off, or the device is locked.

---

## 🎯 Features

- **Truly Headless Operation** – Records video even if the app is not in the foreground  
- **Background & Foreground Service** – Survives system memory cleanup and OS background limits  
- **Boot Persistence** – Automatically restarts on device boot or app update  
- **Broadcast Control** – Start/stop/loop recording via ADB or custom intents  
- **Wake Lock** – Prevents device from sleeping during recording  
- **Multiple Control Methods** – Activity start, broadcasts, or service commands  

---

## 📋 Requirements

- **Android 10+ (API 29+)**, tested up through Android 14 (API 34)  
- **Camera & microphone** hardware  
- **Storage access** for saving recordings (app-specific directory)  
- **ADB** or shell access for advanced control  

---

## 📱 Installation & Setup

### 1. Build the APK  
```bash
./gradlew assembleDebug
````

### 2. Install on Device

```bash
# Standard install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Alternative push & install
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/
adb shell pm install -r /data/local/tmp/app-debug.apk
```

---

## 🔐 Grant Required Permissions

> **Note:** On Android 11+ (API 30+), scoped storage is enforced; you can still pull files via ADB, but file explorers may not show the app’s private directory without root.

```bash
# Core runtime permissions
adb shell pm grant com.example.headlesscamera android.permission.CAMERA
adb shell pm grant com.example.headlesscamera android.permission.RECORD_AUDIO
adb shell pm grant com.example.headlesscamera android.permission.FOREGROUND_SERVICE
adb shell pm grant com.example.headlesscamera android.permission.FOREGROUND_SERVICE_CAMERA

# Boot & wake-lock
adb shell pm grant com.example.headlesscamera android.permission.RECEIVE_BOOT_COMPLETED
adb shell pm grant com.example.headlesscamera android.permission.WAKE_LOCK

# Background & system overlay (may require manual user action)
# SYSTEM_ALERT_WINDOW and REQUEST_IGNORE_BATTERY_OPTIMIZATIONS cannot always be granted via pm grant;
# the user must enable them in Settings → Apps → Special access.
adb shell pm grant com.example.headlesscamera android.permission.SYSTEM_ALERT_WINDOW
adb shell pm grant com.example.headlesscamera android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

# Android 14+ background start
adb shell pm grant com.example.headlesscamera android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND
```

---

## 🔄 Simulate Boot Receiver

To test the boot-persistence without rebooting:

```bash
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
```

---

## 📱 Usage

### Method 1: Direct Activity Start (Recommended)

**Start Recording**

```bash
adb shell am start \
  -n com.example.headlesscamera/.MainActivity \
  --es auto_command start \
  --es auto_action com.example.headlesscamera.START_RECORDING
```

**Stop Recording**

```bash
adb shell am start \
  -n com.example.headlesscamera/.MainActivity \
  --es auto_command stop \
  --es auto_action com.example.headlesscamera.STOP_RECORDING
```

### Method 2: Broadcast Intents

**Start**

```bash
adb shell am broadcast \
  -n com.example.headlesscamera/.BroadcastHandler \
  -a com.example.headlesscamera.START_RECORDING
```

**Stop**

```bash
adb shell am broadcast \
  -n com.example.headlesscamera/.BroadcastHandler \
  -a com.example.headlesscamera.STOP_RECORDING
```

**Enable Looping**

```bash
adb shell am broadcast \
  -n com.example.headlesscamera/.BroadcastHandler \
  -a com.example.headlesscamera.ENABLE_LOOPING
```

### Method 3: Shell Commands on Device

```bash
# Start
am start \
  -n com.example.headlesscamera/.MainActivity \
  --es auto_command start \
  --es auto_action com.example.headlesscamera.START_RECORDING

# ...wait as needed...

# Stop
am start \
  -n com.example.headlesscamera/.MainActivity \
  --es auto_command stop \
  --es auto_action com.example.headlesscamera.STOP_RECORDING
```

---

## 📁 Retrieving Recordings

### Locate Files

```bash
adb shell find /storage/emulated/0/Android/data/com.example.headlesscamera/ -name "*.mp4" -o -name "latest_output.txt"
```

### Pull to PC

```bash
mkdir recordings
adb pull /storage/emulated/0/Android/data/com.example.headlesscamera/files/Movies/ ./recordings/
adb pull /storage/emulated/0/Android/data/com.example.headlesscamera/files/latest_output.txt
```

> ⚠️ On Android 11+ (API 30+), the app’s private directory is hidden from user-facing file managers—use ADB.

---

## 🗂 Permissions Reference

| Permission                                                               | Purpose                                           |
| ------------------------------------------------------------------------ | ------------------------------------------------- |
| `android.permission.CAMERA`                                              | Access camera hardware                            |
| `android.permission.RECORD_AUDIO`                                        | Access microphone                                 |
| `android.permission.WRITE_EXTERNAL_STORAGE` (≤API 34)                    | Save recordings to external storage               |
| `android.permission.READ_EXTERNAL_STORAGE` (≤API 34)                     | Read from external storage                        |
| `android.permission.FOREGROUND_SERVICE`                                  | Run service in foreground                         |
| `android.permission.FOREGROUND_SERVICE_CAMERA` (API 34+)                 | Camera usage in foreground service on Android 14+ |
| `android.permission.RECEIVE_BOOT_COMPLETED`                              | Auto-start after device boot                      |
| `android.permission.WAKE_LOCK`                                           | Prevent sleep during recording                    |
| `android.permission.SYSTEM_ALERT_WINDOW`                                 | Overlay for notifications/UI if needed            |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`                | Bypass Doze battery optimizations                 |
| `android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND` (API 34+) | Start service from background on Android 14+      |

---

## 🔧 Configuration

By default, `CameraService.setupMediaRecorder()` uses:

* **Resolution:** 1920×1080 (1080p)
* **Frame Rate:** 30 FPS
* **Video Codec:** H.264
* **Audio Codec:** AAC
* **Bitrate:** 5 Mbps

To adjust, open `CameraService.java` and modify the `setupMediaRecorder()` parameters.

---

## 🛠️ Maintenance & Debug

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

---

## ⚠️ Troubleshooting

### App Won't Record

1. Verify **all** permissions are granted
2. Ensure **no other** app is using the camera
3. Check **available storage**
4. Inspect `adb logcat -s CameraService` for errors

### Service Stops Unexpectedly

1. Disable battery optimizations for the app
2. Whitelist in “Background restrictions” (if OEM-specific)
3. Confirm **wake lock** acquisition in logs

### Files Not Found

1. Confirm storage permissions
2. Ensure external storage is mounted
3. Use ADB to browse `/storage/emulated/0/Android/data/...`

---

## 🎯 Use Cases

* **Security Monitoring** – Discreet surveillance
* **Automated Recording** – Triggered by external events
* **Remote Control** – Via ADB or networked shell
* **Background Documentation** – Capture video while using other apps
* **Camera Testing** – Automated validation in CI environments

---

## 📄 License

This project is provided as-is for educational and development purposes. Always comply with local privacy and recording laws.

---

## 🤝 Contributing

Pull requests, issues, and forks are welcome! For major changes, please open an issue first to discuss your proposal.

---

> **Disclaimer:** Designed for legitimate use cases only. Always obtain proper consent before recording audio or video.

```
```
