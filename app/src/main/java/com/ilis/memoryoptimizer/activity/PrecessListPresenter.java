package com.ilis.memoryoptimizer.activity;

import android.text.TextUtils;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.ilis.memoryoptimizer.data.ProcessInfo;
import com.ilis.memoryoptimizer.data.ProcessInfoSource;
import com.ilis.memoryoptimizer.util.ProcessCleanUpHelper;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class PrecessListPresenter {

    private ProcessInfoSource infoRepository;
    private ProcessListView view;
    private List<ProcessInfo> mData = new ArrayList<>();

    public PrecessListPresenter(ProcessInfoSource infoRepository, ProcessListView view) {
        this.infoRepository = infoRepository;
        this.view = view;
    }

    public void refresh() {
        infoRepository.refresh();
        view.showReloading();
        infoRepository.getAllProcess()
                .doOnNext(this::saveProcessData)
                .doOnNext(ignore -> view.showProcess(mData))
                .doOnNext(infoList -> view.showProcessCount(infoList.size()))
                .flatMap(ignore -> infoRepository.getSystemMemStatus())
                .subscribe(
                        view::showMemStatus,
                        Throwable::printStackTrace,
                        view::hideReloading);
    }

    public void selectAll() {
        for (ProcessInfo info : mData) {
            boolean notSelf = !TextUtils.equals(info.getPackageName(), MemOptApplication.getCurrentPackageName());
            info.setPrepareToClean(notSelf);
        }
        view.notifyListChange();
    }

    public void selectOther() {
        for (ProcessInfo info : mData) {
            boolean notSelf = !TextUtils.equals(info.getPackageName(), MemOptApplication.getCurrentPackageName());
            if (notSelf) {
                info.setPrepareToClean(!info.isPrepareToClean());
            }
        }
        view.notifyListChange();
    }

    public void cleanupAllSelected() {
        Observable.fromArray(mData.toArray())
                .subscribeOn(Schedulers.single())
                .doOnSubscribe(ignore -> view.showReloading())
                .observeOn(Schedulers.newThread())
                .cast(ProcessInfo.class)
                .filter(ProcessInfo::isPrepareToClean)
                .map(ProcessInfo::getProcessName)
                .toList()
                .flatMapObservable(ProcessCleanUpHelper::cleanupListProcess)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(endTime -> refresh());
    }

    public void saveProcessData(List<ProcessInfo> data) {
        mData.clear();
        mData.addAll(data);
    }

    public void destroy() {
        infoRepository = null;
        view = null;
        mData = null;
    }
}
