package com.ilis.memoryoptimizer.adapter;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.ilis.memoryoptimizer.activity.MainActivity;
import com.ilis.memoryoptimizer.data.ProcessInfo;
import com.ilis.memoryoptimizer.holder.ProcessViewHolder;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class ProcessListAdapter extends RecyclerView.Adapter<ProcessViewHolder> {

    private final WeakReference<Activity> activityWeakReference;
    private List<ProcessInfo> data = Collections.emptyList();

    public ProcessListAdapter(MainActivity mainActivity) {
        activityWeakReference = new WeakReference<>(mainActivity);
    }

    public void setData(List<ProcessInfo> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void notifyListChange() {
        notifyItemRangeChanged(0, data.size());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private Activity getActivity() {
        return activityWeakReference.get();
    }

    @NonNull
    @Override
    public ProcessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ProcessViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ProcessViewHolder holder, int position) {
        holder.update(getActivity(), data.get(position));
    }
}
