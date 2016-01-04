package com.yalin.io2015.application;

import android.app.Application;
import android.content.Context;

import com.yalin.u2fclient.U2FClientApi;
import com.yalin.io2015.log.StatLog;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by YaLin on 2015/12/29.
 */
public class IoApplication extends Application{
    private static Context mContext;
    private Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread thread, final Throwable ex) {
                StringWriter wr = new StringWriter();
                PrintWriter err = new PrintWriter(wr);
                ex.printStackTrace(err);

                StatLog.printLog("app exception", wr.toString() + defaultUncaughtExceptionHandler.getClass().getName());

                defaultUncaughtExceptionHandler.uncaughtException(thread, ex);
            }
        });

        U2FClientApi.init(mContext);
    }

    public static Context getContext() {
        return mContext;
    }
}
