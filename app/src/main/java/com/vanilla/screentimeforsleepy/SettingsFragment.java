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

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SettingsFragment extends Fragment {

    private EditText etApiUrl, etApiKey, etDeviceId, etDisplayName, etCheckInterval;
    private Button btnSave, btnAutoStart, btnBatteryOptimization, btnManageBlacklist, btnManageWhitelist, btnClearIconCache, btnPickBackground;
    private TextView tvNotification, tvAccessibility, tvBackgroundStatus, tvBackgroundOpacityValue, tvCardOpacityValue, tvLogsOpacityValue;
    private android.widget.SeekBar sbBackgroundOpacity, sbCardOpacity, sbLogsOpacity;
    private Switch swNotification, swAccessibility, swHideSystemApps, swBlacklist, swWhitelist, swHideInMultitask;
    private ImageButton btnToggleApiKey;
    private SharedPreferences sharedPreferences;
    private boolean isApiKeyVisible = false;
    private static final int REQUEST_PICK_BACKGROUND = 2;

    // 从XML获取主题色
    private int getThemeColor() {
        return android.graphics.Color.parseColor("#a1de93");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 初始化SharedPreferences
        sharedPreferences = getActivity().getSharedPreferences("app_config", getActivity().MODE_PRIVATE);

        // 初始化控件（不添加监听器）
        initViewsWithoutListeners(view);

        // 加载已保存的配置
        loadSavedConfig();
        
        // 立即应用主题颜色到所有Switch（在视图创建时调用，避免颜色突变）
        applyThemeToSwitchesImmediately();

        // 添加实时预览监听器
        addRealTimePreviewListeners();

        // 设置点击事件
        setupListeners();

        // 检查权限状态
        checkPermissions();
        
        // 更新黑白名单按钮上的应用数量显示
        updateFilterButtons();

        return view;
    }
    
    // 立即应用主题颜色到所有Switch（在视图创建时调用，避免颜色突变）
    private void applyThemeToSwitchesImmediately() {
        try {
            int color = getThemeColor();
            
            // 创建Switch轨道颜色状态列表
            android.content.res.ColorStateList trackColorStateList = createSwitchTrackColorStateList(color);
            
            // 应用到所有Switch
            swNotification.setTrackTintList(trackColorStateList);
            swAccessibility.setTrackTintList(trackColorStateList);
            swHideSystemApps.setTrackTintList(trackColorStateList);
            swHideInMultitask.setTrackTintList(trackColorStateList);
            swBlacklist.setTrackTintList(trackColorStateList);
            swWhitelist.setTrackTintList(trackColorStateList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 创建Switch背景条的颜色状态列表（只设置开启状态，其他保持默认）
    private android.content.res.ColorStateList createSwitchTrackColorStateList(int color) {
        // 动态获取默认颜色（根据当前主题）
        int defaultTrackColor = getDefaultTrackColor();
        int disabledTrackColor = getDisabledTrackColor();
        
        int[][] states = new int[][]{new int[]{-android.R.attr.state_enabled}, // 禁用状态
            new int[]{android.R.attr.state_checked},  // 开启状态（checked）
            new int[]{}                               // 默认状态（关闭）
        };
        int[] colors = new int[]{disabledTrackColor, // 禁用状态保持默认
            color,              // 开启状态使用主题色
            defaultTrackColor   // 关闭状态保持默认
        };
        return new android.content.res.ColorStateList(states, colors);
    }
    
    // 动态获取默认轨道颜色（根据当前主题）
    private int getDefaultTrackColor() {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.colorControlNormal, typedValue, true)) {
            return typedValue.data & 0xFFFFFF | 0x4D000000; // 添加30%透明度
        }
        int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            return android.graphics.Color.parseColor("#4DFFFFFF"); // 夜间模式：白色30%透明度
        }
        return android.graphics.Color.parseColor("#4D000000"); // 日间模式：黑色30%透明度
    }
    
    // 动态获取禁用状态轨道颜色
    private int getDisabledTrackColor() {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.colorControlNormal, typedValue, true)) {
            return typedValue.data & 0xFFFFFF | 0x1F000000; // 添加12%透明度
        }
        int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            return android.graphics.Color.parseColor("#1FFFFFFF"); // 夜间模式：白色12%透明度
        }
        return android.graphics.Color.parseColor("#1F000000"); // 日间模式：黑色12%透明度
    }

    // 初始化控件（不添加监听器）
    private void initViewsWithoutListeners(View view) {
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
        btnClearIconCache = view.findViewById(R.id.btn_clear_icon_cache);
        btnPickBackground = view.findViewById(R.id.btn_pick_background);
        btnToggleApiKey = view.findViewById(R.id.btn_toggle_api_key);
        tvNotification = view.findViewById(R.id.tv_notification);
        tvAccessibility = view.findViewById(R.id.tv_accessibility);
        tvBackgroundStatus = view.findViewById(R.id.tv_background_status);
        tvBackgroundOpacityValue = view.findViewById(R.id.tv_background_opacity_value);
        tvCardOpacityValue = view.findViewById(R.id.tv_card_opacity_value);
        tvLogsOpacityValue = view.findViewById(R.id.tv_logs_opacity_value);
        sbBackgroundOpacity = view.findViewById(R.id.sb_background_opacity);
        sbCardOpacity = view.findViewById(R.id.sb_card_opacity);
        sbLogsOpacity = view.findViewById(R.id.sb_logs_opacity);
        swNotification = view.findViewById(R.id.sw_notification);
        swAccessibility = view.findViewById(R.id.sw_accessibility);
        swHideSystemApps = view.findViewById(R.id.sw_hide_system_apps);
        swHideInMultitask = view.findViewById(R.id.sw_hide_in_multitask);
        swBlacklist = view.findViewById(R.id.sw_blacklist);
        swWhitelist = view.findViewById(R.id.sw_whitelist);
    }
    
    // 添加实时预览监听器
    private void addRealTimePreviewListeners() {
        // 背景不透明度SeekBar监听
        sbBackgroundOpacity.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                tvBackgroundOpacityValue.setText(progress + "%");
                previewTheme();
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        
        // 应用卡片不透明度SeekBar监听
        sbCardOpacity.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                tvCardOpacityValue.setText(progress + "%");
                previewTheme();
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        
        // 日志背景不透明度SeekBar监听
        sbLogsOpacity.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                tvLogsOpacityValue.setText(progress + "%");
                previewTheme();
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
    }
    
    // 预览主题
    private void previewTheme() {
        try {
            float backgroundOpacity = sbBackgroundOpacity.getProgress() / 100.0f;
            float cardOpacity = sbCardOpacity.getProgress() / 100.0f;
            float logsOpacity = sbLogsOpacity.getProgress() / 100.0f;
            
            // 临时保存主题设置到内存存储
            ThemeMemoryStorage memoryStorage = ThemeMemoryStorage.getInstance();
            memoryStorage.setBackgroundOpacity(backgroundOpacity);
            memoryStorage.setCardOpacity(cardOpacity);
            memoryStorage.setLogsOpacity(logsOpacity);
            
            // 应用主题预览到整个Activity
            ThemeManager themeManager = new ThemeManager(getActivity());
            themeManager.applyTheme();
            
            // 动态刷新当前Fragment中的按钮和Switch
            View rootView = getView();
            if (rootView != null) {
                themeManager.applyThemeToView(rootView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 设置点击事件
    private void setupListeners() {
        // 保存按钮点击事件
        btnSave.setOnClickListener(v -> {
            saveConfig(); // 保存配置
            Toast.makeText(getActivity(), "配置已保存~", Toast.LENGTH_SHORT).show();
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
        
        // 清除缓存的图标按钮
        btnClearIconCache.setOnClickListener(v -> {
            // 显示二次确认弹窗
            new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                .setTitle("确认清除")
                .setMessage("你确定要删除并更新缓存的图标吗？这样会重新生成最新的应用图标，仅建议在更换系统主题时首页应用图标未更新时使用。")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 清除缓存的图标
                    clearIconCache();
                    Toast.makeText(getActivity(), "🎉 缓存的图标已清除~", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
        });
        
        // 背景图片选择按钮
        btnPickBackground.setOnClickListener(v -> {
            // 使用标准DocumentUI选择图片
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_PICK_BACKGROUND);
        });
    }
    
    // 清除缓存的图标
    private void clearIconCache() {
        // 获取正确的图标缓存目录路径（与AppUsageTracker中一致）
        File iconCacheDir = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "app_icons");
        if (iconCacheDir.exists()) {
            // 递归删除缓存目录
            deleteDirectory(iconCacheDir);
        }
        
        // 重新获取所有已安装应用的图标
        refreshAppIcons();
    }
    
    // 重新获取首页显示的应用图标
    private void refreshAppIcons() {
        new Thread(() -> {
            try {
                // 获取hideSystemApps的设置值
                SharedPreferences prefs = getActivity().getSharedPreferences("app_config", getActivity().MODE_PRIVATE);
                boolean hideSystemApps = prefs.getBoolean("hide_system_apps", true);
                
                // 初始化UsageStatsHelper
                UsageStatsHelper usageStatsHelper = new UsageStatsHelper(getActivity());
                // 获取首页显示的应用列表
                List<AppUsageInfo> appUsageInfoList = usageStatsHelper.getTodayUsageStats(hideSystemApps);
                
                // 重新创建图标目录
                File iconDir = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "app_icons");
                if (!iconDir.exists()) {
                    iconDir.mkdirs();
                }
                
                // 遍历应用，重新获取图标
                for (AppUsageInfo appUsageInfo : appUsageInfoList) {
                    try {
                        // 获取PackageManager
                        PackageManager packageManager = getActivity().getPackageManager();
                        // 获取应用信息
                        ApplicationInfo appInfo = packageManager.getApplicationInfo(appUsageInfo.getPackageName(), 0);
                        // 获取应用图标
                        Drawable icon = appInfo.loadIcon(packageManager);
                        
                        // 生成图标文件名
                        String fileName = appInfo.packageName.replace('.', '_') + ".png";
                        File iconFile = new File(iconDir, fileName);
                        
                        // 保存图标到文件
                        Bitmap bitmap = drawableToBitmap(icon);
                        FileOutputStream fos = new FileOutputStream(iconFile);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        // 忽略单个应用的错误，继续处理其他应用
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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
    
    // 递归删除目录
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            return directory.delete();
        }
        return false;
    }
    
    // 处理Activity结果
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == REQUEST_PICK_BACKGROUND) {
                // 处理背景图片选择
                Uri uri = data.getData();
                if (uri != null) {
                    // 复制图片到应用数据目录
                    copyImageToAppData(uri);
                }
            }
        }
    }
    
    // 复制图片到应用数据目录
    private void copyImageToAppData(Uri uri) {
        try {
            // 获取输入流
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                // 创建应用数据目录
                File appDataDir = new File(getActivity().getExternalFilesDir(null), "backgrounds");
                if (!appDataDir.exists()) {
                    appDataDir.mkdirs();
                }
                
                // 创建目标文件
                String fileName = "background_" + System.currentTimeMillis() + ".jpg";
                File outputFile = new File(appDataDir, fileName);
                
                // 复制文件
                OutputStream outputStream = new FileOutputStream(outputFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();
                
                // 保存背景图片路径
                sharedPreferences.edit().putString("background_image", outputFile.getAbsolutePath()).apply();
                Toast.makeText(getActivity(), "背景图片已设置", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "设置背景图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 加载已保存的配置
    private void loadSavedConfig() {
        etApiUrl.setText(sharedPreferences.getString("api_url", ""));
        etApiKey.setText(sharedPreferences.getString("api_key", ""));
        etDeviceId.setText(sharedPreferences.getString("device_id", ""));
        etDisplayName.setText(sharedPreferences.getString("display_name", ""));
        etCheckInterval.setText(String.valueOf(sharedPreferences.getInt("check_interval", 60)));
        
        // 加载不透明度设置
        float backgroundOpacity = sharedPreferences.getFloat("background_opacity", 1.0f);
        float cardOpacity = sharedPreferences.getFloat("card_opacity", 0.8f);
        float logsOpacity = sharedPreferences.getFloat("logs_opacity", 0.8f);
        
        sbBackgroundOpacity.setProgress((int) (backgroundOpacity * 100));
        sbCardOpacity.setProgress((int) (cardOpacity * 100));
        sbLogsOpacity.setProgress((int) (logsOpacity * 100));
        
        tvBackgroundOpacityValue.setText((int) (backgroundOpacity * 100) + "%");
        tvCardOpacityValue.setText((int) (cardOpacity * 100) + "%");
        tvLogsOpacityValue.setText((int) (logsOpacity * 100) + "%");
        
        swHideSystemApps.setChecked(sharedPreferences.getBoolean("hide_system_apps", true));
        swHideInMultitask.setChecked(sharedPreferences.getBoolean("hide_in_multitask", false));
        
        // 加载背景图片状态
        String backgroundImage = sharedPreferences.getString("background_image", "");
        if (!backgroundImage.isEmpty()) {
            tvBackgroundStatus.setText("已设置");
        } else {
            tvBackgroundStatus.setText("未设置");
        }
        
        // 加载黑白名单启用状态
        AppFilterManager filterManager = new AppFilterManager(getActivity());
        swBlacklist.setChecked(filterManager.isBlacklistEnabled());
        swWhitelist.setChecked(filterManager.isWhitelistEnabled());
    }

    // 保存配置
    private void saveConfig() {
        // 检查多任务隐藏设置是否发生变化
        boolean oldHideInMultitask = sharedPreferences.getBoolean("hide_in_multitask", false);
        boolean newHideInMultitask = swHideInMultitask.isChecked();
        
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
        editor.putBoolean("hide_in_multitask", newHideInMultitask);
        editor.putBoolean("enable_blacklist", swBlacklist.isChecked());
        editor.putBoolean("enable_whitelist", swWhitelist.isChecked());
        
        // 保存主题设置
        float backgroundOpacity = sbBackgroundOpacity.getProgress() / 100.0f;
        float cardOpacity = sbCardOpacity.getProgress() / 100.0f;
        float logsOpacity = sbLogsOpacity.getProgress() / 100.0f;
        editor.putFloat("background_opacity", backgroundOpacity);
        editor.putFloat("card_opacity", cardOpacity);
        editor.putFloat("logs_opacity", logsOpacity);

        editor.apply(); // 应用保存

        // 更新 AppFilterManager 中的黑白名单启用状态
        AppFilterManager filterManager = new AppFilterManager(getActivity());
        filterManager.setBlacklistEnabled(swBlacklist.isChecked());
        filterManager.setWhitelistEnabled(swWhitelist.isChecked());
        
        // 立即应用主题设置
        ThemeManager themeManager = new ThemeManager(getActivity());
        themeManager.applyTheme();
        
        // 清除内存存储中的临时设置
        ThemeMemoryStorage.getInstance().clear();
        
        // 如果修改了多任务隐藏设置，实时生效
        if (oldHideInMultitask != newHideInMultitask) {
            // 使用ActivityManager API实时生效 (API 21+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                android.app.ActivityManager am = (android.app.ActivityManager) getActivity().getSystemService(android.content.Context.ACTIVITY_SERVICE);
                if (am != null) {
                    java.util.List<android.app.ActivityManager.AppTask> tasks = am.getAppTasks();
                    if (tasks != null && !tasks.isEmpty()) {
                        tasks.get(0).setExcludeFromRecents(newHideInMultitask);
                    }
                }
            }
        }
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
