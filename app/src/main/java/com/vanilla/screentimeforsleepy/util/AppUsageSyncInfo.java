package com.vanilla.screentimeforsleepy.util;

public class AppUsageSyncInfo {
    private String name;
    private String icon;
    private int totalTime;
    
    public AppUsageSyncInfo() {
    }
    
    public AppUsageSyncInfo(String name, String icon, int totalTime) {
        this.name = name;
        this.icon = icon;
        this.totalTime = totalTime;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public int getTotalTime() {
        return totalTime;
    }
    
    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }
}
