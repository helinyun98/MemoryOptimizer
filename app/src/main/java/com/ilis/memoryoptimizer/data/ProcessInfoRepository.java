package com.ilis.memoryoptimizer.data;

import android.app.ActivityManager;
import android.support.annotation.Nullable;
import android.text.format.Formatter;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.jaredrummler.android.processes.AndroidProcesses;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ProcessInfoRepository implements ProcessInfoSource {

    @Nullable
    private static volatile ProcessInfoRepository INSTANCE = null;

    private volatile boolean mRefreshing;
    private PublishSubject<Long> mRefreshStartNotification = PublishSubject.create();
    private PublishSubject<Long> mRefreshEndNotification = PublishSubject.create();

    private List<ProcessInfo> mCachedProcessInfo;
    private ActivityManager.MemoryInfo mCachedMemoryInfo;

    private ProcessInfoRepository() {
        initRefreshListener();
    }

    public static synchronized ProcessInfoSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProcessInfoRepository();
        }
        return INSTANCE;
    }

    @Override
    public void refresh() {
        mCachedProcessInfo = null;
        mCachedMemoryInfo = null;
    }

    @Override
    public Observable<List<ProcessInfo>> getAllProcess() {
        if (mCachedProcessInfo != null) {
            return Observable.just(mCachedProcessInfo);
        }
        notifyRefreshStart();
        return refreshEnd()
                .map(endTime -> mCachedProcessInfo)
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Integer> getProcessCount() {
        if (mCachedProcessInfo != null) {
            return Observable.just(mCachedProcessInfo.size());
        }
        notifyRefreshStart();
        return Observable.just(AndroidProcesses.getRunningAppProcesses())
                .map(List::size)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Long> getTotalMemoryByte() {
        if (mCachedMemoryInfo != null) {
            return Observable.just(mCachedMemoryInfo.totalMem);
        }
        return Observable.just(cacheMemoryInfo())
                .map(info -> info.totalMem)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Long> getAvailMemoryByte() {
        if (mCachedMemoryInfo != null) {
            return Observable.just(mCachedMemoryInfo.availMem);
        }
        return Observable.just(cacheMemoryInfo())
                .map(info -> info.totalMem)
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<String> getSystemMemStatus() {
        return Observable.combineLatest(
                getAvailMemoryByte(), getTotalMemoryByte(),
                (availMemory, totalMemory) -> {
                    String formatAvailMem = Formatter.formatFileSize(MemOptApplication.getApplication(), availMemory);
                    String formatTotalMem = Formatter.formatFileSize(MemOptApplication.getApplication(), totalMemory);
                    return formatAvailMem + "/" + formatTotalMem;
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
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
        mRefreshEndNotification
                .subscribeOn(Schedulers.single())
                .subscribe(ignore -> mRefreshing = false);
    }

    private void notifyRefreshStart() {
        mRefreshStartNotification.onNext(System.currentTimeMillis());
    }

    private Observable<Long> refreshEnd() {
        return mRefreshEndNotification
                .take(1)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void refreshInternal() {
        Observable.just(AndroidProcesses.getRunningAppProcesses())
                .flatMap(Observable::fromIterable)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.computation())
                .map(ProcessInfoDataMapper::transform)
                .sorted((process1, process2) -> (int) (process2.getMemSize() - process1.getMemSize()) / (1024))
                .toList()
                .observeOn(Schedulers.single())
                .doOnSuccess(ignore -> cacheMemoryInfo())
                .doOnError(Throwable::printStackTrace)
                .retry()
                .doOnSuccess(processInfoList -> mCachedProcessInfo = processInfoList)
                .doOnSuccess(ignore -> mRefreshEndNotification.onNext(System.currentTimeMillis()))
                .subscribe();
    }

    private ActivityManager.MemoryInfo cacheMemoryInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        MemOptApplication.getActivityManager().getMemoryInfo(memoryInfo);
        mCachedMemoryInfo = memoryInfo;
        return memoryInfo;
    }
}
