package com.ilis.memoryoptimizer.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.ilis.memoryoptimizer.data.ProcessInfoRepository;
import com.ilis.memoryoptimizer.service.WidgetUpdateService;
import com.ilis.memoryoptimizer.util.ProcessCleanUpHelper;
import com.ilis.memoryoptimizer.util.ToastUtil;

public class ProcessManagerWidget extends AppWidgetProvider {

    public static final String ACTION_CLEAR = MemOptApplication.getCurrentPackageName() + ".CLEAR_ALL";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (TextUtils.equals(ACTION_CLEAR, intent.getAction())) {
            ProcessCleanUpHelper.cleanupAllProcess()
                    .flatMap(ignore -> ProcessInfoRepository.getInstance().getAllProcess(true))
                    .doOnComplete(() -> ToastUtil.showToast("进程清理已完成,系统已达到最佳状态"));
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, WidgetUpdateService.class);
        context.startService(intent);
    }

    @Override
    public void onEnabled(Context context) {
        Intent intent = new Intent(context, WidgetUpdateService.class);
        context.startService(intent);
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        Intent intent = new Intent(context, WidgetUpdateService.class);
        context.stopService(intent);
        super.onDisabled(context);
    }
}

