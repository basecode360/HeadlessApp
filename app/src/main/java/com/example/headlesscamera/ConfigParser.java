package com.example.headlesscamera;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

public class ConfigParser {
    private static final String TAG = "ConfigParser";
    private static final String CONFIG_FILENAME = "camera_config.json";

    public static VideoConfig loadConfig(Context context) {
        Log.d(TAG, "📋 === Loading video configuration ===");
        Log.d(TAG, "🔍 Context: " + context.getClass().getSimpleName());

        VideoConfig config = new VideoConfig();
        JSONObject json = null;
        String configSource = "defaults";

        // Try multiple sources in order of priority:

        // 1. External storage (user can modify)
        Log.d(TAG, "🔍 === Trying external storage ===");
        json = loadFromExternalStorage();
        if (json != null) {
            Log.d(TAG, "✅ Config loaded from external storage");
            configSource = "external storage";
        } else {
            Log.d(TAG, "❌ External storage config failed");
        }

        // 2. App's external files directory
        if (json == null) {
            Log.d(TAG, "🔍 === Trying app external files ===");
            json = loadFromAppExternalFiles(context);
            if (json != null) {
                Log.d(TAG, "✅ Config loaded from app external files");
                configSource = "app external files";
            } else {
                Log.d(TAG, "❌ App external files config failed");
            }
        }

        // 3. App's internal files directory
        if (json == null) {
            Log.d(TAG, "🔍 === Trying internal files ===");
            json = loadFromInternalFiles(context);
            if (json != null) {
                Log.d(TAG, "✅ Config loaded from internal files");
                configSource = "internal files";
            } else {
                Log.d(TAG, "❌ Internal files config failed");
            }
        }

        // 4. Assets (bundled with app)
        if (json == null) {
            Log.d(TAG, "🔍 === Trying app assets ===");
            json = loadFromAssets(context);
            if (json != null) {
                Log.d(TAG, "✅ Config loaded from app assets");
                configSource = "app assets";
            } else {
                Log.d(TAG, "❌ App assets config failed");
            }
        }

        if (json == null) {
            Log.w(TAG, "⚠️ No config file found anywhere, using hardcoded defaults");
            config.updateDimensionsFromResolution();
            logFinalConfig(config, "hardcoded defaults");
            return config;
        }

        // Parse JSON and populate config
        try {
            Log.d(TAG, "🔧 === Parsing JSON config ===");
            Log.d(TAG, "📄 Raw JSON: " + json.toString());

            if (json.has("resolution")) {
                String oldResolution = config.resolution;
                config.resolution = json.getString("resolution");
                Log.d(TAG, "📐 Resolution: " + oldResolution + " -> " + config.resolution);
            }
            if (json.has("frame_rate")) {
                int oldFrameRate = config.frameRate;
                config.frameRate = json.getInt("frame_rate");
                Log.d(TAG, "🎞️ Frame rate: " + oldFrameRate + " -> " + config.frameRate);
            }
            if (json.has("encoding")) {
                String oldEncoding = config.encoding;
                config.encoding = json.getString("encoding");
                Log.d(TAG, "🎬 Encoding: " + oldEncoding + " -> " + config.encoding);
            }
            if (json.has("audio_enabled")) {
                boolean oldAudio = config.audioEnabled;
                config.audioEnabled = json.getBoolean("audio_enabled");
                Log.d(TAG, "🎤 Audio: " + oldAudio + " -> " + config.audioEnabled);
            }
            if (json.has("duration_seconds")) {
                int oldDuration = config.durationSeconds;
                config.durationSeconds = json.getInt("duration_seconds");
                Log.d(TAG, "⏱️ Duration: " + oldDuration + " -> " + config.durationSeconds + " seconds");
            }
            if (json.has("loop_enabled")) {
                boolean oldLoop = config.loopEnabled;
                config.loopEnabled = json.getBoolean("loop_enabled");
                Log.d(TAG, "🔄 Loop: " + oldLoop + " -> " + config.loopEnabled);
            }
            if (json.has("interval_minutes")) {
                int oldInterval = config.intervalMinutes;
                config.intervalMinutes = json.getInt("interval_minutes");
                Log.d(TAG, "⏰ Interval: " + oldInterval + " -> " + config.intervalMinutes + " minutes");
            }

            // Update dimensions based on resolution
            config.updateDimensionsFromResolution();

            logFinalConfig(config, configSource);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing config from " + configSource + ": " + e.getMessage(), e);
            config.updateDimensionsFromResolution();
            logFinalConfig(config, "defaults due to parse error");
        }

        return config;
    }

    private static void logFinalConfig(VideoConfig config, String source) {
        Log.d(TAG, "🎯 === FINAL CONFIG (from " + source + ") ===");
        Log.d(TAG, "  📐 Resolution: " + config.resolution + " (" + config.width + "x" + config.height + ")");
        Log.d(TAG, "  🎞️ Frame rate: " + config.frameRate + " fps");
        Log.d(TAG, "  🎬 Encoding: " + config.encoding);
        Log.d(TAG, "  🎤 Audio: " + config.audioEnabled);
        Log.d(TAG, "  ⏱️ Duration: " + config.durationSeconds + " seconds");
        Log.d(TAG, "  🔄 Loop: " + config.loopEnabled);
        Log.d(TAG, "  ⏰ Interval: " + config.intervalMinutes + " minutes");
        Log.d(TAG, "  💾 Bitrate: " + config.bitrate + " bps");
        Log.d(TAG, "🎯 === END CONFIG ===");
    }

