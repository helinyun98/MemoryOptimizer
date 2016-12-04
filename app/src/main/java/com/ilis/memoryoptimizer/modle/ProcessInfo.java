package com.ilis.memoryoptimizer.modle;

import android.graphics.drawable.Drawable;

public class ProcessInfo {

    private String packName;
    private String appName;
    private Drawable icon;
    private long memSize;
    private boolean userProcess;
    private boolean checked;

    public String getPackName() {
        return packName;
    }

    public void setPackName(String packName) {
        this.packName = packName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public long getMemSize() {
        return memSize;
    }

    public void setMemSize(long memSize) {
        this.memSize = memSize;
    }

    public boolean isUserProcess() {
        return userProcess;
    }

    public void setUserProcess(boolean userProcess) {
        this.userProcess = userProcess;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessInfo that = (ProcessInfo) o;

        if (memSize != that.memSize) return false;
        if (userProcess != that.userProcess) return false;
        if (checked != that.checked) return false;
        if (packName != null ? !packName.equals(that.packName) : that.packName != null)
            return false;
        if (appName != null ? !appName.equals(that.appName) : that.appName != null) return false;
        return icon != null ? icon.equals(that.icon) : that.icon == null;

    }

    @Override
    public int hashCode() {
        int result = packName != null ? packName.hashCode() : 0;
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        result = 31 * result + (icon != null ? icon.hashCode() : 0);
        result = 31 * result + (int) (memSize ^ (memSize >>> 32));
        result = 31 * result + (userProcess ? 1 : 0);
        result = 31 * result + (checked ? 1 : 0);
        return result;
    }
}
