package com.ilis.memoryoptimizer.data;

import java.util.List;

import io.reactivex.Observable;

public interface ProcessInfoSource {

    void refresh();

    Observable<List<ProcessInfo>> getAllProcess();

    Observable<Integer> getProcessCount();

    /**
     * Avail/TotalMem (Mega byte String)
     */
    Observable<String> getSystemMemStatus();

    /**
     * Mega byte
     */
    Observable<Integer> getTotalMemory();

    /**
     * Mega byte
     */
    Observable<Integer> getUsedMemory();
}
