package com.example.eric.animaldetect;

import android.util.Log;

public class LogUtil {

    private static final Boolean DEBUG = false;

    public static void d(String tag, String loginfo) {
        if (DEBUG) {
            Log.d(tag, loginfo);
        }
    }
}
