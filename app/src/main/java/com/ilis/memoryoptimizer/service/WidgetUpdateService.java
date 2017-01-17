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
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class WidgetUpdateService extends Service {

    public static final String ACTION_CLEAR = CleanUpActionReceiver.class.getName() + ".CLEAR_ALL";

    private AppWidgetManager appWidgetManager;
    private CleanUpActionReceiver receiver;
    private ActivityManager activityManager;
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
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        componentName = new ComponentName(this, ProcessManagerWidget.class);
        startUpdateAction();
        initWidgetView();
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
        cleanupProcesses();
    }

    private void initReceiver() {
        receiver = new CleanUpActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CLEAR);
        registerReceiver(receiver, filter);
    }

    private void startUpdateAction() {
        updateDispose = Observable.interval(1, TimeUnit.MINUTES)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        ProcessInfoProvider.update();
                    }
                });
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
        if (viewDispose == null || viewDispose.isDisposed()) {
            viewDispose = ProcessInfoProvider.getProvider()
                    .doOnSubscribe(new Consumer<Disposable>() {
                        @Override
                        public void accept(Disposable disposable) throws Exception {
                            ProcessInfoProvider.update();
                        }
                    })
                    .subscribe(new Consumer<List<ProcessInfo>>() {
                        @Override
                        public void accept(List<ProcessInfo> newInfo) throws Exception {
                            int processCount = ProcessInfoProvider.getProcessCount();
                            String memStatus = ProcessInfoProvider.getSystemMemStatus();
                            int totalMemory = ProcessInfoProvider.getTotalMemory();
                            int usedMemory = ProcessInfoProvider.getUsedMemory();
                            views.setTextViewText(R.id.processCount, String.format(getString(R.string.process_count), processCount));
                            views.setTextViewText(R.id.memoryStatus, String.format(getString(R.string.memory_status), memStatus));
                            views.setProgressBar(R.id.availPercent, totalMemory, usedMemory, false);
                            try {
                                appWidgetManager.updateAppWidget(componentName, views);
                            } catch (RuntimeException e) {
                                initWidgetView();
                                System.gc();
                            }
                        }
                    });
        }
    }

    private void unBindRemoteView() {
        if (viewDispose != null && !viewDispose.isDisposed()) {
            viewDispose.dispose();
        }
    }

    private void endUpdateAction() {
        if (updateDispose != null && !updateDispose.isDisposed()) {
            updateDispose.dispose();
        }
    }

    private void cleanupProcesses() {
        Observable
                .create(new ObservableOnSubscribe<Long>() {
                    @Override
                    public void subscribe(ObservableEmitter<Long> e) throws Exception {
                        List<ProcessInfo> processInfoList = ProcessInfoProvider.getProcessInfo();
                        for (ProcessInfo info : processInfoList) {
                            if (info.getPackName().equals(getPackageName())) {
                                continue;
                            }
                            activityManager.killBackgroundProcesses(info.getPackName());
                        }
                        e.onNext(System.currentTimeMillis());
                        e.onComplete();
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        ProcessInfoProvider.update();
                    }
                })
                .flatMap(new Function<Long, ObservableSource<List<ProcessInfo>>>() {
                    @Override
                    public ObservableSource<List<ProcessInfo>> apply(Long aLong) throws Exception {
                        return ProcessInfoProvider.getProvider();
                    }
                })
                .take(1L)
                .subscribe(new Consumer<List<ProcessInfo>>() {
                    @Override
                    public void accept(List<ProcessInfo> newInfo) throws Exception {
                        int processCount = ProcessInfoProvider.getProcessCount();
                        String memStatus = ProcessInfoProvider.getSystemMemStatus();
                        int totalMemory = ProcessInfoProvider.getTotalMemory();
                        int usedMemory = ProcessInfoProvider.getUsedMemory();
                        views.setTextViewText(R.id.processCount, String.format(getString(R.string.process_count), processCount));
                        views.setTextViewText(R.id.memoryStatus, String.format(getString(R.string.memory_status), memStatus));
                        views.setProgressBar(R.id.availPercent, totalMemory, usedMemory, false);
                        views.setViewVisibility(R.id.cleanup, View.VISIBLE);
                        views.setViewVisibility(R.id.clearingView, View.GONE);
                        appWidgetManager.updateAppWidget(componentName, views);
                        ToastUtil.showToast("进程清理已完成,系统已达到最佳状态");
                    }
                });
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
