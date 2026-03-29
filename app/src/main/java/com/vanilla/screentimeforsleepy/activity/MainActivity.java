package com.vanilla.screentimeforsleepy.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vanilla.screentimeforsleepy.util.AppLogger;
import com.vanilla.screentimeforsleepy.HomeFragment;
import com.vanilla.screentimeforsleepy.R;
import com.vanilla.screentimeforsleepy.service.ScreenTimeSyncService;
import com.vanilla.screentimeforsleepy.util.ThemeManager;
import com.vanilla.screentimeforsleepy.fragment.LogsFragment;
import com.vanilla.screentimeforsleepy.fragment.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        // 检查是否需要隐藏在最近任务中
        SharedPreferences prefs = getSharedPreferences("app_config", MODE_PRIVATE);
        boolean hideInMultitask = prefs.getBoolean("hide_in_multitask", false);
        
        // 使用ActivityManager API设置是否显示在最近任务中 (API 21+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.app.ActivityManager.TaskDescription taskDescription = new android.app.ActivityManager.TaskDescription(null, null, 0);
            setTaskDescription(taskDescription);
            
            // 使用ActivityManager API设置任务的排除状态
            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(android.content.Context.ACTIVITY_SERVICE);
            if (am != null) {
                java.util.List<android.app.ActivityManager.AppTask> tasks = am.getAppTasks();
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
