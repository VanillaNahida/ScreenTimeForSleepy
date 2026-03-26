package com.vanilla.screentimeforsleepy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HomeFragment extends Fragment {

    private RecyclerView rvAppUsage;
    private TextView tvPermissionNeeded;
    private Button btnGrantPermission;
    private Button btnRefresh;
    private Button btnSyncNow;
    private Switch switchHideSystemApps;
    private UsageStatsHelper usageStatsHelper;
    private boolean hideSystemApps = true; // 默认隐藏系统应用

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 初始化UI组件
        rvAppUsage = view.findViewById(R.id.rv_app_usage);
        tvPermissionNeeded = view.findViewById(R.id.tv_permission_needed);
        btnGrantPermission = view.findViewById(R.id.btn_grant_permission);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnSyncNow = view.findViewById(R.id.btn_sync_now);
        switchHideSystemApps = view.findViewById(R.id.switch_hide_system_apps);

        // 初始化UsageStatsHelper
        usageStatsHelper = new UsageStatsHelper(getActivity());

        // 设置权限请求按钮点击事件
        btnGrantPermission.setOnClickListener(v -> {
            PermissionUtils.requestUsageStatsPermission(getActivity());
        });

        // 设置刷新按钮点击事件
        btnRefresh.setOnClickListener(v -> {
            checkPermissionAndLoadData();
        });

        // 设置立即同步按钮点击事件
        btnSyncNow.setOnClickListener(v -> {
            ScreenTimeSyncService.startService(getActivity());
            Toast.makeText(getActivity(), "开始执行同步任务。", Toast.LENGTH_SHORT).show();
        });

        // 设置开关状态变化监听器
        switchHideSystemApps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hideSystemApps = isChecked;
            // 保存设置
            SharedPreferences prefs = getActivity().getSharedPreferences("app_config", getActivity().MODE_PRIVATE);
            prefs.edit().putBoolean("hide_system_apps", isChecked).apply();
            checkPermissionAndLoadData();
        });

        // 从设置中读取开关状态
        SharedPreferences prefs = getActivity().getSharedPreferences("app_config", getActivity().MODE_PRIVATE);
        hideSystemApps = prefs.getBoolean("hide_system_apps", true);
        switchHideSystemApps.setChecked(hideSystemApps);

        // 检查权限并加载数据
        checkPermissionAndLoadData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 应用主题设置
        ThemeManager themeManager = new ThemeManager(getActivity());
        themeManager.applyTheme();
        // 在恢复时重新检查权限并加载数据
        checkPermissionAndLoadData();
    }

    /**
     * 检查权限并加载数据
     */
    private void checkPermissionAndLoadData() {
        if (PermissionUtils.checkUsageStatsPermission(getActivity())) {
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
        java.util.List<AppUsageInfo> appUsageInfoList = usageStatsHelper.getTodayUsageStats(hideSystemApps);

        // 计算最大使用时长
        long maxUsageTime = UsageStatsHelper.getMaxUsageTime(appUsageInfoList);

        // 初始化RecyclerView
        AppUsageAdapter adapter = new AppUsageAdapter(appUsageInfoList, maxUsageTime);
        // 设置应用过滤变化监听器
        adapter.setOnAppFilterChangeListener(() -> {
            // 当应用被添加到黑白名单后，刷新应用列表
            checkPermissionAndLoadData();
        });
        rvAppUsage.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvAppUsage.setAdapter(adapter);
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
