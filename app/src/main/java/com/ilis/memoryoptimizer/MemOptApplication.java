package com.ilis.memoryoptimizer;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.ilis.memoryoptimizer.util.ProcessInfoProvider;

public class MemOptApplication extends Application {

    private static MemOptApplication mContext;
    private static Handler mMainHandler;
    private static Thread mMainThread;
    private static int mMainThreadId;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mMainHandler = new Handler(Looper.getMainLooper());
        mMainThread = Thread.currentThread();
        mMainThreadId = android.os.Process.myTid();

        ProcessInfoProvider.init(this);
    }

    public static MemOptApplication getApplication() {
        return mContext;
    }

    public static Handler getMainThreadHandler() {
        return mMainHandler;
    }

    public static Thread getMainThread() {
        return mMainThread;
    }

    public static int getMainThreadId() {
        return mMainThreadId;
    }

    public static boolean isMainThread() {
        return android.os.Process.myTid() == MemOptApplication.getMainThreadId();
    }

}
