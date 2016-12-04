package com.ilis.memoryoptimizer.util;

import android.support.v7.util.DiffUtil;

import com.ilis.memoryoptimizer.modle.ProcessInfo;

import java.util.List;


public class ProcessInfoDiff extends DiffUtil.Callback {

    private List<ProcessInfo> newData;
    private List<ProcessInfo> oldData;

    public ProcessInfoDiff(List<ProcessInfo> newData, List<ProcessInfo> oldData) {
        this.newData = newData;
        this.oldData = oldData;
    }

    @Override
    public int getOldListSize() {
        return oldData == null ? 0 : oldData.size();
    }

    @Override
    public int getNewListSize() {
        return newData == null ? 0 : newData.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        ProcessInfo oldItem = oldData.get(oldItemPosition);
        ProcessInfo newItem = newData.get(newItemPosition);
        return oldItem.getPackName().equals(newItem.getPackName());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ProcessInfo oldItem = oldData.get(oldItemPosition);
        ProcessInfo newItem = newData.get(newItemPosition);
        return oldItem.hashCode() == newItem.hashCode() && oldItem.equals(newItem);
    }
}
