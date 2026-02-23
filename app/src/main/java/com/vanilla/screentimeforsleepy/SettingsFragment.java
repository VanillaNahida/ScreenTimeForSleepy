package com.vanilla.screentimeforsleepy;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import java.util.List;

public class SettingsFragment extends Fragment {

    private EditText etApiUrl, etApiKey, etDeviceId, etDisplayName, etCheckInterval;
    private Button btnSave, btnAutoStart, btnBatteryOptimization, btnManageBlacklist, btnManageWhitelist;
    private TextView tvNotification, tvAccessibility;
    private Switch swNotification, swAccessibility, swHideSystemApps, swBlacklist, swWhitelist;
    private ImageButton btnToggleApiKey;
    private SharedPreferences sharedPreferences;
    private boolean isApiKeyVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 初始化SharedPreferences
        sharedPreferences = getActivity().getSharedPreferences("app_config", getActivity().MODE_PRIVATE);

        // 初始化控件
        initViews(view);

        // 设置点击事件
        setupListeners();

        // 加载已保存的配置
        loadSavedConfig();

        // 检查权限状态
        checkPermissions();
        
        // 更新黑白名单按钮上的应用数量显示
        updateFilterButtons();

        return view;
    }

    // 初始化控件
    private void initViews(View view) {
        etApiUrl = view.findViewById(R.id.et_api_url);
        etApiKey = view.findViewById(R.id.et_api_key);
        etDeviceId = view.findViewById(R.id.et_device_id);
        etDisplayName = view.findViewById(R.id.et_display_name);
        etCheckInterval = view.findViewById(R.id.et_check_interval);
        btnSave = view.findViewById(R.id.btn_save);
        btnAutoStart = view.findViewById(R.id.btn_auto_start);
        btnBatteryOptimization = view.findViewById(R.id.btn_battery_optimization);
        btnManageBlacklist = view.findViewById(R.id.btn_manage_blacklist);
        btnManageWhitelist = view.findViewById(R.id.btn_manage_whitelist);
        btnToggleApiKey = view.findViewById(R.id.btn_toggle_api_key);
        tvNotification = view.findViewById(R.id.tv_notification);
        tvAccessibility = view.findViewById(R.id.tv_accessibility);
        swNotification = view.findViewById(R.id.sw_notification);
        swAccessibility = view.findViewById(R.id.sw_accessibility);
        swHideSystemApps = view.findViewById(R.id.sw_hide_system_apps);
        swBlacklist = view.findViewById(R.id.sw_blacklist);
        swWhitelist = view.findViewById(R.id.sw_whitelist);
    }

    // 设置点击事件
    private void setupListeners() {
        // 保存按钮点击事件
        btnSave.setOnClickListener(v -> {
            saveConfig(); // 保存配置
            Toast.makeText(getActivity(), "配置已保存", Toast.LENGTH_SHORT).show();
        });

        // 自启动权限按钮
        btnAutoStart.setOnClickListener(v -> {
            showPermissionDialog("自启动权限", "请在应用信息页面开启自启动权限，以确保应用在设备重启后能正常运行。", () -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            });
        });

        // 忽略电池优化按钮
        btnBatteryOptimization.setOnClickListener(v -> {
            showPermissionDialog("忽略电池优化", "请将应用添加到忽略电池优化列表，以确保应用在后台能正常运行。", () -> {
                Intent intent = new Intent();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                    startActivity(intent);
                } else {
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getActivity().getPackageName(), null));
                    startActivity(intent);
                }
            });
        });

        // 无障碍权限开关
        swAccessibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 先检查无障碍服务是否已启用
                if (!isAccessibilityServiceEnabled()) {
                    showPermissionDialog("无障碍权限", "请在无障碍设置页面开启本应用的无障碍服务，以确保应用能正常监控屏幕使用时间。", () -> {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivity(intent);
                    });
                }
            }
        });

        // API密钥显示/隐藏按钮
        btnToggleApiKey.setOnClickListener(v -> {
            toggleApiKeyVisibility();
        });

        // 常驻通知权限开关
        swNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestNotificationPermission();
            }
        });

        // 黑名单管理按钮
        btnManageBlacklist.setOnClickListener(v -> {
            if (checkAppListPermission()) {
                Intent intent = new Intent(getActivity(), AppFilterActivity.class);
                intent.putExtra("filter_type", "blacklist");
                startActivity(intent);
            }
        });

        // 白名单管理按钮
        btnManageWhitelist.setOnClickListener(v -> {
            if (checkAppListPermission()) {
                Intent intent = new Intent(getActivity(), AppFilterActivity.class);
                intent.putExtra("filter_type", "whitelist");
                startActivity(intent);
            }
        });

        // 黑名单开关
        swBlacklist.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 启用黑名单时，自动禁用白名单
                swWhitelist.setChecked(false);
            }
        });

        // 白名单开关
        swWhitelist.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 启用白名单时，自动禁用黑名单
                swBlacklist.setChecked(false);
            }
        });
    }

    // 加载已保存的配置
    private void loadSavedConfig() {
        etApiUrl.setText(sharedPreferences.getString("api_url", ""));
        etApiKey.setText(sharedPreferences.getString("api_key", ""));
        etDeviceId.setText(sharedPreferences.getString("device_id", ""));
        etDisplayName.setText(sharedPreferences.getString("display_name", ""));
        etCheckInterval.setText(String.valueOf(sharedPreferences.getInt("check_interval", 60)));
        swHideSystemApps.setChecked(sharedPreferences.getBoolean("hide_system_apps", true));
        
        // 加载黑白名单启用状态
        AppFilterManager filterManager = new AppFilterManager(getActivity());
        swBlacklist.setChecked(filterManager.isBlacklistEnabled());
        swWhitelist.setChecked(filterManager.isWhitelistEnabled());
    }

    // 保存配置
    private void saveConfig() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("api_url", etApiUrl.getText().toString());
        editor.putString("api_key", etApiKey.getText().toString());
        editor.putString("device_id", etDeviceId.getText().toString());
        editor.putString("display_name", etDisplayName.getText().toString());

        // 处理检查间隔，默认值为60秒
        String intervalStr = etCheckInterval.getText().toString();
        int interval = 60;
        if (!intervalStr.isEmpty()) {
            try {
                interval = Integer.parseInt(intervalStr);
            } catch (NumberFormatException e) {
                interval = 60;
            }
        }
        editor.putInt("check_interval", interval);
        editor.putBoolean("hide_system_apps", swHideSystemApps.isChecked());
        editor.putBoolean("enable_blacklist", swBlacklist.isChecked());
        editor.putBoolean("enable_whitelist", swWhitelist.isChecked());

        editor.apply(); // 应用保存

        // 更新 AppFilterManager 中的黑白名单启用状态
        AppFilterManager filterManager = new AppFilterManager(getActivity());
        filterManager.setBlacklistEnabled(swBlacklist.isChecked());
        filterManager.setWhitelistEnabled(swWhitelist.isChecked());
    }

    // 检查权限状态
    private void checkPermissions() {
        // 检查通知权限
        boolean hasNotificationPermission = NotificationManagerCompat.from(getActivity()).areNotificationsEnabled();
        updateNotificationStatus(hasNotificationPermission);

        // 检查无障碍权限
        boolean hasAccessibilityPermission = isAccessibilityServiceEnabled();
        updateAccessibilityStatus(hasAccessibilityPermission);

        // 如果有通知权限，启动前台服务显示常驻通知
        if (hasNotificationPermission) {
            ScreenTimeService.startService(getActivity());
        } else {
            ScreenTimeService.stopService(getActivity());
        }
    }
    
    // 更新黑白名单按钮上的应用数量显示
    private void updateFilterButtons() {
        AppFilterManager filterManager = new AppFilterManager(getActivity());
        int blacklistCount = filterManager.getBlacklistCount();
        int whitelistCount = filterManager.getWhitelistCount();
        
        // 更新黑名单按钮
        if (blacklistCount > 0) {
            btnManageBlacklist.setText("管理黑名单(" + blacklistCount + ")");
        } else {
            btnManageBlacklist.setText("管理黑名单");
        }
        
        // 更新白名单按钮
        if (whitelistCount > 0) {
            btnManageWhitelist.setText("管理白名单(" + whitelistCount + ")");
        } else {
            btnManageWhitelist.setText("管理白名单");
        }
    }

    // 更新通知权限状态显示
    private void updateNotificationStatus(boolean hasPermission) {
        if (hasPermission) {
            tvNotification.setText("常驻通知权限: 已开启");
            swNotification.setChecked(true);
        } else {
            tvNotification.setText("常驻通知权限: 未开启");
            swNotification.setChecked(false);
        }
    }

    // 更新无障碍权限状态显示
    private void updateAccessibilityStatus(boolean hasPermission) {
        if (hasPermission) {
            tvAccessibility.setText("无障碍权限: 已开启");
            swAccessibility.setChecked(true);
        } else {
            tvAccessibility.setText("无障碍权限: 未开启");
            swAccessibility.setChecked(false);
        }
    }

    // 检查无障碍服务是否已启用
    private boolean isAccessibilityServiceEnabled() {
        String serviceName = getActivity().getPackageName() + ".ScreenTimeAccessibilityService";
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = android.provider.Settings.Secure.getInt(
                    getActivity().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (android.provider.Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (accessibilityEnabled == 1) {
            String settingValue = android.provider.Settings.Secure.getString(
                    getActivity().getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            return settingValue != null && settingValue.contains(serviceName);
        }
        return false;
    }

    // 显示权限申请对话框
    private void showPermissionDialog(String title, String message, Runnable onConfirm) {
        new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("前往设置", (dialog, which) -> {
                    onConfirm.run();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    // 切换API密钥显示/隐藏状态
    private void toggleApiKeyVisibility() {
        isApiKeyVisible = !isApiKeyVisible;

        // 保存当前光标位置
        int selectionStart = etApiKey.getSelectionStart();
        int selectionEnd = etApiKey.getSelectionEnd();

        if (isApiKeyVisible) {
            // 显示API密钥
            etApiKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnToggleApiKey.setImageResource(R.drawable.ic_visibility);
        } else {
            // 隐藏API密钥
            etApiKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleApiKey.setImageResource(R.drawable.ic_visibility_off);
        }

        // 恢复光标位置
        etApiKey.setSelection(selectionStart, selectionEnd);
    }

    // 申请常驻通知权限
    private void requestNotificationPermission() {
        // 检查是否已获得通知权限
        if (!NotificationManagerCompat.from(getActivity()).areNotificationsEnabled()) {
            // 跳转到通知设置页面
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
            } else {
                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                intent.setData(uri);
            }
            startActivity(intent);
            Toast.makeText(getActivity(), "请开启通知权限以显示常驻通知", Toast.LENGTH_LONG).show();
        } else {
            // 已获得通知权限，启动前台服务显示常驻通知
        }
    }

    // 检查应用列表权限
    private boolean checkAppListPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上，需要 QUERY_ALL_PACKAGES 权限
            // 检查是否已经有该权限
            boolean hasPermission = checkQueryAllPackagesPermission();
            if (!hasPermission) {
                // 没有权限，提示用户跳转到设置页面
                showPermissionDialog("应用列表权限", "需要获取应用列表权限以管理黑白名单", () -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                });
            }
            return true; // 即使没有权限，也让用户尝试，因为权限可能已经在设置中授予
        } else {
            // Android 10 及以下，不需要特殊权限
            return true;
        }
    }

    // 检查是否有 QUERY_ALL_PACKAGES 权限
    private boolean checkQueryAllPackagesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 在 Android 11 及以上，QUERY_ALL_PACKAGES 是一个敏感权限
            // 我们可以通过尝试获取应用列表来间接检查是否有权限
            try {
                PackageManager packageManager = getActivity().getPackageManager();
                List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                // 如果能获取到应用列表，说明有权限
                return apps != null && !apps.isEmpty();
            } catch (Exception e) {
                // 如果获取失败，说明没有权限
                return false;
            }
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 当用户从设置页面返回时，重新检查权限状态
        checkPermissions();
        // 更新黑白名单按钮上的应用数量显示
        updateFilterButtons();
    }
}
