package com.vanilla.screentimeforsleepy.util;

import android.graphics.drawable.Drawable;

public class AppUsageInfo {
    private final String appName;
    private final String packageName;
    private final long usageTime;
    private final Drawable appIcon;
    private final String alias;
    
    public AppUsageInfo(String appName, String packageName, long usageTime, Drawable appIcon) {
        this.appName = appName;
        this.packageName = packageName;
        this.usageTime = usageTime;
        this.appIcon = appIcon;
        this.alias = null;
    }
    
    public AppUsageInfo(String appName, String packageName, long usageTime, Drawable appIcon, String alias) {
        this.appName = appName;
        this.packageName = packageName;
        this.usageTime = usageTime;
        this.appIcon = appIcon;
        this.alias = alias;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public long getUsageTime() {
        return usageTime;
    }
    
    public Drawable getAppIcon() {
        return appIcon;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public String getDisplayName() {
        return alias != null ? alias : appName;
    }
}