package com.vanilla.screentimeforsleepy.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class ScreenTimeAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 这里可以处理无障碍事件，用于监控屏幕使用情况
    }

    @Override
    public void onInterrupt() {
        // 服务被中断时的处理
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        // 服务连接时的初始化操作
    }
}
