package com.ilis.memoryoptimizer;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class MemOptApplication extends Application {

    private static MemOptApplication mContext;
    private static Handler mMainHandler;
    private static int mMainThreadId;

    private static ActivityManager activityManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mMainHandler = new Handler(Looper.getMainLooper());
        mMainThreadId = android.os.Process.myTid();

        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ProcessInfoProvider.init(this);
    }

    public static MemOptApplication getApplication() {
        return mContext;
    }

    public static Handler getMainHandler() {
        return mMainHandler;
    }

    public static boolean isMainThread() {
        return android.os.Process.myTid() == mMainThreadId;
    }

    public static ActivityManager getActivityManager() {
        return activityManager;
    }

    public static String getCurrentPackageName() {
        return mContext.getPackageName();
    }
}
