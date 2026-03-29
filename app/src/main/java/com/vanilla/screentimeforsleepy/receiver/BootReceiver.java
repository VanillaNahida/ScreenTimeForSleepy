package com.vanilla.screentimeforsleepy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // 启动同步服务
            Intent syncServiceIntent = new Intent(context, ScreenTimeSyncService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(syncServiceIntent);
            } else {
                context.startService(syncServiceIntent);
            }
        }
    }
}
