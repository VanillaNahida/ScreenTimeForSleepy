package com.vanilla.screentimeforsleepy.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import com.vanilla.screentimeforsleepy.R;

import java.io.File;

public class ThemeManager {
    
    private static final String PREFS_NAME = "app_config";
    private static final String KEY_BACKGROUND_IMAGE = "background_image";
    private static final String KEY_BACKGROUND_OPACITY = "background_opacity";
    private static final String KEY_CARD_OPACITY = "card_opacity";
    private static final String KEY_LOGS_OPACITY = "logs_opacity";
    
    private Activity activity;
    private SharedPreferences prefs;
    
    public ThemeManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
    }
    
    // 应用主题到Activity
    public void applyTheme() {
        // 优先从内存存储读取，如果没有则使用SharedPreferences
        ThemeMemoryStorage memoryStorage = ThemeMemoryStorage.getInstance();
        
        String backgroundImage = memoryStorage.getBackgroundImage() != null 
            ? memoryStorage.getBackgroundImage() 
            : prefs.getString(KEY_BACKGROUND_IMAGE, "");
            
        float opacity = memoryStorage.getBackgroundOpacity() != null 
            ? memoryStorage.getBackgroundOpacity() 
            : prefs.getFloat(KEY_BACKGROUND_OPACITY, 0.5f);
            
        float cardOpacity = memoryStorage.getCardOpacity() != null 
            ? memoryStorage.getCardOpacity() 
            : prefs.getFloat(KEY_CARD_OPACITY, 0.8f);
            
        float logsOpacity = memoryStorage.getLogsOpacity() != null 
            ? memoryStorage.getLogsOpacity() 
            : prefs.getFloat(KEY_LOGS_OPACITY, 0.8f);
            
        // 计算底部导航栏的不透明度（背景不透明度+10%，最大为1.0）
        float bottomNavOpacity = Math.min(1.0f, opacity + 0.1f);
        
        // 获取根布局
        View rootView = activity.findViewById(android.R.id.content);
        if (rootView != null) {
            // 应用背景
            if (!backgroundImage.isEmpty()) {
                // 如果有背景图片，使用背景图片
                File bgFile = new File(backgroundImage);
                if (bgFile.exists()) {
                    // 使用背景图片
                    applyBackgroundImage(rootView, backgroundImage, opacity);
                }
            }
            
            // 应用到组件
            applyThemeToComponents(rootView, cardOpacity, logsOpacity, bottomNavOpacity);
        }
    }
    
    // 应用主题到指定视图（用于运行时动态刷新）
    public void applyThemeToView(View view) {
        // 优先从内存存储读取，如果没有则使用SharedPreferences
        ThemeMemoryStorage memoryStorage = ThemeMemoryStorage.getInstance();
            
        float cardOpacity = memoryStorage.getCardOpacity() != null 
            ? memoryStorage.getCardOpacity() 
            : prefs.getFloat(KEY_CARD_OPACITY, 0.8f);
            
        float logsOpacity = memoryStorage.getLogsOpacity() != null 
            ? memoryStorage.getLogsOpacity() 
            : prefs.getFloat(KEY_LOGS_OPACITY, 0.8f);
            
        float bottomNavOpacity = Math.min(1.0f, 
            (memoryStorage.getBackgroundOpacity() != null 
                ? memoryStorage.getBackgroundOpacity() 
                : prefs.getFloat(KEY_BACKGROUND_OPACITY, 0.5f)) + 0.1f);
        
        // 应用到指定视图及其子视图
        applyThemeToComponents(view, cardOpacity, logsOpacity, bottomNavOpacity);
    }
    
    // 应用背景图片
    private void applyBackgroundImage(View view, String imagePath, float opacity) {
        try {
            File bgFile = new File(imagePath);
            if (bgFile.exists()) {
                // 从文件创建Bitmap
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    // 获取屏幕尺寸
                    int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
                    int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
                    
                    // 计算缩放比例，使用centerCrop方式填充屏幕
                    float scaleX = (float) screenWidth / bitmap.getWidth();
                    float scaleY = (float) screenHeight / bitmap.getHeight();
                    float scale = Math.max(scaleX, scaleY);
                    
                    // 计算裁剪后的尺寸
                    int newWidth = Math.round(bitmap.getWidth() * scale);
                    int newHeight = Math.round(bitmap.getHeight() * scale);
                    
                    // 创建缩放后的Bitmap
                    android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    
                    // 计算居中裁剪的区域
                    int cropX = Math.max(0, (newWidth - screenWidth) / 2);
                    int cropY = Math.max(0, (newHeight - screenHeight) / 2);
                    int cropWidth = Math.min(screenWidth, newWidth);
                    int cropHeight = Math.min(screenHeight, newHeight);
                    
                    // 裁剪Bitmap
                    android.graphics.Bitmap croppedBitmap = android.graphics.Bitmap.createBitmap(scaledBitmap, cropX, cropY, cropWidth, cropHeight);
                    
                    // 创建BitmapDrawable
                    android.graphics.drawable.BitmapDrawable drawable = new android.graphics.drawable.BitmapDrawable(activity.getResources(), croppedBitmap);
                    // 设置透明度
                    drawable.setAlpha((int) (255 * opacity));
                    // 设置背景
                    view.setBackground(drawable);
                    
                    // 清理临时Bitmap
                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 应用主题到组件
    private void applyThemeToComponents(View view, float cardOpacity, float logsOpacity, float bottomNavOpacity) {
        try {
            // 检查视图ID，应用不同的不透明度
            int viewId = view.getId();
            // 检查是否是日志显示框
            if (viewId == R.id.tv_logs) {
                applyOpacityToBackground(view, logsOpacity);
            } 
            // 检查是否是应用卡片
            else if (viewId == R.id.app_card_layout) {
                applyOpacityToBackground(view, cardOpacity);
            }
            // 检查是否是底部导航栏
            else if (viewId == R.id.bottom_navigation) {
                applyOpacityToBackground(view, bottomNavOpacity);
            }
            
            // 递归遍历所有子视图
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    applyThemeToComponents(child, cardOpacity, logsOpacity, bottomNavOpacity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 应用不透明度到背景
    private void applyOpacityToBackground(View view, float opacity) {
        try {
            Drawable background = view.getBackground();
            if (background != null) {
                // 复制背景drawable，避免修改原始drawable
                Drawable.ConstantState state = background.getConstantState();
                if (state != null) {
                    Drawable newBackground = state.newDrawable();
                    newBackground.setAlpha((int) (255 * opacity));
                    view.setBackground(newBackground);
                } else {
                    background.setAlpha((int) (255 * opacity));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
