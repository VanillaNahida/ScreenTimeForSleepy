package com.vanilla.screentimeforsleepy;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UsageStatsHelper {
    private static final String TAG = "UsageStatsHelper";
    private final Context context;
    private final UsageStatsManager usageStatsManager;
    private final PackageManager packageManager;
    
    public UsageStatsHelper(Context context) {
        this.context = context;
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.packageManager = context.getPackageManager();
    }
    
    /**
     * 获取当日应用使用统计
     */
    public List<AppUsageInfo> getTodayUsageStats(boolean hideSystemApps) {
        // 获取AppFilterManager实例
        AppFilterManager filterManager = new AppFilterManager(context);
        
        // 获取今天凌晨的时间戳
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        
        // 获取当前时间戳
        long endTime = System.currentTimeMillis();
        
        // 获取应用使用统计
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        
        // 转换为AppUsageInfo列表
        List<AppUsageInfo> appUsageInfoList = new ArrayList<>();
        
        for (UsageStats usageStats : usageStatsList) {
            // 跳过使用时长为0的应用
            if (usageStats.getTotalTimeInForeground() > 0) {
                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(usageStats.getPackageName(), 0);
                    
                    // 检查是否为系统应用，如果需要隐藏则跳过
                    if (hideSystemApps && isSystemApp(appInfo)) {
                        continue;
                    }
                    
                    String appName = packageManager.getApplicationLabel(appInfo).toString();
                    String packageName = appInfo.packageName;
                    
                    // 根据黑白名单过滤应用
                    if (!filterManager.shouldIncludeApp(packageName)) {
                        continue;
                    }
                    
                    AppUsageInfo appUsageInfo = new AppUsageInfo(
                            appName,
                            usageStats.getPackageName(),
                            usageStats.getTotalTimeInForeground(),
                            appInfo.loadIcon(packageManager)
                    );
                    
                    appUsageInfoList.add(appUsageInfo);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Package not found: " + usageStats.getPackageName(), e);
                }
            }
        }
        
        // 按使用时长降序排序
        appUsageInfoList.sort((a, b) -> Long.compare(b.getUsageTime(), a.getUsageTime()));
        
        return appUsageInfoList;
    }
    
    /**
     * 检查是否为系统应用
     */
    private boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
    
    /**
     * 获取当日应用使用统计（默认隐藏系统应用）
     */
    public List<AppUsageInfo> getTodayUsageStats() {
        return getTodayUsageStats(true);
    }
    
    /**
     * 格式化使用时长为可读格式
     */
    public static String formatUsageTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d分%02d秒", minutes, seconds);
    }
    
    /**
     * 计算最大使用时长
     */
    public static long getMaxUsageTime(List<AppUsageInfo> appUsageInfoList) {
        long maxTime = 0;
        for (AppUsageInfo appUsageInfo : appUsageInfoList) {
            if (appUsageInfo.getUsageTime() > maxTime) {
                maxTime = appUsageInfo.getUsageTime();
            }
        }
        return maxTime;
    }
}