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

import com.ilis.memoryoptimizer.R;
import com.ilis.memoryoptimizer.activity.MainActivity;
import com.ilis.memoryoptimizer.data.ProcessInfoRepository;
import com.ilis.memoryoptimizer.util.ProcessCleanUpHelper;
import com.ilis.memoryoptimizer.widget.ProcessManagerWidget;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.ilis.memoryoptimizer.R.id.cleanup;
import static com.ilis.memoryoptimizer.R.id.processCount;
import static com.ilis.memoryoptimizer.widget.ProcessManagerWidget.ACTION_CLEAR;

public class WidgetUpdateService extends Service {

    private AppWidgetManager appWidgetManager;
    private CleanUpActionReceiver receiver;
    private ComponentName componentName;
    private RemoteViews views;
    private Disposable viewDispose;
    private Disposable updateDispose;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        appWidgetManager = AppWidgetManager.getInstance(this);
        componentName = new ComponentName(this, ProcessManagerWidget.class);
        initWidgetView();
        startUpdateAction();
        bindRemoteView();
        initReceiver();
    }

    @Override
    public void onDestroy() {
        endUpdateAction();
        unBindRemoteView();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        ProcessCleanUpHelper.cleanupAllProcess();
    }

    private void initReceiver() {
        receiver = new CleanUpActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CLEAR);
        registerReceiver(receiver, filter);
    }

    private void bindRemoteView() {
        if (viewDispose == null || viewDispose.isDisposed()) {
            viewDispose = ProcessInfoRepository.getInstance()
                    .refresh()
                    .refreshEnd()
                    .subscribe(endTime -> setupWidget());
        }
    }

    private void unBindRemoteView() {
        if (viewDispose != null && !viewDispose.isDisposed()) {
            viewDispose.dispose();
        }
    }

    private void startUpdateAction() {
        updateDispose = Observable.interval(1, TimeUnit.MINUTES)
                .subscribe(aLong -> ProcessInfoRepository.getInstance().refresh());
    }

    private void endUpdateAction() {
        if (updateDispose != null && !updateDispose.isDisposed()) {
            updateDispose.dispose();
        }
    }

    private void initWidgetView() {
        views = new RemoteViews(getPackageName(), R.layout.widget_manager_view);
        Intent intentBroadcast = new Intent(ACTION_CLEAR);
        PendingIntent pendingIntentBroadcast
                = PendingIntent.getBroadcast(this, 0, intentBroadcast, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(cleanup, pendingIntentBroadcast);
        Intent intentActivity = new Intent(this, MainActivity.class);
        intentActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntentActivity
                = PendingIntent.getActivity(this, 0, intentActivity, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntentActivity);
    }

    private void setupWidget() {
        Observable.combineLatest(
                ProcessInfoRepository.getInstance().getProcessCount(),
                ProcessInfoRepository.getInstance().getSystemMemStatus(),
                ProcessInfoRepository.getInstance().getTotalMemory(),
                ProcessInfoRepository.getInstance().getUsedMemory(),
                (processCount1, memStatus, totalMemory, usedMemory) -> {
                    views.setTextViewText(processCount, String.format(getString(R.string.process_count), processCount1));
                    views.setTextViewText(R.id.memoryStatus, String.format(getString(R.string.memory_status), memStatus));
                    views.setProgressBar(R.id.availPercent, totalMemory, usedMemory, false);
                    views.setViewVisibility(cleanup, View.VISIBLE);
                    views.setViewVisibility(R.id.clearingView, View.GONE);
                    updateWidget(views);
                    return System.currentTimeMillis();
                });
    }

    private class CleanUpActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            views.setViewVisibility(cleanup, View.INVISIBLE);
            views.setViewVisibility(R.id.clearingView, View.VISIBLE);
            views.setTextViewText(processCount, "进程清理进行中...");
            updateWidget(views);
        }
    }

    private void updateWidget(RemoteViews views) {
        try {
            appWidgetManager.updateAppWidget(componentName, views);
        } catch (RuntimeException e) {
            e.printStackTrace();
            initWidgetView();
            System.gc();
        }
    }
}
