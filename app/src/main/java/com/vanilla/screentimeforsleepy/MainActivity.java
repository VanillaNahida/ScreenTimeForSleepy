package com.vanilla.screentimeforsleepy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvAppUsage;
    private TextView tvPermissionNeeded;
    private Button btnGrantPermission;
    private Button btnRefresh;
    private Button btnSyncNow;
    private ImageButton btnSettings;
    private ImageButton btnLogs;
    private Switch switchHideSystemApps;
    private UsageStatsHelper usageStatsHelper;
    private boolean hideSystemApps = true; // 默认隐藏系统应用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化UI组件
        rvAppUsage = findViewById(R.id.rv_app_usage);
        tvPermissionNeeded = findViewById(R.id.tv_permission_needed);
        btnGrantPermission = findViewById(R.id.btn_grant_permission);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnSyncNow = findViewById(R.id.btn_sync_now);
        btnSettings = findViewById(R.id.btn_settings);
        btnLogs = findViewById(R.id.btn_logs);
        switchHideSystemApps = findViewById(R.id.switch_hide_system_apps);

        // 初始化UsageStatsHelper
        usageStatsHelper = new UsageStatsHelper(this);
        
        // 启动屏幕使用时间同步服务
        AppLogger.i("MainActivity", "启动屏幕使用时间同步服务");
        ScreenTimeSyncService.startService(this);
        AppLogger.i("MainActivity", "应用启动成功");

        // 设置权限请求按钮点击事件
        btnGrantPermission.setOnClickListener(v -> {
            PermissionUtils.requestUsageStatsPermission(this);
        });

        // 设置刷新按钮点击事件
        btnRefresh.setOnClickListener(v -> {
            checkPermissionAndLoadData();
        });

        // 设置立即同步按钮点击事件
        btnSyncNow.setOnClickListener(v -> {
            ScreenTimeSyncService.startService(this);
            Toast.makeText(this, "开始执行同步任务。", Toast.LENGTH_SHORT).show();
        });

        // 设置日志按钮点击事件
        btnLogs.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LogsActivity.class);
            startActivity(intent);
        });

        // 设置设置按钮点击事件
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // 设置开关状态变化监听器
        switchHideSystemApps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hideSystemApps = isChecked;
            // 保存设置
            SharedPreferences prefs = getSharedPreferences("app_config", MODE_PRIVATE);
            prefs.edit().putBoolean("hide_system_apps", isChecked).apply();
            checkPermissionAndLoadData();
        });

        // 从设置中读取开关状态
        SharedPreferences prefs = getSharedPreferences("app_config", MODE_PRIVATE);
        hideSystemApps = prefs.getBoolean("hide_system_apps", true);
        switchHideSystemApps.setChecked(hideSystemApps);

        // 设置默认开关状态
        switchHideSystemApps.setChecked(hideSystemApps);

        // 检查权限并加载数据
        checkPermissionAndLoadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 在恢复时重新检查权限并加载数据
        checkPermissionAndLoadData();
    }

    /**
     * 检查权限并加载数据
     */
    private void checkPermissionAndLoadData() {
        if (PermissionUtils.checkUsageStatsPermission(this)) {
            // 有权限，显示应用列表
            showAppUsageList();
        } else {
            // 无权限，显示权限请求界面
            showPermissionRequestUI();
        }
    }

    /**
     * 显示应用使用列表
     */
    private void showAppUsageList() {
        // 隐藏权限请求界面
        tvPermissionNeeded.setVisibility(View.GONE);
        btnGrantPermission.setVisibility(View.GONE);
        rvAppUsage.setVisibility(View.VISIBLE);

        // 获取应用使用统计数据
        List<AppUsageInfo> appUsageInfoList = usageStatsHelper.getTodayUsageStats(hideSystemApps);

        // 计算最大使用时长
        long maxUsageTime = UsageStatsHelper.getMaxUsageTime(appUsageInfoList);

        // 初始化RecyclerView
        rvAppUsage.setLayoutManager(new LinearLayoutManager(this));
        rvAppUsage.setAdapter(new AppUsageAdapter(appUsageInfoList, maxUsageTime));
    }

    /**
     * 显示权限请求界面
     */
    private void showPermissionRequestUI() {
        // 隐藏应用列表
        rvAppUsage.setVisibility(View.GONE);
        // 显示权限请求界面
        tvPermissionNeeded.setVisibility(View.VISIBLE);
        btnGrantPermission.setVisibility(View.VISIBLE);
    }
}