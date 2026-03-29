package com.vanilla.screentimeforsleepy;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;

import com.vanilla.screentimeforsleepy.manager.AppFilterManager;
import com.vanilla.screentimeforsleepy.util.AppLogger;
import com.vanilla.screentimeforsleepy.util.AppUsageSyncInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AppUsageTracker {

    private static final String TAG = "AppUsageTracker";
    private static final String ICON_DIR = "app_icons";
    
    private final Context context;
    private final UsageStatsManager usageStatsManager;
    private final PackageManager packageManager;

    public AppUsageTracker(Context context) {
        this.context = context;
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.packageManager = context.getPackageManager();
    }

    // 获取今日应用使用时间
    public Map<String, AppUsageSyncInfo> getTodayAppUsage(boolean hideSystemApps) {
        Map<String, AppUsageSyncInfo> appUsageMap = new TreeMap<>();
        
        // 获取AppFilterManager实例
        AppFilterManager filterManager = new AppFilterManager(context);
        
        // 获取今天的开始时间
        long startTime = getTodayStartTime();
        long endTime = System.currentTimeMillis();
        
        // 获取使用统计信息
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
        );
        
        if (usageStatsList != null) {
            for (UsageStats usageStats : usageStatsList) {
                try {
                    // 获取应用信息
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(
                            usageStats.getPackageName(),
                            PackageManager.GET_META_DATA
                    );
                    
                    // 检查是否为系统应用，如果需要隐藏则跳过
                    if (hideSystemApps && isSystemApp(appInfo)) {
                        continue;
                    }
                    
                    // 跳过使用时长为0的应用
                    if (usageStats.getTotalTimeInForeground() <= 0) {
                        continue;
                    }
                    
                    // 获取应用名称和包名
                    String appName = appInfo.loadLabel(packageManager).toString();
                    String packageName = appInfo.packageName;
                    
                    // 根据黑白名单过滤应用
                    if (!filterManager.shouldIncludeApp(packageName)) {
                        continue;
                    }
                    
                    // 获取应用图标并保存
                    String iconFileName = saveAppIcon(appInfo);
                    
                    // 计算总使用时间（毫秒转换为秒）
                    long totalTimeSeconds = usageStats.getTotalTimeInForeground() / 1000;
                    
                    // 添加到映射中
                    AppUsageSyncInfo appUsageInfo = new AppUsageSyncInfo();
                    appUsageInfo.setIcon(iconFileName);
                    appUsageInfo.setTotalTime((int) totalTimeSeconds);
                    
                    appUsageMap.put(appName, appUsageInfo);
                    
                } catch (PackageManager.NameNotFoundException e) {
                    AppLogger.e(TAG, "Package not found: " + usageStats.getPackageName(), e);
                }
            }
        }
        
        return appUsageMap;
    }
    
    // 检查是否为系统应用
    private boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
    
    // 获取今日应用使用时间（默认隐藏系统应用）
    public Map<String, AppUsageSyncInfo> getTodayAppUsage() {
        return getTodayAppUsage(true);
    }

    // 保存应用图标到本地
    private String saveAppIcon(ApplicationInfo appInfo) {
        try {
            // 获取应用图标
            Drawable icon = appInfo.loadIcon(packageManager);
            
            // 创建图标目录
            File iconDir = new File(getIconDirPath());
            if (!iconDir.exists()) {
                iconDir.mkdirs();
            }
            
            // 生成图标文件名
            String fileName = appInfo.packageName.replace('.', '_') + ".png";
            File iconFile = new File(iconDir, fileName);
            
            // 如果图标文件不存在，则保存
            if (!iconFile.exists()) {
                // 将Drawable转换为Bitmap
                Bitmap bitmap = drawableToBitmap(icon);
                
                // 保存Bitmap到文件
                FileOutputStream fos = new FileOutputStream(iconFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            }
            
            return fileName;
            
        } catch (IOException e) {
            AppLogger.e(TAG, "Error saving app icon", e);
            return "";
        }
    }

    // 获取图标目录路径
    private String getIconDirPath() {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + ICON_DIR;
    }

    // 将Drawable转换为Bitmap
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        
        int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 100;
        int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 100;
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        
        return bitmap;
    }

    // 获取今天的开始时间
    private long getTodayStartTime() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // 获取图标文件
    public File getIconFile(String fileName) {
        return new File(getIconDirPath(), fileName);
    }

    // 获取当前日期字符串
    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    // 获取当前时间字符串
    public String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
}
