package com.vanilla.screentimeforsleepy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etApiUrl, etApiKey, etDeviceId, etDisplayName, etCheckInterval;
    private Button btnSave;
    private SharedPreferences sharedPreferences;

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
    }

    // 加载已保存的配置
    private void loadSavedConfig() {
        etApiUrl.setText(sharedPreferences.getString("api_url", ""));
        etApiKey.setText(sharedPreferences.getString("api_key", ""));
        etDeviceId.setText(sharedPreferences.getString("device_id", ""));
        etDisplayName.setText(sharedPreferences.getString("display_name", ""));
        etCheckInterval.setText(String.valueOf(sharedPreferences.getInt("check_interval", 60)));
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
        
        editor.apply(); // 应用保存
    }
}