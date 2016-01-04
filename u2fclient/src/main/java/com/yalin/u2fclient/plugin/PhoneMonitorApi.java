package com.yalin.u2fclient.plugin;

import java.lang.reflect.Method;

import android.content.Context;

public class PhoneMonitorApi {
    private static final String STAT_MONITOR_PLUGIN_API_CLASS_NAME = "com.idsmanager.phonemonitorplugin.MonitorApi";
    private static Class<?> sMonitorApiClass;

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (Exception e) {
            return null;
        }
    }

    private static synchronized Class<?> getClass(Context context) {
        if (sMonitorApiClass == null) {
            sMonitorApiClass = loadClass(STAT_MONITOR_PLUGIN_API_CLASS_NAME);
        }
        return sMonitorApiClass;
    }

    public static void init(Context context) {
        Class<?> cls = getClass(context);
        if (cls == null) {
            return;
        }
        try {
            Method method = cls.getDeclaredMethod("start", Context.class);
            if (method != null) {
                Object[] args = new Object[1];
                args[0] = context;
                method.invoke(null, args);
            }
        } catch (Exception e) {
        }
    }

    public static void setUID(Context context, byte[] uid) {
        Class<?> cls = getClass(context);
        if (cls == null) {
            return;
        }
        try {
            Method method = cls.getDeclaredMethod("setUID", byte[].class);
            if (method != null) {
                Object[] args = new Object[1];
                args[0] = uid;
                method.invoke(null, args);
            }
        } catch (Exception e) {
        }
    }

    public static void synTime(Context context, long timeSecond, Object callback) {
        Class<?> cls = getClass(context);
        if (cls == null) {
            return;
        }
        try {
            Method method = cls.getDeclaredMethod("synTime", long.class, Object.class);
            if (method != null) {
                Object[] args = new Object[2];
                args[0] = timeSecond;
                args[1] = callback;
                method.invoke(null, args);
            }
        } catch (Exception e) {
        }
    }
}
