package com.example.headlesscamera;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

public class ConfigParser {

    private static final String TAG = "ConfigParser";
    private static final String CONFIG_FILENAME = "config.json";

    public static class VideoConfig {
        public String resolution = "720p";           // Default fallback values
        public int frameRate = 30;
        public String encoding = "H.264";
        public boolean audioEnabled = true;
        public int durationSeconds = 10;
        public boolean loopEnabled = false;
        public int intervalMinutes = 5;

        // Video dimensions based on resolution
        public int width = 1280;
        public int height = 720;
        public int bitrate = 2000000;

        public void updateDimensionsFromResolution() {
            switch (resolution.toLowerCase()) {
                case "4k":
                case "2160p":
                    width = 3840;
                    height = 2160;
                    bitrate = 15000000;
                    break;
                case "1440p":
                    width = 2560;
                    height = 1440;
                    bitrate = 8000000;
                    break;
                case "1080p":
                    width = 1920;
                    height = 1080;
                    bitrate = 5000000;
                    break;
                case "720p":
                    width = 1280;
                    height = 720;
                    bitrate = 2000000;
                    break;
                case "480p":
                    width = 854;
                    height = 480;
                    bitrate = 1000000;
                    break;
                default:
                    Log.w(TAG, "⚠️ Unknown resolution: " + resolution + ", using 720p");
                    width = 1280;
                    height = 720;
                    bitrate = 2000000;
            }
            Log.d(TAG, "📐 Resolution " + resolution + " -> " + width + "x" + height + " @ " + bitrate + " bps");
        }
    }

    public static VideoConfig loadConfig(Context context) {
        Log.d(TAG, "📋 === Loading video configuration ===");

        VideoConfig config = new VideoConfig();
        JSONObject json = null;

        // Try multiple sources in order of priority:

        // 1. External storage (user can modify)
        json = loadFromExternalStorage();
        if (json != null) {
            Log.d(TAG, "✅ Config loaded from external storage");
        }

        // 2. App's external files directory
        if (json == null) {
            json = loadFromAppExternalFiles(context);
            if (json != null) {
                Log.d(TAG, "✅ Config loaded from app external files");
            }
        }

        // 3. App's internal files directory
        if (json == null) {
            json = loadFromInternalFiles(context);
            if (json != null) {
                Log.d(TAG, "✅ Config loaded from internal files");
            }
        }

        // 4. Assets (bundled with app)
        if (json == null) {
            json = loadFromAssets(context);
            if (json != null) {
                Log.d(TAG, "✅ Config loaded from app assets");
            }
        }

        if (json == null) {
            Log.w(TAG, "⚠️ No config file found, using defaults");
            config.updateDimensionsFromResolution();
            return config;
        }

        // Parse JSON and populate config
        try {
            if (json.has("resolution")) {
                config.resolution = json.getString("resolution");
            }
            if (json.has("frame_rate")) {
                config.frameRate = json.getInt("frame_rate");
            }
            if (json.has("encoding")) {
                config.encoding = json.getString("encoding");
            }
            if (json.has("audio_enabled")) {
                config.audioEnabled = json.getBoolean("audio_enabled");
            }
            if (json.has("duration_seconds")) {
                config.durationSeconds = json.getInt("duration_seconds");
            }
            if (json.has("loop_enabled")) {
                config.loopEnabled = json.getBoolean("loop_enabled");
            }
            if (json.has("interval_minutes")) {
                config.intervalMinutes = json.getInt("interval_minutes");
            }

            // Update dimensions based on resolution
            config.updateDimensionsFromResolution();

            // Log final config
            Log.d(TAG, "📐 Final config:");
            Log.d(TAG, "  Resolution: " + config.resolution + " (" + config.width + "x" + config.height + ")");
            Log.d(TAG, "  Frame rate: " + config.frameRate + " fps");
            Log.d(TAG, "  Encoding: " + config.encoding);
            Log.d(TAG, "  Audio: " + config.audioEnabled);
            Log.d(TAG, "  Duration: " + config.durationSeconds + " seconds");
            Log.d(TAG, "  Loop: " + config.loopEnabled);
            Log.d(TAG, "  Interval: " + config.intervalMinutes + " minutes");
            Log.d(TAG, "  Bitrate: " + config.bitrate + " bps");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing config: " + e.getMessage());
            config.updateDimensionsFromResolution();
        }

        return config;
    }

    // 1. External storage (/storage/emulated/0/config.json)
    private static JSONObject loadFromExternalStorage() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), CONFIG_FILENAME);
            Log.d(TAG, "🔍 Checking external storage: " + file.getAbsolutePath());
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                String json = new Scanner(fis).useDelimiter("\\A").next();
                fis.close();
                return new JSONObject(json);
            }
        } catch (Exception e) {
            Log.d(TAG, "External storage config not found: " + e.getMessage());
        }
        return null;
    }

    // 2. App external files (/storage/emulated/0/Android/data/com.example.headlesscamera/files/config.json)
    private static JSONObject loadFromAppExternalFiles(Context context) {
        try {
            File file = new File(context.getExternalFilesDir(null), CONFIG_FILENAME);
            Log.d(TAG, "🔍 Checking app external files: " + file.getAbsolutePath());
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                String json = new Scanner(fis).useDelimiter("\\A").next();
                fis.close();
                return new JSONObject(json);
            }
        } catch (Exception e) {
            Log.d(TAG, "App external files config not found: " + e.getMessage());
        }
        return null;
    }

    // 3. Internal files (/data/data/com.example.headlesscamera/files/config.json)
    private static JSONObject loadFromInternalFiles(Context context) {
        try {
            File file = new File(context.getFilesDir(), CONFIG_FILENAME);
            Log.d(TAG, "🔍 Checking internal files: " + file.getAbsolutePath());
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                String json = new Scanner(fis).useDelimiter("\\A").next();
                fis.close();
                return new JSONObject(json);
            }
        } catch (Exception e) {
            Log.d(TAG, "Internal files config not found: " + e.getMessage());
        }
        return null;
    }

    // 4. Assets (bundled with app)
    private static JSONObject loadFromAssets(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            Log.d(TAG, "🔍 Checking app assets: " + CONFIG_FILENAME);
            InputStream is = assetManager.open(CONFIG_FILENAME);
            String json = new Scanner(is).useDelimiter("\\A").next();
            is.close();
            return new JSONObject(json);
        } catch (Exception e) {
            Log.d(TAG, "Assets config not found: " + e.getMessage());
        }
        return null;
    }
}