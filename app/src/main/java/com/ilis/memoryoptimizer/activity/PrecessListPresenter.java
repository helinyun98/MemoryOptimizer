package com.ilis.memoryoptimizer.activity;

import android.text.TextUtils;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.ilis.memoryoptimizer.data.ProcessInfo;
import com.ilis.memoryoptimizer.data.ProcessInfoSource;
import com.ilis.memoryoptimizer.util.ProcessCleanUpHelper;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.ArrayCompositeSubscription;
import io.reactivex.schedulers.Schedulers;

public class PrecessListPresenter {

    private ProcessInfoSource infoRepository;
    private ProcessListView view;
    private List<ProcessInfo> mData = new ArrayList<>();
    private CompositeDisposable disposable = new CompositeDisposable();

    public PrecessListPresenter(ProcessInfoSource infoRepository, ProcessListView view) {
        this.infoRepository = infoRepository;
        this.view = view;
    }

    public void refresh() {
        infoRepository.refresh();
        view.showReloading();
        disposable.add(infoRepository.getAllProcess()
                .doOnNext(this::saveProcessData)
                .doOnNext(ignore -> view.showProcess(mData))
                .doOnNext(infoList -> view.showProcessCount(infoList.size()))
                .flatMap(ignore -> infoRepository.getSystemMemStatus())
                .subscribe(
                        view::showMemStatus,
                        Throwable::printStackTrace,
                        view::hideReloading));
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
        disposable.add(Observable.fromIterable(mData)
                .subscribeOn(Schedulers.single())
                .doOnSubscribe(ignore -> view.showReloading())
                .observeOn(Schedulers.newThread())
                .filter(ProcessInfo::isPrepareToClean)
                .map(ProcessInfo::getProcessName)
                .toList()
                .flatMapObservable(ProcessCleanUpHelper::cleanupListProcess)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(endTime -> refresh()));
    }

    public void saveProcessData(List<ProcessInfo> data) {
        mData.clear();
        mData.addAll(data);
    }

    public void destroy() {
        disposable.clear();
        infoRepository = null;
        view = null;
        mData = null;
    }
}
