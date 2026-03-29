package com.vanilla.screentimeforsleepy.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.vanilla.screentimeforsleepy.R;

public class ScreenTimeService extends Service {

    private static final String CHANNEL_ID = "screen_time_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 创建常驻通知
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 创建通知通道
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "屏幕使用时间统计",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于显示屏幕使用时间统计服务的运行状态");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // 创建通知
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("屏幕使用时间统计")
                .setContentText("屏幕使用时间统计正在运行中")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true); // 设置为常驻通知

        return builder.build();
    }

    // 启动服务的静态方法
    public static void startService(Context context) {
        try {
            Intent intent = new Intent(context, ScreenTimeService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 停止服务的静态方法
    public static void stopService(Context context) {
        try {
            Intent intent = new Intent(context, ScreenTimeService.class);
            context.stopService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
