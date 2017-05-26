package com.ilis.memoryoptimizer.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.widget.ImageView;

public class PackageIconLoader {

    private static Drawable defaultIcon;

    public static void loadAppIcon(Context context, ImageView view, String packageName) {
        try {
            Drawable icon = context.getPackageManager().getApplicationIcon(packageName);
            view.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            if (defaultIcon != null) {
                defaultIcon = ResourcesCompat.getDrawable(context.getResources(), android.R.mipmap.sym_def_app_icon, null);
            }
            view.setImageDrawable(defaultIcon);
        }
    }
}
