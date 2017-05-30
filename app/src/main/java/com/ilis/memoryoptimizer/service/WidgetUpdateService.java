package com.ilis.memoryoptimizer.service;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import com.ilis.memoryoptimizer.MemOptApplication;
import com.ilis.memoryoptimizer.R;
import com.ilis.memoryoptimizer.activity.MainActivity;
import com.ilis.memoryoptimizer.data.ProcessInfoRepository;
import com.ilis.memoryoptimizer.util.ProcessCleanUpHelper;
import com.ilis.memoryoptimizer.util.ToastUtil;
import com.ilis.memoryoptimizer.widget.ProcessManagerWidget;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.ilis.memoryoptimizer.R.id.cleanup;
import static com.ilis.memoryoptimizer.R.id.processCount;

public class WidgetUpdateService extends Service {

    private AppWidgetManager appWidgetManager;
    private CleanUpActionReceiver receiver;
    private ComponentName componentName;
    private RemoteViews views;
    private Disposable updateDispose;

    public static final String ACTION_CLEAR = MemOptApplication.getCurrentPackageName() + ".CLEAR_ALL";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        appWidgetManager = AppWidgetManager.getInstance(this);
        componentName = new ComponentName(this, ProcessManagerWidget.class);
        initWidgetView();
        setupWidget();
        startUpdateAction();
        initReceiver();
    }

    @Override
    public void onDestroy() {
        endUpdateAction();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Intent actionCleanup = new Intent(ACTION_CLEAR);
        sendBroadcast(actionCleanup);
    }

    private void initReceiver() {
        receiver = new CleanUpActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CLEAR);
        registerReceiver(receiver, filter);
    }

    private void startUpdateAction() {
        updateDispose = Observable.interval(3, TimeUnit.MINUTES)
                .subscribe(aLong -> {
                    ProcessInfoRepository.getInstance().refresh();
                    setupWidget();
                });
    }

    private void endUpdateAction() {
        if (updateDispose != null && !updateDispose.isDisposed()) {
            updateDispose.dispose();
        }
    }

    private class CleanUpActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            views.setViewVisibility(cleanup, View.INVISIBLE);
            views.setViewVisibility(R.id.clearingView, View.VISIBLE);
            views.setTextViewText(processCount, "进程清理进行中...");
            updateWidget(views);
            cleanupAction();
        }
    }

    private void cleanupAction() {
        ProcessCleanUpHelper.cleanupAllProcess()
                .subscribe(ignore -> setupWidget(),
                        Throwable::printStackTrace,
                        () -> ToastUtil.showToast("进程清理已完成,系统已达到最佳状态"));
    }

    private void initWidgetView() {
        views = new RemoteViews(getPackageName(), R.layout.widget_manager_view);

        PendingIntent actionCleanup = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_CLEAR),
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(cleanup, actionCleanup);

        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openActivity = PendingIntent.getActivity(this, 0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widgetLayout, openActivity);
    }

    private void setupWidget() {
        Observable.combineLatest(
                ProcessInfoRepository.getInstance().getProcessCount(),
                ProcessInfoRepository.getInstance().getSystemMemStatus(),
                ProcessInfoRepository.getInstance().getTotalMemory(),
                ProcessInfoRepository.getInstance().getUsedMemory(),
                (processCount, memStatus, totalMemory, usedMemory) -> {
                    views.setTextViewText(R.id.processCount, String.format(getString(R.string.process_count), processCount));
                    views.setTextViewText(R.id.memoryStatus, String.format(getString(R.string.memory_status), memStatus));
                    views.setProgressBar(R.id.availPercent, totalMemory, usedMemory, false);
                    views.setViewVisibility(cleanup, View.VISIBLE);
                    views.setViewVisibility(R.id.clearingView, View.GONE);
                    updateWidget(views);
                    return System.currentTimeMillis();
                })
                .subscribe();
    }

    private void updateWidget(RemoteViews views) {
        try {
            appWidgetManager.updateAppWidget(componentName, views);
        } catch (RuntimeException e) {
            e.printStackTrace();
            initWidgetView();
            setupWidget();
            System.gc();
        }
    }
}
