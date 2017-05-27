package com.ilis.memoryoptimizer.holder;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.ilis.memoryoptimizer.R;
import com.ilis.memoryoptimizer.data.ProcessInfo;
import com.ilis.memoryoptimizer.util.PackageIconLoader;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProcessViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.appIcon)
    ImageView appIcon;
    @BindView(R.id.appName)
    TextView appName;
    @BindView(R.id.memSize)
    TextView memSize;
    @BindView(R.id.checkbox)
    CheckBox checkbox;
    @BindView(R.id.appLabel)
    ImageView appLabel;

    private ProcessViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public static ProcessViewHolder create(ViewGroup parent) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_process, parent, false);
        return new ProcessViewHolder(itemView);
    }

    public void update(Activity context, final ProcessInfo processInfo) {
        PackageIconLoader.loadAppIcon(context, appIcon, processInfo.getPackageName());
        appName.setText(processInfo.getAppName());
        memSize.setText(Formatter.formatFileSize(context, processInfo.getMemSize()));
        appLabel.setImageResource(processInfo.isUserProcess() ? R.drawable.ic_user_app : R.drawable.ic_system_app);
        checkbox.setOnCheckedChangeListener(null);
        if (TextUtils.equals(MemOptApplication.getCurrentPackageName(), processInfo.getPackageName())) {
            checkbox.setEnabled(false);
            checkbox.setChecked(false);
            processInfo.setPrepareToClean(false);
            itemView.setOnClickListener(null);
        } else {
            checkbox.setEnabled(true);
            checkbox.setChecked(processInfo.isPrepareToClean());
            itemView.setOnClickListener(v -> checkbox.toggle());
            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> processInfo.setPrepareToClean(isChecked));
        }
    }
}
