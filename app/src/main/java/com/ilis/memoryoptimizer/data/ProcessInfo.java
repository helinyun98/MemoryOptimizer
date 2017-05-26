package com.ilis.memoryoptimizer.data;

public final class ProcessInfo {

    private String processName;
    private String packageName;
    private String appName;
    private long memSize;
    private boolean userProcess;
    private boolean prepareToClean;

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
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

    public boolean isPrepareToClean() {
        return prepareToClean;
    }

    public void setPrepareToClean(boolean prepareToClean) {
        this.prepareToClean = prepareToClean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessInfo that = (ProcessInfo) o;

        if (memSize != that.memSize) return false;
        if (userProcess != that.userProcess) return false;
        if (!processName.equals(that.processName)) return false;
        if (packageName != null ? !packageName.equals(that.packageName) : that.packageName != null)
            return false;
        return appName.equals(that.appName);

    }

    @Override
    public int hashCode() {
        int result = processName.hashCode();
        result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
        result = 31 * result + appName.hashCode();
        result = 31 * result + (int) (memSize ^ (memSize >>> 32));
        result = 31 * result + (userProcess ? 1 : 0);
        return result;
    }
}
