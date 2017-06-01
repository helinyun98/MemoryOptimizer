package com.ilis.memoryoptimizer.data;

import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

public class ProcessInfoDataMapper {

    public static ProcessInfo transform(@NonNull AndroidAppProcess process) {
        ProcessInfo info = new ProcessInfo();
        info.setProcessName(process.name);
        info.setPackageName(process.getPackageName());
        info.setMemSize(getProcessMemoryInfo(process.pid));
        info.setAppName(getAppName(process.name));
        info.setUserProcess(isUserProcess(process.getPackageName()));
        return info;
    }

    private static long getProcessMemoryInfo(int pid) {
        ActivityManager activityManager = MemOptApplication.getActivityManager();
        Debug.MemoryInfo[] memoryInfo = activityManager.getProcessMemoryInfo(new int[]{pid});
        int privateMemory = memoryInfo[0].getTotalPrivateDirty();
        return privateMemory * 1024L;
    }

    private static String getAppName(String processName) {
        PackageManager packageManager = MemOptApplication.getApplication().getPackageManager();
        try {
            String packageName = processName.split(":")[0];
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            String appLabel = applicationInfo.loadLabel(packageManager).toString();
            if (TextUtils.equals(packageName, processName)) {
                return appLabel;
            } else {
                return processName.replaceFirst(packageName, appLabel);
            }
        } catch (PackageManager.NameNotFoundException e) {
            return processName;
        }
    }

    private static boolean isUserProcess(String packageName) {
        PackageManager packageManager = MemOptApplication.getApplication().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
