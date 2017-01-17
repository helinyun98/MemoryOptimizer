package com.ilis.memoryoptimizer.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.format.Formatter;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.ilis.memoryoptimizer.modle.ProcessInfo;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ProcessInfoProvider {

    private static List<ProcessInfo> userProcessInfo = new ArrayList<>();
    private static List<ProcessInfo> systemProcessInfo = new ArrayList<>();
    private static List<ProcessInfo> processInfo = new ArrayList<>();
    private static String availMem;
    private static String totalMem;
    private static int usedMemory;
    private static int totalMemory;
    private static ActivityManager am;
    private static PackageManager pm;
    private static String currentPackageName;
    private static Drawable defIcon;

    public static void init(Context context) {
        am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        pm = context.getPackageManager();
        currentPackageName = context.getPackageName();
        defIcon = ResourcesCompat.getDrawable(context.getResources(), android.R.mipmap.sym_def_app_icon, null);
    }

    private static synchronized void updateProcessInfo() {
        userProcessInfo.clear();
        systemProcessInfo.clear();
        processInfo.clear();

        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        availMem = Formatter.formatFileSize(MemOptApplication.getApplication(), memoryInfo.availMem);
        totalMem = Formatter.formatFileSize(MemOptApplication.getApplication(), memoryInfo.totalMem);

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
                info.setIcon(defIcon);
            }

            if (info.isUserProcess()) {
                userProcessInfo.add(info);
            } else {
                systemProcessInfo.add(info);
            }
        }

        processInfo.addAll(userProcessInfo);
        processInfo.addAll(systemProcessInfo);
    }

    private static volatile boolean isUpdating = false;
    private static PublishSubject<List<ProcessInfo>> processInfoPublisher = PublishSubject.create();

    public synchronized static void update() {
        if (isUpdating) {
            return;
        }
        isUpdating = true;
        Observable
                .create(new ObservableOnSubscribe<Long>() {
                    @Override
                    public void subscribe(ObservableEmitter<Long> e) throws Exception {
                        updateProcessInfo();
                        e.onComplete();
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultObserver<Long>() {
                    @Override
                    public void onNext(Long ignore) {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                        processInfoPublisher.onNext(processInfo);
                        isUpdating = false;
                    }
                });
    }

    public static PublishSubject<List<ProcessInfo>> getProvider() {
        return processInfoPublisher;
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