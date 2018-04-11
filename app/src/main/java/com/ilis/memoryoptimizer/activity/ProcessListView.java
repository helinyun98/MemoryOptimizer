package com.ilis.memoryoptimizer.activity;

import com.ilis.memoryoptimizer.data.ProcessInfo;
import com.trello.rxlifecycle2.LifecycleProvider;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.List;

public interface ProcessListView {

    void showReloading();

    void hideReloading();

    void showProcess(List<ProcessInfo> infoList);

    void notifyListChange();

    void showMemStatus(String MemStatus);

    void showProcessCount(int count);

    void showToast(String text);

}
