package com.ilis.memoryoptimizer.holder;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.ilis.memoryoptimizer.R;
import com.ilis.memoryoptimizer.modle.ProcessInfo;
import com.ilis.memoryoptimizer.util.ProcessInfoProvider;

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

    public void update(Activity activity, final ProcessInfo processInfo) {
        appIcon.setImageDrawable(processInfo.getIcon());
        appName.setText(processInfo.getAppName());
        memSize.setText(Formatter.formatFileSize(activity, processInfo.getMemSize()));
        if (processInfo.isUserProcess()) {
            appLabel.setImageResource(R.drawable.ic_user_app);
        } else {
            appLabel.setImageResource(R.drawable.ic_system_app);
        }
        checkbox.setOnCheckedChangeListener(null);
        if (processInfo.getPackName().equals(ProcessInfoProvider.getCurrentPackageName())) {
            checkbox.setEnabled(false);
            checkbox.setChecked(false);
            processInfo.setChecked(false);
            itemView.setOnClickListener(null);
        } else {
            checkbox.setEnabled(true);
            checkbox.setChecked(processInfo.isChecked());
            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    processInfo.setChecked(isChecked);
                }
            });
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkbox.toggle();
                }
            });
        }
    }
}
