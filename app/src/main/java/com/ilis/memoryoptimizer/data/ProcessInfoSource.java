package com.ilis.memoryoptimizer.data;

import java.util.List;

import io.reactivex.Observable;

public interface ProcessInfoSource {

    ProcessInfoSource refresh();

    Observable<Long> refreshEnd();

    Observable<List<ProcessInfo>> getAllProcess(boolean ignoreCache);

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
