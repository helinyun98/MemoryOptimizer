package com.ilis.memoryoptimizer.util;

import android.text.TextUtils;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.ilis.memoryoptimizer.data.ProcessInfo;
import com.ilis.memoryoptimizer.data.ProcessInfoRepository;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class ProcessCleanUpHelper {

    public static Observable<Long> cleanupAllProcess() {
        return ProcessInfoRepository.getInstance()
                .getAllProcess(false)
                .observeOn(Schedulers.newThread())
                .flatMap(processInfoList ->
                        Observable.fromArray(processInfoList.toArray(new ProcessInfo[0])))
                .filter(processInfo ->
                        !TextUtils.equals(
                                MemOptApplication.getCurrentPackageName(),
                                processInfo.getPackageName()))
                .doOnNext(processInfo ->
                        MemOptApplication.getActivityManager()
                                .killBackgroundProcesses(processInfo.getPackageName()))
                .toList()
                .observeOn(Schedulers.single())
                .map(ignore -> System.currentTimeMillis())
                .onErrorReturn(ignore -> System.currentTimeMillis())
                .toObservable();
    }
}