    // Enhanced external storage loader with better error handling
    private static JSONObject loadFromExternalStorage() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), CONFIG_FILENAME);
            Log.d(TAG, "🔍 Checking external storage: " + file.getAbsolutePath());
            Log.d(TAG, "📁 File exists: " + file.exists());
            Log.d(TAG, "📁 File readable: " + file.canRead());
            Log.d(TAG, "📁 File size: " + (file.exists() ? file.length() + " bytes" : "N/A"));

            if (file.exists() && file.canRead()) {
                FileInputStream fis = new FileInputStream(file);
                String json = new Scanner(fis).useDelimiter("\\A").next();
                fis.close();
                Log.d(TAG, "📄 File content: " + json);
                return new JSONObject(json);
            } else {
                Log.d(TAG, "❌ External storage file not accessible");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ External storage config error: " + e.getMessage(), e);
        }
        return null;
    }

    private static JSONObject loadFromAppExternalFiles(Context context) {
        try {
            File file = new File(context.getExternalFilesDir(null), CONFIG_FILENAME);
            Log.d(TAG, "🔍 Checking app external files: " + file.getAbsolutePath());

            if (file.exists() && file.canRead()) {
                FileInputStream fis = new FileInputStream(file);
                String json = new Scanner(fis).useDelimiter("\\A").next();
                fis.close();
                return new JSONObject(json);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ App external files config error: " + e.getMessage(), e);
        }
        return null;
    }

    private static JSONObject loadFromInternalFiles(Context context) {
        try {
            File file = new File(context.getFilesDir(), CONFIG_FILENAME);
            Log.d(TAG, "🔍 Checking internal files: " + file.getAbsolutePath());

            if (file.exists() && file.canRead()) {
                FileInputStream fis = new FileInputStream(file);
                String json = new Scanner(fis).useDelimiter("\\A").next();
                fis.close();
                return new JSONObject(json);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Internal files config error: " + e.getMessage(), e);
        }
        return null;
    }

    private static JSONObject loadFromAssets(Context context) {
        try {
            Log.d(TAG, "🔍 Checking app assets for: " + CONFIG_FILENAME);
            InputStream is = context.getAssets().open(CONFIG_FILENAME);
            String json = new Scanner(is).useDelimiter("\\A").next();
            is.close();
            return new JSONObject(json);
        } catch (Exception e) {
            Log.e(TAG, "❌ App assets config error: " + e.getMessage(), e);
        }
        return null;
    }
    public static class VideoConfig {
        private static final String TAG = "VideoConfig";

        // Default values
        public String resolution = "1080p";
        public int frameRate = 30;
        public String encoding = "H.264";
        public boolean audioEnabled = true;
        public int durationSeconds = 0; // 0 = record until manually stopped
        public boolean loopEnabled = false;
        public int intervalMinutes = 5;

        // Derived values (calculated from resolution)
        public int width = 1920;
        public int height = 1080;
        public int bitrate = 5000000; // 5 Mbps default

        public VideoConfig() {
            updateDimensionsFromResolution();
        }

        public void updateDimensionsFromResolution() {
            Log.d(TAG, "🔧 Updating dimensions for resolution: " + resolution);

            switch (resolution.toLowerCase()) {
                case "4k":
                case "2160p":
                    width = 3840;
                    height = 2160;
                    bitrate = 20000000; // 20 Mbps for 4K
                    break;
                case "1440p":
                case "2k":
                    width = 2560;
                    height = 1440;
                    bitrate = 10000000; // 10 Mbps for 1440p
                    break;
                case "1080p":
                case "fhd":
                    width = 1920;
                    height = 1080;
                    bitrate = 5000000; // 5 Mbps for 1080p
                    break;
                case "720p":
                case "hd":
                    width = 1280;
                    height = 720;
                    bitrate = 3000000; // 3 Mbps for 720p
                    break;
                case "480p":
                    width = 854;
                    height = 480;
                    bitrate = 1500000; // 1.5 Mbps for 480p
                    break;
                case "360p":
                    width = 640;
                    height = 360;
                    bitrate = 1000000; // 1 Mbps for 360p
                    break;
                default:
                    Log.w(TAG, "⚠️ Unknown resolution '" + resolution + "', using 1080p defaults");
                    width = 1920;
                    height = 1080;
                    bitrate = 5000000;
                    resolution = "1080p";
                    break;
            }

            Log.d(TAG, "📐 Resolution set to " + resolution + " (" + width + "x" + height + ") @ " + bitrate + " bps");
        }

        @Override
        public String toString() {
            return "VideoConfig{" +
                    "resolution='" + resolution + '\'' +
                    ", frameRate=" + frameRate +
                    ", encoding='" + encoding + '\'' +
                    ", audioEnabled=" + audioEnabled +
                    ", durationSeconds=" + durationSeconds +
                    ", loopEnabled=" + loopEnabled +
                    ", intervalMinutes=" + intervalMinutes +
                    ", width=" + width +
                    ", height=" + height +
                    ", bitrate=" + bitrate +
                    '}';
        }
    }
}