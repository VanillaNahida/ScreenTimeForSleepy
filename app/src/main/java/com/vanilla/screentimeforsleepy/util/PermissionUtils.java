package com.vanilla.screentimeforsleepy;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.app.AppOpsManager;
import android.util.Log;

public class PermissionUtils {
    private static final String TAG = "PermissionUtils";
    
    /**
     * 检查是否有使用情况访问权限
     */
    public static boolean checkUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    
    /**
     * 请求使用情况访问权限
     */
    public static void requestUsageStatsPermission(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }
}