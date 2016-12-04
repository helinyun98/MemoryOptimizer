package com.ilis.memoryoptimizer.service;

import android.app.ActivityManager;
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
import com.ilis.memoryoptimizer.modle.ProcessInfo;
import com.ilis.memoryoptimizer.util.ProcessInfoProvider;
import com.ilis.memoryoptimizer.util.ToastUtil;
import com.ilis.memoryoptimizer.widget.ProcessManagerWidget;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;

public class WidgetUpdateService extends Service {

    private static final String ACTION_CLEAR = "com.ilis.memoryoptimizer.CLEAR_ALL";

    private AppWidgetManager appWidgetManager;
    private Timer timer;
    private CleanUpActionReceiver receiver;
    private ActivityManager activityManager;
    private ComponentName componentName;
    private RemoteViews views;
    private Disposable disposable;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        appWidgetManager = AppWidgetManager.getInstance(this);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        componentName = new ComponentName(this, ProcessManagerWidget.class);

        initWidgetView();
        bindRemoteView();

        timer = new Timer();
        timer.schedule(new UpdateTimerTask(), 0, 60 * 1000);

        receiver = new CleanUpActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CLEAR);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        unBindRemoteView();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        cleanupProcesses();
    }

    private void initWidgetView() {
        views = new RemoteViews(getPackageName(), R.layout.widget_manager_view);
        Intent intentBroadcast = new Intent(ACTION_CLEAR);
        PendingIntent pendingIntentBroadcast
                = PendingIntent.getBroadcast(this, 0, intentBroadcast, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.cleanup, pendingIntentBroadcast);
        Intent intentActivity = new Intent(this, MainActivity.class);
        intentActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntentActivity
                = PendingIntent.getActivity(this, 0, intentActivity, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntentActivity);
    }

    private void bindRemoteView() {
        disposable = ProcessInfoProvider.getProvider()
                .subscribe(new Consumer<List<ProcessInfo>>() {
                    @Override
                    public void accept(List<ProcessInfo> processInfos) throws Exception {
                        int processCount = ProcessInfoProvider.getProcessCount();
                        String memStatus = ProcessInfoProvider.getSystemMemStatus();
                        int totalMemory = ProcessInfoProvider.getTotalMemory();
                        int usedMemory = ProcessInfoProvider.getUsedMemory();
                        views.setTextViewText(R.id.processCount, String.format(getString(R.string.process_count), processCount));
                        views.setTextViewText(R.id.memoryStatus, String.format(getString(R.string.memory_status), memStatus));
                        views.setProgressBar(R.id.availPercent, totalMemory, usedMemory, false);
                        views.setViewVisibility(R.id.cleanup, View.VISIBLE);
                        views.setViewVisibility(R.id.clearingView, View.GONE);
                        try {
                            appWidgetManager.updateAppWidget(componentName, views);
                        } catch (RuntimeException e) {
                            initWidgetView();
                            System.gc();
                        }
                    }
                });
    }

    private void unBindRemoteView() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private void cleanupProcesses() {
        Observable
                .create(new ObservableOnSubscribe<Void>() {
                    @Override
                    public void subscribe(ObservableEmitter<Void> e) throws Exception {
                        List<ProcessInfo> processInfoList = ProcessInfoProvider.getProcessInfo();
                        for (ProcessInfo info : processInfoList) {
                            if (info.getPackName().equals(getPackageName())) {
                                continue;
                            }
                            activityManager.killBackgroundProcesses(info.getPackName());
                        }
                        e.onComplete();
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultObserver<Void>() {
                    @Override
                    public void onNext(Void value) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        ProcessInfoProvider.update();
                        ToastUtil.showToast("进程清理已完成,系统已达到最佳状态");
                    }
                });
    }

    private class UpdateTimerTask extends TimerTask {
        @Override
        public void run() {
            ProcessInfoProvider.update();
        }
    }

    private class CleanUpActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            views.setViewVisibility(R.id.cleanup, View.INVISIBLE);
            views.setViewVisibility(R.id.clearingView, View.VISIBLE);
            views.setTextViewText(R.id.processCount, "进程清理进行中...");
            appWidgetManager.updateAppWidget(componentName, views);
            cleanupProcesses();
        }
    }

}
