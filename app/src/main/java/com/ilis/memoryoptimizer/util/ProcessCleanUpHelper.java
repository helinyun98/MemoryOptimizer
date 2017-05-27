package com.ilis.memoryoptimizer.util;

import android.text.TextUtils;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.ilis.memoryoptimizer.data.ProcessInfo;
import com.ilis.memoryoptimizer.data.ProcessInfoRepository;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class ProcessCleanUpHelper {

    public static Observable<Long> cleanupAllProcess() {
        return ProcessInfoRepository.getInstance()
                .getAllProcess()
                .observeOn(Schedulers.newThread())
                .flatMap(processInfoList ->
                        Observable.fromArray(processInfoList.toArray()))
                .cast(ProcessInfo.class)
                .doOnError(Throwable::printStackTrace)
                .filter(processInfo ->
                        !TextUtils.equals(
                                MemOptApplication.getCurrentPackageName(),
                                processInfo.getPackageName()))
                .doOnNext(processInfo ->
                        MemOptApplication.getActivityManager()
                                .killBackgroundProcesses(processInfo.getProcessName()))
                .toList()
                .observeOn(Schedulers.single())
                .map(ignore -> System.currentTimeMillis())
                .onErrorReturn(ignore -> System.currentTimeMillis())
                .doOnSuccess(ignore -> ProcessInfoRepository.getInstance().refresh())
                .toObservable();
    }

    public static Observable<Long> cleanupListProcess(List<String> processNameList) {
        return Observable.fromArray(processNameList.toArray())
                .cast(String.class)
                .doOnError(Throwable::printStackTrace)
                .filter(processName ->
                        !TextUtils.equals(
                                MemOptApplication.getCurrentPackageName(),
                                processName))
                .doOnNext(processName ->
                        MemOptApplication.getActivityManager()
                                .killBackgroundProcesses(processName))
                .toList()
                .observeOn(Schedulers.single())
                .map(ignore -> System.currentTimeMillis())
                .onErrorReturn(ignore -> System.currentTimeMillis())
                .doOnSuccess(ignore -> ProcessInfoRepository.getInstance().refresh())
                .toObservable();
    }
}
