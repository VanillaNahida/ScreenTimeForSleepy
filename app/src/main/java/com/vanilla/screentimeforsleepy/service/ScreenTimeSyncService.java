package com.vanilla.screentimeforsleepy.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;

import androidx.core.app.NotificationCompat;

import com.vanilla.screentimeforsleepy.util.AppLogger;
import com.vanilla.screentimeforsleepy.util.AppUsageSyncInfo;
import com.vanilla.screentimeforsleepy.util.AppUsageTracker;
import com.vanilla.screentimeforsleepy.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenTimeSyncService extends Service {

    private static final String TAG = "ScreenTimeSyncService";
    private static final String CHANNEL_ID = "screen_time_sync_channel";
    private static final int NOTIFICATION_ID = 2;
    
    private ExecutorService executorService;
    private ExecutorService networkExecutor;
    private Handler handler;
    private SharedPreferences sharedPreferences;
    private AppUsageTracker appUsageTracker;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化
        executorService = Executors.newSingleThreadExecutor();
        networkExecutor = Executors.newFixedThreadPool(3);
        handler = new Handler(Looper.getMainLooper());
        sharedPreferences = getSharedPreferences("app_config", MODE_PRIVATE);
        appUsageTracker = new AppUsageTracker(this);
        
        // 创建通知通道
        createNotificationChannel();
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification("服务启动中..."));
        
        AppLogger.i(TAG, "服务启动");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            // 立即执行一次同步（在主线程中启动，避免等待executorService）
            new Thread(() -> syncScreenTimeData()).start();
            // 开始同步任务
            startSyncTask();
        } else {
            // 服务已在运行，立即执行一次同步（使用独立线程）
            new Thread(() -> syncScreenTimeData()).start();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        executorService.shutdown();
        networkExecutor.shutdown();
        
        // 服务被销毁时尝试重启
        Intent restartIntent = new Intent(getApplicationContext(), ScreenTimeSyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
        AppLogger.d(TAG, "服务已销毁，尝试重启");
    }

    // 启动同步任务
    private void startSyncTask() {
        executorService.execute(() -> {
            while (isRunning) {
                try {
                    // 执行同步
                    syncScreenTimeData();
                    
                    // 获取检查间隔（默认60秒）
                    int checkInterval = sharedPreferences.getInt("check_interval", 60);
                    
                    // 等待指定时间后再次同步
                    Thread.sleep(checkInterval * 1000);
                    
                } catch (InterruptedException e) {
                    AppLogger.e(TAG, "同步任务被中断", e);
                    break;
                }
            }
        });
    }

    // 同步屏幕使用时间数据
    private void syncScreenTimeData() {
        try {
            // 获取服务器配置
            String serverUrl = sharedPreferences.getString("api_url", "");
            String apiKey = sharedPreferences.getString("api_key", "");
            String deviceId = sharedPreferences.getString("device_id", "");
            String deviceName = sharedPreferences.getString("display_name", "");
            boolean hideSystemApps = sharedPreferences.getBoolean("hide_system_apps", true);
            
            // 检查配置是否完整
            if (serverUrl.isEmpty() || apiKey.isEmpty() || deviceId.isEmpty()) {
                updateNotification("配置不完整，无法同步");
                return;
            }
            
            // 获取应用使用时间数据
            Map<String, AppUsageSyncInfo> appUsageMap = appUsageTracker.getTodayAppUsage(hideSystemApps);
            
            // 构建JSON数据
            JSONObject jsonData = buildScreenTimeJson(deviceId, deviceName, appUsageMap);
            
            // 异步发送数据到服务器
            networkExecutor.execute(() -> {
                long startTime = System.currentTimeMillis();
                boolean success = sendScreenTimeData(serverUrl, apiKey, jsonData);
                long duration = System.currentTimeMillis() - startTime;
                
                // 更新通知
                handler.post(() -> {
                    String syncTime = appUsageTracker.getCurrentTime();
                    if (success) {
                        String notificationText = "同步成功，应用数: " + appUsageMap.size() + " | " + syncTime;
                        updateNotification(notificationText);
                        // 记录上报状态报告
                        Map<String, String> params = new HashMap<>();
                        params.put("应用数量", String.valueOf(appUsageMap.size()));
                        params.put("同步时间", syncTime);
                        params.put("耗时", duration + "ms");
                        AppLogger.logUploadStatus("上报状态报告", params);
                    } else {
                        String notificationText = "同步失败 | " + syncTime;
                        updateNotification(notificationText);
                        // 记录上报失败报告
                        Map<String, String> params = new HashMap<>();
                        params.put("错误码", "500");
                        params.put("同步时间", syncTime);
                        params.put("耗时", duration + "ms");
                        AppLogger.logUploadError("上报失败报告", params);
                    }
                });
            });
            
            // 异步上传应用图标
            networkExecutor.execute(() -> {
                uploadAppIcons(serverUrl, apiKey, deviceId, appUsageMap);
            });
            
        } catch (Exception e) {
            AppLogger.e(TAG, "同步屏幕使用时间数据出错", e);
            updateNotification("同步出错: " + e.getMessage());
        }
    }

    // 构建屏幕使用时间JSON数据
    private JSONObject buildScreenTimeJson(String deviceId, String deviceName, Map<String, AppUsageSyncInfo> appUsageMap) throws JSONException {
        JSONObject jsonData = new JSONObject();
        jsonData.put("device-id", deviceId);
        jsonData.put("device-name", deviceName);
        jsonData.put("date", appUsageTracker.getCurrentDate());
        jsonData.put("update-time", appUsageTracker.getCurrentTime());
        
        JSONObject screenUsageTime = new JSONObject();
        JSONObject appUsage = new JSONObject();
        
        // 添加应用使用时间数据
        for (Map.Entry<String, AppUsageSyncInfo> entry : appUsageMap.entrySet()) {
            JSONObject appInfo = new JSONObject();
            appInfo.put("icon", entry.getValue().getIcon());
            appInfo.put("total_time", entry.getValue().getTotalTime());
            appUsage.put(entry.getKey(), appInfo);
        }
        
        screenUsageTime.put("app_usage", appUsage);
        screenUsageTime.put("website_usage", new JSONObject()); // 暂不支持网站使用时间
        jsonData.put("screen_usage_time", screenUsageTime);
        
        return jsonData;
    }

    // 发送屏幕使用时间数据到服务器
    private boolean sendScreenTimeData(String serverUrl, String apiKey, JSONObject jsonData) {
        try {
            // 构建完整的URL
            String urlString = serverUrl + "/plugin/screen_usage_time/usage";
            URL url = new URL(urlString);
            
            // 创建HTTP连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Sleepy-Secret", apiKey);
            connection.setDoOutput(true);
            
            // 发送数据
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(jsonData.toString().getBytes());
            outputStream.flush();
            outputStream.close();
            
            // 获取响应
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                AppLogger.d(TAG, "屏幕使用时间数据已发送，响应码: " + responseCode);
            } else {
                // 记录非200响应码的错误信息
                Map<String, String> params = new HashMap<>();
                params.put("错误码", String.valueOf(responseCode));
                AppLogger.logUploadError("服务器返回错误", params);
            }
            return responseCode == HttpURLConnection.HTTP_OK;
            
        } catch (Exception e) {
            // 记录网络异常
            Map<String, String> params = new HashMap<>();
            params.put("错误类型", e.getClass().getSimpleName());
            params.put("错误消息", e.getMessage() != null ? e.getMessage() : "未知错误");
            AppLogger.logUploadError("发送数据异常", params);
            AppLogger.e(TAG, "发送屏幕使用时间数据出错", e);
            return false;
        }
    }

    // 上传应用图标
    private void uploadAppIcons(String serverUrl, String apiKey, String deviceId, Map<String, AppUsageSyncInfo> appUsageMap) {
        for (Map.Entry<String, AppUsageSyncInfo> entry : appUsageMap.entrySet()) {
            String iconFileName = entry.getValue().getIcon();
            if (!iconFileName.isEmpty()) {
                uploadAppIcon(serverUrl, apiKey, deviceId, iconFileName);
            }
        }
    }

    // 上传单个应用图标
    private void uploadAppIcon(String serverUrl, String apiKey, String deviceId, String iconFileName) {
        try {
            // 获取图标文件
            File iconFile = appUsageTracker.getIconFile(iconFileName);
            if (!iconFile.exists()) {
                return;
            }
            
            // 构建完整的URL
            String urlString = serverUrl + "/plugin/screen_usage_time/icons";
            URL url = new URL(urlString);
            
            // 创建HTTP连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "image/png");
            connection.setRequestProperty("Sleepy-Secret", apiKey);
            connection.setRequestProperty("filename", Base64.encodeToString(iconFileName.getBytes("UTF-8"), Base64.NO_WRAP));
            connection.setRequestProperty("x-device-id", deviceId);
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setFixedLengthStreamingMode((int) iconFile.length());
            
            // 读取图标文件并发送
            FileInputStream fis = new FileInputStream(iconFile);
            OutputStream outputStream = connection.getOutputStream();
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.flush();
            outputStream.close();
            fis.close();
            
            // 获取响应
            int responseCode = connection.getResponseCode();
            AppLogger.d(TAG, "图标已上传: " + iconFileName + ", 响应码: " + responseCode);
            connection.disconnect();
            
        } catch (Exception e) {
            AppLogger.e(TAG, "上传应用图标出错: " + iconFileName, e);
        }
    }

    // 创建通知通道
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "屏幕使用时间同步",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于显示屏幕使用时间同步服务的运行状态");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // 创建通知
    private Notification createNotification(String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("屏幕使用时间同步")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        return builder.build();
    }

    // 更新通知
    private void updateNotification(String content) {
        handler.post(() -> {
            Notification notification = createNotification(content);
            startForeground(NOTIFICATION_ID, notification);
        });
    }

    // 启动服务的静态方法
    public static void startService(Context context) {
        Intent intent = new Intent(context, ScreenTimeSyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    // 停止服务的静态方法
    public static void stopService(Context context) {
        Intent intent = new Intent(context, ScreenTimeSyncService.class);
        context.stopService(intent);
    }
}
