package com.vanilla.screentimeforsleepy.manager;

import android.content.Context;
import android.content.SharedPreferences;

public class AppAliasManager {
    private static final String PREF_NAME = "app_aliases";
    private final SharedPreferences sharedPreferences;

    public AppAliasManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 为应用设置别名
    public void setAppAlias(String packageName, String alias) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(packageName, alias);
        editor.apply();
    }

    // 获取应用别名，如果没有设置则返回null
    public String getAppAlias(String packageName) {
        return sharedPreferences.getString(packageName, null);
    }

    // 删除应用别名
    public void removeAppAlias(String packageName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(packageName);
        editor.apply();
    }

    // 检查应用是否有别名
    public boolean hasAppAlias(String packageName) {
        return sharedPreferences.contains(packageName);
    }
}
