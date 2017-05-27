package com.ilis.memoryoptimizer.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import com.ilis.memoryoptimizer.service.WidgetUpdateService;

public class ProcessManagerWidget extends AppWidgetProvider {

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

