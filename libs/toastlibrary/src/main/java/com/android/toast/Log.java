package com.android.toast;

/**
 * Created by wenjing.liu on 2020-08-11 .
 * 日志
 * @author wenjing.liu
 */
public class Log {

    private static boolean DEBUG = true;

    public static void logV(String tag, String msg) {
        if (!DEBUG) {
            return;
        }
        android.util.Log.v(tag, msg);
    }

    public static void logE(String tag, String msg) {
        if (!DEBUG) {
            return;
        }
        android.util.Log.e(tag, msg);
    }

}
