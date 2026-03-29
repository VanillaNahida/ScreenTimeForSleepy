package com.vanilla.screentimeforsleepy.util;

/**
 * 主题内存存储类
 * 用于临时存储主题设置，实现实时预览效果
 * 在调整设置时先存储在内存中，保存时才持久化到SharedPreferences
 */
public class ThemeMemoryStorage {
    
    private static ThemeMemoryStorage instance;
    
    // 临时存储的主题设置
    private String backgroundImage = null;
    private Float backgroundOpacity = null;
    private Float cardOpacity = null;
    private Float logsOpacity = null;
    
    // 标记是否有未保存的更改
    private boolean hasUnsavedChanges = false;
    
    private ThemeMemoryStorage() {
        // 私有构造函数，单例模式
    }
    
    public static synchronized ThemeMemoryStorage getInstance() {
        if (instance == null) {
            instance = new ThemeMemoryStorage();
        }
        return instance;
    }
    
    /**
     * 设置背景图片路径
     */
    public void setBackgroundImage(String imagePath) {
        this.backgroundImage = imagePath;
        this.hasUnsavedChanges = true;
    }
    
    /**
     * 获取背景图片路径
     */
    public String getBackgroundImage() {
        return backgroundImage;
    }
    
    /**
     * 设置背景不透明度
     */
    public void setBackgroundOpacity(float opacity) {
        this.backgroundOpacity = opacity;
        this.hasUnsavedChanges = true;
    }
    
    /**
     * 获取背景不透明度
     */
    public Float getBackgroundOpacity() {
        return backgroundOpacity;
    }
    
    /**
     * 设置应用卡片不透明度
     */
    public void setCardOpacity(float opacity) {
        this.cardOpacity = opacity;
        this.hasUnsavedChanges = true;
    }
    
    /**
     * 获取应用卡片不透明度
     */
    public Float getCardOpacity() {
        return cardOpacity;
    }
    
    /**
     * 设置日志背景不透明度
     */
    public void setLogsOpacity(float opacity) {
        this.logsOpacity = opacity;
        this.hasUnsavedChanges = true;
    }
    
    /**
     * 获取日志背景不透明度
     */
    public Float getLogsOpacity() {
        return logsOpacity;
    }
    
    /**
     * 检查是否有未保存的更改
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    /**
     * 清除所有临时存储的设置
     * 在保存到SharedPreferences后调用
     */
    public void clear() {
        backgroundImage = null;
        backgroundOpacity = null;
        cardOpacity = null;
        logsOpacity = null;
        hasUnsavedChanges = false;
    }
    
    /**
     * 清除所有临时存储的设置（不标记为未保存）
     * 在取消设置时调用
     */
    public void discardChanges() {
        backgroundImage = null;
        backgroundOpacity = null;
        cardOpacity = null;
        logsOpacity = null;
        hasUnsavedChanges = false;
    }
}
