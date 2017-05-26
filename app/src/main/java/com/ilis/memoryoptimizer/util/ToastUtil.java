package com.ilis.memoryoptimizer.util;

import android.support.annotation.NonNull;
import android.widget.Toast;

import com.ilis.memoryoptimizer.MemOptApplication;

import static com.ilis.memoryoptimizer.MemOptApplication.isMainThread;

public class ToastUtil {

    private static Toast toast;

    public static void showToast(@NonNull final String text) {
        showToast(text, false);
    }

    public static void showToast(@NonNull final String text, final boolean isLongToast) {
        if (toast != null) {
            toast.cancel();
        }
        if (isMainThread()) {
            toast = Toast.makeText(
                    MemOptApplication.getApplication(),
                    text,
                    isLongToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
            );
            toast.show();
        } else {
            MemOptApplication
                    .getMainHandler()
                    .post(() -> {
                        toast = Toast.makeText(
                                MemOptApplication.getApplication(),
                                text,
                                isLongToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
                        );
                        toast.show();
                    });
        }
    }

}
