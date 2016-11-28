package com.ilis.memoryoptimizer.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.format.Formatter;

import com.ilis.memoryoptimizer.modle.ProcessInfo;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.util.ArrayList;
import java.util.List;

public class ProcessInfoProvider {

    private static List<ProcessInfo> userProcessInfo = new ArrayList<>();
    private static List<ProcessInfo> systemProcessInfo = new ArrayList<>();
    private static List<ProcessInfo> processInfo = new ArrayList<>();
    private static String availMem;
    private static String totalMem;
    private static int usedMemory;
    private static int totalMemory;
    private static String currentPackageName;

    public static synchronized void updateProcessInfo(Context context) {
        ProcessInfoProvider.userProcessInfo.clear();
        ProcessInfoProvider.systemProcessInfo.clear();
        ProcessInfoProvider.processInfo.clear();

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = context.getPackageManager();

        currentPackageName = context.getPackageName();

        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        availMem = Formatter.formatFileSize(context, memoryInfo.availMem);
        totalMem = Formatter.formatFileSize(context, memoryInfo.totalMem);

        totalMemory = (int) (memoryInfo.totalMem / (1024 * 1024));
        usedMemory = totalMemory - (int) (memoryInfo.availMem / (1024 * 1024));

        List<AndroidAppProcess> runningAppProcesses = AndroidProcesses.getRunningAppProcesses();

        for (AndroidAppProcess process : runningAppProcesses) {
            ProcessInfo info = new ProcessInfo();

            String packName = process.name;
            info.setPackName(packName);

            android.os.Debug.MemoryInfo[] memoryInfos = am.getProcessMemoryInfo(new int[]{process.pid});
            long memSize = memoryInfos[0].getTotalPrivateDirty() * 1024L;
            info.setMemSize(memSize);

            try {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(packName, 0);
                Drawable icon = applicationInfo.loadIcon(pm);
                info.setIcon(icon);

                String name = applicationInfo.loadLabel(pm).toString();
                info.setAppName(name);

                if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    info.setUserProcess(true);
                } else {
                    info.setUserProcess(false);
                }

            } catch (PackageManager.NameNotFoundException e) {
                info.setAppName(packName);
                Drawable icon = context.getResources().getDrawable(android.R.mipmap.sym_def_app_icon);
                info.setIcon(icon);
            }

            if (info.isUserProcess()) {
                ProcessInfoProvider.userProcessInfo.add(info);
            } else {
                ProcessInfoProvider.systemProcessInfo.add(info);
            }
        }

        ProcessInfoProvider.processInfo.addAll(userProcessInfo);
        ProcessInfoProvider.processInfo.addAll(systemProcessInfo);
    }


    public static List<ProcessInfo> getProcessInfo() {
        return processInfo;
    }

    public static List<ProcessInfo> getUserProcessInfo() {
        return userProcessInfo;
    }

    public static List<ProcessInfo> getSystemProcessInfo() {
        return systemProcessInfo;
    }

    public static int getProcessCount() {
        return processInfo.size();
    }

    public static int getUserProcessCount() {
        return userProcessInfo.size();
    }

    public static int getSystemProcessCount() {
        return systemProcessInfo.size();
    }

    public static String getSystemMemStatus() {
        return availMem + "/" + totalMem;
    }

    public static int getTotalMemory() {
        return totalMemory;
    }

    public static int getUsedMemory() {
        return usedMemory;
    }

    public static String getCurrentPackageName() {
        return currentPackageName;
    }
}