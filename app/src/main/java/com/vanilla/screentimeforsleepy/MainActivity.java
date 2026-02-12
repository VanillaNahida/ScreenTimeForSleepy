package com.vanilla.screentimeforsleepy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

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
    private ImageButton btnSettings;
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
        btnSettings = findViewById(R.id.btn_settings);
        switchHideSystemApps = findViewById(R.id.switch_hide_system_apps);

        // 初始化UsageStatsHelper
        usageStatsHelper = new UsageStatsHelper(this);

        // 设置权限请求按钮点击事件
        btnGrantPermission.setOnClickListener(v -> {
            PermissionUtils.requestUsageStatsPermission(this);
        });

        // 设置刷新按钮点击事件
        btnRefresh.setOnClickListener(v -> {
            checkPermissionAndLoadData();
        });

        // 设置设置按钮点击事件
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // 设置开关状态变化监听器
        switchHideSystemApps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hideSystemApps = isChecked;
            checkPermissionAndLoadData();
        });

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