package com.vanilla.screentimeforsleepy.util;

import android.graphics.drawable.Drawable;

public class AppInfo {

    private String appName;
    private String packageName;
    private boolean isSystemApp;
    private Drawable appIcon;
    private boolean checked;
    private long installTime;
    private long updateTime;

    public AppInfo(String appName, String packageName, boolean isSystemApp, Drawable appIcon) {
        this.appName = appName;
        this.packageName = packageName;
        this.isSystemApp = isSystemApp;
        this.appIcon = appIcon;
        this.checked = false;
        this.installTime = 0;
        this.updateTime = 0;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public void setSystemApp(boolean systemApp) {
        isSystemApp = systemApp;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public long getInstallTime() {
        return installTime;
    }

    public void setInstallTime(long installTime) {
        this.installTime = installTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
