package moe.studio.frontia;

import android.util.Log;

class Logger {

    static final boolean DEBUG = BuildConfig.DEBUG;

    public static void v(String TAG, String msg) {
        if (!DEBUG) return;
        Log.v(TAG, msg);
    }

    public static void d(String TAG, String msg) {
        if (!DEBUG) return;
        Log.d(TAG, msg);
    }

    public static void i(String TAG, String msg) {
        if (!DEBUG) return;
        Log.i(TAG, msg);
    }

    public static void w(String TAG, String msg) {
        if (!DEBUG) return;
        Log.w(TAG, msg);
    }
}
