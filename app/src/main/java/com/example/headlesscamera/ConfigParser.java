package com.example.headlesscamera;

import android.os.Environment;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

public class ConfigParser {

    public static JSONObject loadConfig() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "config.json");
            FileInputStream fis = new FileInputStream(file);
            String json = new Scanner(fis).useDelimiter("\\A").next();
            return new JSONObject(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
