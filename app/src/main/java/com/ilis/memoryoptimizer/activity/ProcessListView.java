package com.ilis.memoryoptimizer.activity;

import com.ilis.memoryoptimizer.data.ProcessInfo;

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
