package com.vanilla.screentimeforsleepy;

public class AppUsageSyncInfo {
    private String icon;
    private int totalTime;
    
    public AppUsageSyncInfo() {
    }
    
    public AppUsageSyncInfo(String icon, int totalTime) {
        this.icon = icon;
        this.totalTime = totalTime;
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
