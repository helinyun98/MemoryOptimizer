package com.ilis.memoryoptimizer.data;

import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.support.annotation.Nullable;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ProcessInfoRepository implements ProcessInfoSource {

    @Nullable
    private static volatile ProcessInfoRepository INSTANCE = null;

    private static PublishSubject<Long> mRefreshEndNotification = PublishSubject.create();
    private static PublishSubject<Long> mRefreshStartNotification = PublishSubject.create();
    private static volatile boolean mRefreshing;

    private List<ProcessInfo> mCachedProcessInfo;
    private static ActivityManager.MemoryInfo mCachedMemoryInfo;

    private ProcessInfoRepository() {
        initRefreshListener();
    }

    public static ProcessInfoSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProcessInfoRepository();
        }
        return INSTANCE;
    }

    @Override
    public ProcessInfoSource refresh() {
        mCachedProcessInfo = null;
        mCachedMemoryInfo = null;
        mRefreshStartNotification.onNext(System.currentTimeMillis());
        return this;
    }

    @Override
    public Observable<Long> refreshEnd() {
        return mRefreshEndNotification
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<ProcessInfo>> getAllProcess(boolean ignoreCache) {
        if (!ignoreCache && mCachedProcessInfo != null) {
            return Observable.just(mCachedProcessInfo);
        }
        refresh();
        return refreshEnd()
                .take(1)
                .map(endTime -> mCachedProcessInfo);
    }

    @Override
    public Observable<Integer> getProcessCount() {
        if (mCachedProcessInfo != null) {
            return Observable.just(mCachedProcessInfo.size());
        }
        refresh();
        return refreshEnd()
                .take(1)
                .map(endTime -> mCachedProcessInfo.size());
    }

    private Observable<Long> getTotalMemoryByte() {
        if (mCachedMemoryInfo != null) {
            return Observable.just(mCachedMemoryInfo.totalMem);
        }
        refresh();
        return refreshEnd()
                .take(1)
                .map(endTime -> mCachedMemoryInfo.totalMem);
    }

    private Observable<Long> getAvailMemoryByte() {
        if (mCachedMemoryInfo != null) {
            return Observable.just(mCachedMemoryInfo.availMem);
        }
        refresh();
        return refreshEnd()
                .take(1)
                .map(memoryInfo -> mCachedMemoryInfo.availMem);
    }

    @Override
    public Observable<String> getSystemMemStatus() {
        return Observable.combineLatest(
                getAvailMemoryByte(), getTotalMemoryByte(),
                (availMemory, totalMemory) -> availMemory + "/" + totalMemory);
    }

    @Override
    public Observable<Integer> getTotalMemory() {
        return getTotalMemoryByte()
                .map(totalMemory -> (int) (totalMemory / (1024 * 1024)));
    }

    @Override
    public Observable<Integer> getUsedMemory() {
        return Observable.combineLatest(
                getAvailMemoryByte(), getTotalMemoryByte(),
                (availMemory, totalMemory) -> totalMemory - availMemory)
                .map(usedMemory -> (int) (usedMemory / (1024 * 1024)));
    }

    private void initRefreshListener() {
        mRefreshStartNotification
                .debounce(1, TimeUnit.SECONDS)
                .filter(ignore -> !mRefreshing)
                .doOnNext(ignore -> mRefreshing = true)
                .subscribeOn(Schedulers.single())
                .subscribe(ignore -> refreshInternal());
    }

    private void refreshInternal() {
        Observable.just(AndroidProcesses.getRunningAppProcesses())
                .flatMap(runningProcessList ->
                        Observable.fromArray(runningProcessList.toArray(new AndroidAppProcess[0])))
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.computation())
                .map(process -> {
                    ProcessInfo info = new ProcessInfo();
                    info.setProcessName(process.name);
                    info.setPackageName(process.getPackageName());
                    info.setMemSize(getProcessMemoryInfo(process.pid));
                    info.setAppName(getAppName(process.name));
                    info.setUserProcess(isUserProcess(process.name));
                    return info;
                })
                .observeOn(Schedulers.io())
                .toSortedList((process1, process2) -> {
                    if (process1.isUserProcess() && process2.isUserProcess()) {
                        return 0;
                    }
                    if (process1.isUserProcess()) {
                        return -1;
                    }
                    return 1;
                })
                .doOnSuccess(ignore -> cacheMemoryInfo())
                .retry()
                .observeOn(Schedulers.single())
                .doOnSuccess(processInfoList -> mCachedProcessInfo = processInfoList)
                .doOnSuccess(ignore -> mRefreshing = false)
                .doOnSuccess(ignore -> mRefreshEndNotification.onNext(System.currentTimeMillis()))
                .subscribe();
    }

    private long getProcessMemoryInfo(int pid) {
        ActivityManager activityManager = MemOptApplication.getActivityManager();
        Debug.MemoryInfo[] memoryInfo = activityManager.getProcessMemoryInfo(new int[]{pid});
        int privateMemory = memoryInfo[0].getTotalPrivateDirty();
        return privateMemory * 1024L;
    }

    private String getAppName(String processName) {
        PackageManager packageManager = MemOptApplication.getApplication().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(processName, 0);
            return applicationInfo.loadLabel(packageManager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return processName;
        }
    }

    private boolean isUserProcess(String processName) {
        PackageManager packageManager = MemOptApplication.getApplication().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(processName, 0);
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void cacheMemoryInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        MemOptApplication.getActivityManager().getMemoryInfo(memoryInfo);
        mCachedMemoryInfo = memoryInfo;
    }
}
