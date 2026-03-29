package com.vanilla.screentimeforsleepy.activity;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vanilla.screentimeforsleepy.util.AppLogger;
import com.vanilla.screentimeforsleepy.R;

import java.util.List;

public class LogsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Spinner spinnerLogLevel;
    private Switch switchAutoScroll;
    private TextView tvLogs;
    private TextView tvEmptyLogs;
    private String selectedLogLevel = "INFO";
    private boolean autoScrollEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_logs);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化控件
        btnBack = findViewById(R.id.btn_back);
        spinnerLogLevel = findViewById(R.id.spinner_log_level);
        switchAutoScroll = findViewById(R.id.switch_auto_scroll);
        tvLogs = findViewById(R.id.tv_logs);
        tvEmptyLogs = findViewById(R.id.tv_empty_logs);
        
        // 启用 TextView 滚动功能
        tvLogs.setMovementMethod(new android.text.method.ScrollingMovementMethod());

        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> finish());

        // 设置自动滚动开关
        setupAutoScrollSwitch();

        // 设置日志级别选择器
        setupLogLevelSpinner();

        // 加载日志
        loadLogs();
    }

    private void setupAutoScrollSwitch() {
        switchAutoScroll.setChecked(autoScrollEnabled);
        switchAutoScroll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoScrollEnabled = isChecked;
            if (isChecked) {
                scrollToBottom();
            }
        });
    }

    private void setupLogLevelSpinner() {
        // 日志级别选项
        String[] logLevels = {"INFO", "DEBUG", "ERROR"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, logLevels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLogLevel.setAdapter(adapter);

        // 设置默认选择
        spinnerLogLevel.setSelection(0);

        // 设置选择监听器
        spinnerLogLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLogLevel = logLevels[position];
                loadLogs();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void scrollToBottom() {
        if (autoScrollEnabled && tvLogs.getLineCount() > 0) {
            tvLogs.post(() -> {
                int lineCount = tvLogs.getLineCount();
                if (lineCount > 0 && tvLogs.getLayout() != null) {
                    // 滚动到最底部
                    int scrollAmount = tvLogs.getLayout().getLineTop(lineCount) - tvLogs.getHeight();
                    if (scrollAmount > 0) {
                        tvLogs.scrollTo(0, scrollAmount);
                    } else {
                        tvLogs.scrollTo(0, 0);
                    }
                }
            });
        }
    }

    private void loadLogs() {
        // 获取过滤后的日志
        List<String> filteredLogs = AppLogger.getLogsByLevel(selectedLogLevel);

        if (filteredLogs.isEmpty()) {
            tvEmptyLogs.setVisibility(View.VISIBLE);
            tvLogs.setVisibility(View.GONE);
        } else {
            tvEmptyLogs.setVisibility(View.GONE);
            tvLogs.setVisibility(View.VISIBLE);
            
            // 构建带颜色的日志文本
            SpannableStringBuilder logText = new SpannableStringBuilder();
            for (String logEntry : filteredLogs) {
                int color;
                if (logEntry.contains("[DEBUG]")) {
                    color = getResources().getColor(android.R.color.holo_blue_light);
                } else if (logEntry.contains("[INFO]")) {
                    color = getResources().getColor(android.R.color.holo_green_light);
                } else if (logEntry.contains("[ERROR]")) {
                    color = getResources().getColor(android.R.color.holo_red_light);
                } else {
                    color = getResources().getColor(android.R.color.primary_text_dark);
                }
                
                int start = logText.length();
                logText.append(logEntry).append("\n");
                int end = logText.length();
                logText.setSpan(new ForegroundColorSpan(color), start, end, 0);
            }
            tvLogs.setText(logText);
            
            // 自动滚动到最底部
            if (autoScrollEnabled) {
                scrollToBottom();
            }
        }
    }
}