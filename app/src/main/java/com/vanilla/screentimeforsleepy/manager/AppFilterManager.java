package com.vanilla.screentimeforsleepy;

import android.content.Context;
import android.content.SharedPreferences;

import com.vanilla.screentimeforsleepy.activity.AppFilterActivity;

import java.util.ArrayList;
import java.util.List;

public class AppFilterManager {

    private static final String PREF_NAME = "app_filter";
    private static final String KEY_BLACKLIST = "blacklist";
    private static final String KEY_WHITELIST = "whitelist";
    private static final String KEY_ENABLE_BLACKLIST = "enable_blacklist";
    private static final String KEY_ENABLE_WHITELIST = "enable_whitelist";

    private SharedPreferences sharedPreferences;

    public AppFilterManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 添加应用到过滤列表
    public void addToFilterList(String filterType, String packageName) {
        List<String> filterList = getFilterList(filterType);
        if (!filterList.contains(packageName)) {
            filterList.add(packageName);
            saveFilterList(filterType, filterList);
        }
    }

    // 从过滤列表中删除应用
    public void removeFromFilterList(String filterType, String packageName) {
        List<String> filterList = getFilterList(filterType);
        if (filterList.contains(packageName)) {
            filterList.remove(packageName);
            saveFilterList(filterType, filterList);
        }
    }

    // 检查应用是否在过滤列表中
    public boolean isInFilterList(String filterType, String packageName) {
        List<String> filterList = getFilterList(filterType);
        return filterList.contains(packageName);
    }

    // 获取过滤列表
    public List<String> getFilterList(String filterType) {
        String key = filterType.equals(AppFilterActivity.FILTER_TYPE_BLACKLIST) ? KEY_BLACKLIST : KEY_WHITELIST;
        String listString = sharedPreferences.getString(key, "");
        List<String> filterList = new ArrayList<>();

        if (!listString.isEmpty()) {
            String[] packages = listString.split(",");
            for (String pkg : packages) {
                if (!pkg.isEmpty()) {
                    filterList.add(pkg);
                }
            }
        }

        return filterList;
    }

    // 保存过滤列表
    private void saveFilterList(String filterType, List<String> filterList) {
        String key = filterType.equals(AppFilterActivity.FILTER_TYPE_BLACKLIST) ? KEY_BLACKLIST : KEY_WHITELIST;
        StringBuilder listString = new StringBuilder();

        for (int i = 0; i < filterList.size(); i++) {
            listString.append(filterList.get(i));
            if (i < filterList.size() - 1) {
                listString.append(",");
            }
        }

        sharedPreferences.edit().putString(key, listString.toString()).apply();
    }

    // 启用或禁用黑名单
    public void setBlacklistEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_ENABLE_BLACKLIST, enabled).apply();
    }

    // 检查黑名单是否启用
    public boolean isBlacklistEnabled() {
        return sharedPreferences.getBoolean(KEY_ENABLE_BLACKLIST, false);
    }

    // 启用或禁用白名单
    public void setWhitelistEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_ENABLE_WHITELIST, enabled).apply();
    }

    // 检查白名单是否启用
    public boolean isWhitelistEnabled() {
        return sharedPreferences.getBoolean(KEY_ENABLE_WHITELIST, false);
    }

    // 检查应用是否应该被过滤
    public boolean shouldFilterApp(String appIdentifier) {
        boolean enableBlacklist = isBlacklistEnabled();
        boolean enableWhitelist = isWhitelistEnabled();

        // 如果同时启用了黑白名单，优先使用黑名单
        if (enableBlacklist) {
            return isInFilterList(AppFilterActivity.FILTER_TYPE_BLACKLIST, appIdentifier);
        }

        // 如果只启用了白名单，检查应用是否不在白名单中
        if (enableWhitelist) {
            return !isInFilterList(AppFilterActivity.FILTER_TYPE_WHITELIST, appIdentifier);
        }

        // 两个都没有启用，不过滤任何应用
        return false;
    }

    // 检查应用是否应该被包含（显示和上报）
    public boolean shouldIncludeApp(String appIdentifier) {
        return !shouldFilterApp(appIdentifier);
    }
    
    // 获取黑名单应用数量
    public int getBlacklistCount() {
        return getFilterList(AppFilterActivity.FILTER_TYPE_BLACKLIST).size();
    }
    
    // 获取白名单应用数量
    public int getWhitelistCount() {
        return getFilterList(AppFilterActivity.FILTER_TYPE_WHITELIST).size();
    }
}
