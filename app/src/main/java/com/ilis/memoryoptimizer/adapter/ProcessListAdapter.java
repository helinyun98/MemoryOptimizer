package com.ilis.memoryoptimizer.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.ilis.memoryoptimizer.activity.MainActivity;
import com.ilis.memoryoptimizer.holder.ProcessViewCallback;
import com.ilis.memoryoptimizer.holder.ProcessViewHolder;
import com.ilis.memoryoptimizer.modle.ProcessInfo;

import java.lang.ref.WeakReference;
import java.util.List;

public class ProcessListAdapter extends RecyclerView.Adapter<ProcessViewHolder> {

    private final WeakReference<Activity> activityWeakReference;
    private List<ProcessInfo> processInfos;
    private ProcessViewCallback callBack;

    public ProcessListAdapter(MainActivity mainActivity, List<ProcessInfo> processInfos) {
        activityWeakReference = new WeakReference<Activity>(mainActivity);
        this.processInfos = processInfos;
    }

    @Override
    public int getItemCount() {
        return processInfos.size();
    }

    private Activity getActivity() {
        return activityWeakReference.get();
    }

    @Override
    public ProcessViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return ProcessViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(ProcessViewHolder holder, int position) {
        holder.update(getActivity(), processInfos.get(position), callBack);
    }

    public void setProcessViewCallBack(ProcessViewCallback callBack) {
        this.callBack = callBack;
    }

}
