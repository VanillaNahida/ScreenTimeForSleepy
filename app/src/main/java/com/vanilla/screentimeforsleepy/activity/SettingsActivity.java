package com.vanilla.screentimeforsleepy;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton btnBack, btnToggleApiKey;
    private EditText etApiUrl, etApiKey, etDeviceId, etDisplayName, etCheckInterval;
    private Button btnSave, btnAutoStart, btnBatteryOptimization;
    private TextView tvNotification, tvAccessibility;
    private Switch swNotification, swAccessibility, swHideSystemApps;
    private SharedPreferences sharedPreferences;
    private boolean isApiKeyVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences("app_config", MODE_PRIVATE);

        // 初始化控件
        initViews();

        // 设置点击事件
        setupListeners();

        // 加载已保存的配置
        loadSavedConfig();
        
        // 检查权限状态
        checkPermissions();
    }

    // 初始化控件
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etApiUrl = findViewById(R.id.et_api_url);
        etApiKey = findViewById(R.id.et_api_key);
        etDeviceId = findViewById(R.id.et_device_id);
        etDisplayName = findViewById(R.id.et_display_name);
        etCheckInterval = findViewById(R.id.et_check_interval);
        btnSave = findViewById(R.id.btn_save);
        btnAutoStart = findViewById(R.id.btn_auto_start);
        btnBatteryOptimization = findViewById(R.id.btn_battery_optimization);
        btnToggleApiKey = findViewById(R.id.btn_toggle_api_key);
        tvNotification = findViewById(R.id.tv_notification);
        tvAccessibility = findViewById(R.id.tv_accessibility);
        swNotification = findViewById(R.id.sw_notification);
        swAccessibility = findViewById(R.id.sw_accessibility);
        swHideSystemApps = findViewById(R.id.sw_hide_system_apps);
    }

    // 设置点击事件
    private void setupListeners() {
        // 返回按钮点击事件
        btnBack.setOnClickListener(v -> {
            finish(); // 结束当前Activity，返回上一个Activity
        });

        // 保存按钮点击事件
        btnSave.setOnClickListener(v -> {
            saveConfig(); // 保存配置
            finish(); // 结束当前Activity，返回上一个Activity
        });

        // 自启动权限按钮
        btnAutoStart.setOnClickListener(v -> {
            showPermissionDialog("自启动权限", "请在应用信息页面开启自启动权限，以确保应用在设备重启后能正常运行。", () -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
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
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } else {
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
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
    }

    // 加载已保存的配置
    private void loadSavedConfig() {
        etApiUrl.setText(sharedPreferences.getString("api_url", ""));
        etApiKey.setText(sharedPreferences.getString("api_key", ""));
        etDeviceId.setText(sharedPreferences.getString("device_id", ""));
        etDisplayName.setText(sharedPreferences.getString("display_name", ""));
        etCheckInterval.setText(String.valueOf(sharedPreferences.getInt("check_interval", 60)));
        swHideSystemApps.setChecked(sharedPreferences.getBoolean("hide_system_apps", true));
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
        
        editor.apply(); // 应用保存
    }

    // 检查权限状态
    private void checkPermissions() {
        // 检查通知权限
        boolean hasNotificationPermission = NotificationManagerCompat.from(this).areNotificationsEnabled();
        updateNotificationStatus(hasNotificationPermission);
        
        // 检查无障碍权限
        boolean hasAccessibilityPermission = isAccessibilityServiceEnabled();
        updateAccessibilityStatus(hasAccessibilityPermission);
        
        // 如果有通知权限，启动前台服务显示常驻通知
        if (hasNotificationPermission) {
            ScreenTimeService.startService(this);
        } else {
            ScreenTimeService.stopService(this);
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
        String serviceName = getPackageName() + ".ScreenTimeAccessibilityService";
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            return settingValue != null && settingValue.contains(serviceName);
        }
        return false;
    }

    // 显示权限申请对话框
    private void showPermissionDialog(String title, String message, Runnable onConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
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
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            // 跳转到通知设置页面
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
            }
            startActivity(intent);
            Toast.makeText(this, "请开启通知权限以显示常驻通知", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "通知权限已开启", Toast.LENGTH_SHORT).show();
        }
        // 不再手动重置开关状态，让onResume中的checkPermissions()方法来更新
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 当用户从设置页面返回时，重新检查权限状态
        checkPermissions();
    }
}