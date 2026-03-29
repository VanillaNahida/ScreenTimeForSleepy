package com.vanilla.screentimeforsleepy.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vanilla.screentimeforsleepy.manager.AppFilterManager;
import com.vanilla.screentimeforsleepy.util.AppInfo;
import com.vanilla.screentimeforsleepy.util.AppLogger;
import com.vanilla.screentimeforsleepy.HomeFragment;
import com.vanilla.screentimeforsleepy.R;
import com.vanilla.screentimeforsleepy.service.ScreenTimeSyncService;
import com.vanilla.screentimeforsleepy.util.ThemeManager;
import com.vanilla.screentimeforsleepy.fragment.LogsFragment;
import com.vanilla.screentimeforsleepy.fragment.SettingsFragment;

import java.util.ArrayList;
import java.util.List;

public class AppFilterActivity extends AppCompatActivity {

    private static final String EXTRA_FILTER_TYPE = "filter_type";
    public static final String FILTER_TYPE_BLACKLIST = "blacklist";
    public static final String FILTER_TYPE_WHITELIST = "whitelist";

    private String filterType;
    private LinearLayout llAppList;
    private EditText etSearch;
    private ImageButton btnMenu;
    private List<AppInfo> allApps;
    private List<AppInfo> filteredApps;
    private AppFilterManager filterManager;
    private android.os.Handler searchHandler;
    private Runnable searchRunnable;
    
    // 排序和过滤选项
    private String sortBy = "appName"; // 默认按应用名称排序
    private boolean sortDescending = false; // 默认升序
    private boolean showSystemApps = false; // 默认不显示系统应用
    private boolean showOnlyCheckedApps = false; // 默认不显示仅勾选的应用

    private boolean isActivityDestroyed = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_filter);
        isActivityDestroyed = false;

        // 获取过滤器类型
        filterType = getIntent().getStringExtra(EXTRA_FILTER_TYPE);
        if (filterType == null) {
            filterType = FILTER_TYPE_BLACKLIST;
        }

        // 初始化控件
        llAppList = findViewById(R.id.ll_app_list);
        etSearch = findViewById(R.id.et_search);
        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        btnMenu = findViewById(R.id.btn_menu);

        // 设置标题
        if (filterType.equals(FILTER_TYPE_BLACKLIST)) {
            tvTitle.setText("应用黑名单");
        } else {
            tvTitle.setText("应用白名单");
        }

        // 初始化搜索处理器
        searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        // 初始化过滤器管理器
        filterManager = new AppFilterManager(this);

        // 获取所有应用（在后台线程中加载，防止 UI 线程阻塞）
        loadAllAppsAsync();

        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> finish());

        // 设置菜单按钮点击事件
        btnMenu.setOnClickListener(v -> showOptionsMenu());

        // 设置搜索功能（带防抖）
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 取消之前的搜索任务
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                // 创建新的搜索任务，延迟 300ms 执行
                searchRunnable = () -> filterApps(s.toString());
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 设置搜索框回车键监听
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            // 当按下回车键时立即执行搜索
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                // 取消之前的搜索任务
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                // 立即执行搜索
                filterApps(etSearch.getText().toString());
                return true;
            }
            return false;
        });

    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityDestroyed = true;
        // 清理搜索任务
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        // 隐藏加载弹窗
        hideLoadingDialog();
    }



    // 根据筛选选项过滤后的应用列表（搜索的数据源）
    private List<AppInfo> optionFilteredApps;
    
    // 过滤应用
    private void filterApps(String searchText) {
        // 在后台线程中执行过滤操作，避免 UI 线程阻塞
        new Thread(() -> {
            // 创建新列表来存储过滤结果，避免 ConcurrentModificationException
            List<AppInfo> newFilteredApps = new ArrayList<>();
            // 使用根据筛选选项过滤过的 optionFilteredApps 作为数据源，而不是 filteredApps
            List<AppInfo> sourceApps = optionFilteredApps != null ? optionFilteredApps : allApps;
            if (sourceApps != null) {
                if (searchText.isEmpty()) {
                    newFilteredApps.addAll(sourceApps);
                } else {
                    String lowerSearchText = searchText.toLowerCase();
                    for (AppInfo app : sourceApps) {
                        // 同时搜索应用名称和包名
                        if (app != null) {
                            String appName = app.getAppName() != null ? app.getAppName().toLowerCase() : "";
                            String packageName = app.getPackageName() != null ? app.getPackageName().toLowerCase() : "";
                            if (appName.contains(lowerSearchText) || packageName.contains(lowerSearchText)) {
                                newFilteredApps.add(app);
                            }
                        }
                    }
                }
            }
            
            // 在 UI 线程中更新界面
            runOnUiThread(() -> {
                // 检查 Activity 是否已经销毁
                if (!isActivityDestroyed) {
                    // 赋值给成员变量
                    filteredApps = newFilteredApps;
                    // 显示应用列表
                    displayAppList();
                }
            });
        }).start();
    }

    // 显示应用列表
    private void displayAppList() {
        llAppList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (filteredApps != null) {
            for (AppInfo app : filteredApps) {
                View appItem = inflater.inflate(R.layout.app_filter_item, llAppList, false);

                ImageView ivAppIcon = appItem.findViewById(R.id.iv_app_icon);
                TextView tvAppName = appItem.findViewById(R.id.tv_app_name);
                TextView tvPackageName = appItem.findViewById(R.id.tv_package_name);
                CheckBox cbApp = appItem.findViewById(R.id.cb_app);

                ivAppIcon.setImageDrawable(app.getAppIcon());
                tvAppName.setText(app.getAppName());
                tvPackageName.setText(app.getPackageName());
                cbApp.setChecked(app.isChecked());

                // 设置复选框点击事件
                cbApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    app.setChecked(isChecked);
                    if (isChecked) {
                        filterManager.addToFilterList(filterType, app.getPackageName());
                    } else {
                        filterManager.removeFromFilterList(filterType, app.getPackageName());
                    }
                });

                llAppList.addView(appItem);
            }
        }
    }

    // 显示选项菜单
    private void showOptionsMenu() {
        // 创建自定义对话框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("筛选");
        
        // 自定义对话框布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_options, null);
        builder.setView(dialogView);
        
        // 初始化对话框中的控件
        RadioButton rbAppName = dialogView.findViewById(R.id.rb_app_name);
        RadioButton rbPackageName = dialogView.findViewById(R.id.rb_package_name);
        RadioButton rbInstallTime = dialogView.findViewById(R.id.rb_install_time);
        RadioButton rbUpdateTime = dialogView.findViewById(R.id.rb_update_time);
        CheckBox cbSortDescending = dialogView.findViewById(R.id.cb_sort_descending);
        CheckBox cbShowSystemApps = dialogView.findViewById(R.id.cb_show_system_apps);
        CheckBox cbShowOnlyChecked = dialogView.findViewById(R.id.cb_show_only_checked);
        
        // 设置默认值
        switch (sortBy) {
            case "appName":
                rbAppName.setChecked(true);
                break;
            case "packageName":
                rbPackageName.setChecked(true);
                break;
            case "installTime":
                rbInstallTime.setChecked(true);
                break;
            case "updateTime":
                rbUpdateTime.setChecked(true);
                break;
        }
        cbSortDescending.setChecked(sortDescending);
        cbShowSystemApps.setChecked(showSystemApps);
        cbShowOnlyChecked.setChecked(showOnlyCheckedApps);
        
        // 设置确定按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            // 保存排序和过滤选项
            if (rbAppName.isChecked()) {
                sortBy = "appName";
            } else if (rbPackageName.isChecked()) {
                sortBy = "packageName";
            } else if (rbInstallTime.isChecked()) {
                sortBy = "installTime";
            } else if (rbUpdateTime.isChecked()) {
                sortBy = "updateTime";
            }
            sortDescending = cbSortDescending.isChecked();
            showSystemApps = cbShowSystemApps.isChecked();
            showOnlyCheckedApps = cbShowOnlyChecked.isChecked();
            
            // 重新加载和显示应用列表
            reloadApps();
        });
        
        // 设置取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
        });
        
        // 显示对话框
        builder.show();
    }

    // 加载所有应用
    private void loadAllApps() {
        allApps = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : apps) {
            String appName = packageManager.getApplicationLabel(appInfo).toString();
            String packageName = appInfo.packageName;
            boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            
            // 获取安装和更新时间
            long installTime = 0;
            long updateTime = 0;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime;
                    updateTime = packageManager.getPackageInfo(packageName, 0).lastUpdateTime;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            AppInfo app = new AppInfo(appName, packageName, isSystemApp, appInfo.loadIcon(packageManager));
            app.setInstallTime(installTime);
            app.setUpdateTime(updateTime);
            app.setChecked(filterManager.isInFilterList(filterType, packageName));
            allApps.add(app);
        }

        // 排序应用列表
        sortApps();
        filteredApps = new ArrayList<>(allApps);
    }
    
    // 根据筛选选项过滤应用
    private List<AppInfo> filterAppsByOptions(List<AppInfo> apps) {
        List<AppInfo> filtered = new ArrayList<>();
        for (AppInfo app : apps) {
            // 检查是否显示系统应用
            if (!showSystemApps && app.isSystemApp()) {
                continue;
            }
            
            // 检查是否只显示已勾选的应用
            if (showOnlyCheckedApps && !app.isChecked()) {
                continue;
            }
            filtered.add(app);
        }
        return filtered;
    }

    // 排序应用列表
    private void sortApps() {
        allApps.sort((app1, app2) -> {
            // 处理空值情况
            if (app1 == null && app2 == null) {
                return 0;
            }
            if (app1 == null) {
                return -1;
            }
            if (app2 == null) {
                return 1;
            }
            
            int result = 0;
            
            switch (sortBy) {
                case "appName":
                    String appName1 = app1.getAppName() != null ? app1.getAppName() : "";
                    String appName2 = app2.getAppName() != null ? app2.getAppName() : "";
                    result = appName1.compareTo(appName2);
                    break;
                case "packageName":
                    String packageName1 = app1.getPackageName() != null ? app1.getPackageName() : "";
                    String packageName2 = app2.getPackageName() != null ? app2.getPackageName() : "";
                    result = packageName1.compareTo(packageName2);
                    break;
                case "installTime":
                    result = Long.compare(app1.getInstallTime(), app2.getInstallTime());
                    break;
                case "updateTime":
                    result = Long.compare(app1.getUpdateTime(), app2.getUpdateTime());
                    break;
                default:
                    // 默认按应用名称排序
                    String defaultAppName1 = app1.getAppName() != null ? app1.getAppName() : "";
                    String defaultAppName2 = app2.getAppName() != null ? app2.getAppName() : "";
                    result = defaultAppName1.compareTo(defaultAppName2);
                    break;
            }
            
            // 倒序
            if (sortDescending) {
                result = -result;
            }
            
            return result;
        });
    }

    private androidx.appcompat.app.AlertDialog loadingDialog;
    
    // 显示加载弹窗
    private void showLoadingDialog() {
        // 创建加载弹窗
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("加载中");
        
        // 创建自定义布局
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(20, 20, 20, 20);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // 添加加载动画（左边）
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, 20, 0);
        layout.addView(progressBar);
        
        // 添加文字（右边）
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText("正在加载应用列表...");
        textView.setTextSize(16);
        layout.addView(textView);
        
        builder.setView(layout);
        
        // 设置为不可取消
        builder.setCancelable(false);
        
        // 显示弹窗
        loadingDialog = builder.show();
    }
    
    // 隐藏加载弹窗
    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }
    
    // 异步加载所有应用
    private void loadAllAppsAsync() {
        // 显示加载弹窗
        showLoadingDialog();
        
        // 在后台线程中加载应用列表
        new Thread(() -> {
            try {
                // 加载应用列表
                loadAllApps();
                
                // 检查应用数量（基于原始应用列表大小，不是过滤后的）
                final boolean hasAppListPermission = allApps != null && allApps.size() > 1;
                
                // 在 UI 线程中更新界面
                runOnUiThread(() -> {
                    // 检查 Activity 是否已经销毁
                    if (!isActivityDestroyed) {
                        if (hasAppListPermission) {
                            // 根据筛选选项过滤应用
                            optionFilteredApps = filterAppsByOptions(allApps);
                            // 应用搜索过滤
                            filterApps(etSearch.getText().toString());
                            // 显示应用列表
                            displayAppList();
                        } else {
                            // 应用数量小于等于1，说明权限未成功授予，弹窗提示用户开启权限
                            androidx.appcompat.app.AlertDialog.Builder permissionBuilder = new androidx.appcompat.app.AlertDialog.Builder(AppFilterActivity.this);
                            permissionBuilder.setTitle("权限提示");
                            permissionBuilder.setMessage("获取应用列表权限未成功授予，请开启权限以管理应用黑白名单");
                            permissionBuilder.setPositiveButton("前往设置", (dialog, which) -> {
                                // 跳转到应用设置页面
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            });
                            permissionBuilder.setNegativeButton("取消", null);
                            permissionBuilder.show();
                        }
                        // 隐藏加载弹窗
                        hideLoadingDialog();
                    }
                });
            } catch (Exception e) {
                // 发生异常时，在 UI 线程中隐藏加载弹窗
                runOnUiThread(() -> {
                    hideLoadingDialog();
                    // 显示错误提示
                    androidx.appcompat.app.AlertDialog.Builder errorBuilder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    errorBuilder.setTitle("加载失败");
                    errorBuilder.setMessage("加载应用列表时发生错误");
                    errorBuilder.setPositiveButton("确定", null);
                    errorBuilder.show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    // 重新加载应用列表（用于菜单选项更改后）
    private void reloadApps() {
        // 显示加载弹窗
        showLoadingDialog();
        
        // 在后台线程中加载应用列表
        new Thread(() -> {
            try {
                // 加载应用列表
                loadAllApps();
                
                // 检查应用数量（基于原始应用列表大小，不是过滤后的）
                final boolean hasAppListPermission = allApps != null && allApps.size() > 1;
                
                // 在 UI 线程中更新界面
                runOnUiThread(() -> {
                    // 检查 Activity 是否已经销毁
                    if (!isActivityDestroyed) {
                        if (hasAppListPermission) {
                            // 根据筛选选项过滤应用
                            optionFilteredApps = filterAppsByOptions(allApps);
                            // 应用搜索过滤
                            filterApps(etSearch.getText().toString());
                            // 显示应用列表
                            displayAppList();
                        } else {
                            // 应用数量小于等于1，说明权限未成功授予，弹窗提示用户开启权限
                            androidx.appcompat.app.AlertDialog.Builder permissionBuilder = new androidx.appcompat.app.AlertDialog.Builder(AppFilterActivity.this);
                            permissionBuilder.setTitle("权限提示");
                            permissionBuilder.setMessage("获取应用列表权限未成功授予，请开启权限以管理应用黑白名单");
                            permissionBuilder.setPositiveButton("前往设置", (dialog, which) -> {
                                // 跳转到应用设置页面
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            });
                            permissionBuilder.setNegativeButton("取消", null);
                            permissionBuilder.show();
                        }
                        // 隐藏加载弹窗
                        hideLoadingDialog();
                    }
                });
            } catch (Exception e) {
                // 发生异常时，在 UI 线程中隐藏加载弹窗
                runOnUiThread(() -> {
                    hideLoadingDialog();
                    // 显示错误提示
                    androidx.appcompat.app.AlertDialog.Builder errorBuilder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    errorBuilder.setTitle("加载失败");
                    errorBuilder.setMessage("加载应用列表时发生错误");
                    errorBuilder.setPositiveButton("确定", null);
                    errorBuilder.show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    public static class MainActivity extends AppCompatActivity {

        private BottomNavigationView bottomNavigationView;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);

            // 检查是否需要隐藏在最近任务中
            SharedPreferences prefs = getSharedPreferences("app_config", MODE_PRIVATE);
            boolean hideInMultitask = prefs.getBoolean("hide_in_multitask", false);

            // 使用ActivityManager API设置是否显示在最近任务中 (API 21+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.app.ActivityManager.TaskDescription taskDescription = new android.app.ActivityManager.TaskDescription(null, null, 0);
                setTaskDescription(taskDescription);

                // 使用ActivityManager API设置任务的排除状态
                android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(android.content.Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<android.app.ActivityManager.AppTask> tasks = am.getAppTasks();
                    if (tasks != null && !tasks.isEmpty()) {
                        tasks.get(0).setExcludeFromRecents(hideInMultitask);
                    }
                }
            }

            setContentView(R.layout.activity_main);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // 应用主题设置
            ThemeManager themeManager = new ThemeManager(this);
            themeManager.applyTheme();

            // 初始化底部导航栏
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);

            // 启动屏幕使用时间同步服务
            AppLogger.i("MainActivity", "启动屏幕使用时间同步服务");
            ScreenTimeSyncService.startService(this);
            AppLogger.i("MainActivity", "应用启动成功");

            // 默认显示首页 Fragment
            if (savedInstanceState == null) {
                loadFragment(new HomeFragment());
            }
        }

        @Override
        protected void onResume() {
            super.onResume();
            // 每次恢复时重新应用主题（设置页面修改后会立即生效）
            ThemeManager themeManager = new ThemeManager(this);
            themeManager.applyTheme();
        }

        // 底部导航栏选择监听器
        private boolean onNavigationItemSelected(MenuItem item) {
            Fragment fragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_logs) {
                fragment = new LogsFragment();
            } else if (itemId == R.id.nav_settings) {
                fragment = new SettingsFragment();
            }

            return loadFragment(fragment);
        }

        // 加载 Fragment
        private boolean loadFragment(Fragment fragment) {
            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commitNow();
                // 应用主题设置，确保切换Fragment后主题保持一致
                // 使用postDelayed确保Fragment的视图已经创建完成
                findViewById(R.id.fragment_container).post(() -> {
                    ThemeManager themeManager = new ThemeManager(this);
                    themeManager.applyTheme();
                });
                return true;
            }
            return false;
        }
    }
}
