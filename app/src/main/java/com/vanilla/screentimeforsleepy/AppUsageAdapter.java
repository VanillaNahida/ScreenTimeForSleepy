package com.vanilla.screentimeforsleepy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder> {
    private final List<AppUsageInfo> appUsageInfoList;
    private final long maxUsageTime;
    private OnAppFilterChangeListener onAppFilterChangeListener;
    
    public AppUsageAdapter(List<AppUsageInfo> appUsageInfoList, long maxUsageTime) {
        this.appUsageInfoList = appUsageInfoList;
        this.maxUsageTime = maxUsageTime;
    }
    
    // 设置应用过滤变化监听器
    public void setOnAppFilterChangeListener(OnAppFilterChangeListener listener) {
        this.onAppFilterChangeListener = listener;
    }
    
    // 应用过滤变化监听器接口
    public interface OnAppFilterChangeListener {
        void onAppFilterChanged();
    }
    
    @NonNull
    @Override
    public AppUsageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_item, parent, false);
        return new AppUsageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AppUsageViewHolder holder, int position) {
        AppUsageInfo appUsageInfo = appUsageInfoList.get(position);
        
        // 绑定应用图标
        holder.ivAppIcon.setImageDrawable(appUsageInfo.getAppIcon());
        
        // 绑定应用名称
        holder.tvAppName.setText(appUsageInfo.getAppName());
        
        // 绑定使用时长
        holder.tvUsageTime.setText(UsageStatsHelper.formatUsageTime(appUsageInfo.getUsageTime()));
        
        // 计算并显示进度条百分比
        if (maxUsageTime > 0) {
            int progress = (int) ((appUsageInfo.getUsageTime() * 100) / maxUsageTime);
            holder.pbUsageTime.setProgress(progress);
        } else {
            holder.pbUsageTime.setProgress(0);
        }
        
        // 添加长按事件，弹出菜单
        holder.itemView.setOnLongClickListener(v -> {
            showAppOptionsMenu(v, appUsageInfo);
            return true;
        });
    }
    
    // 显示应用选项菜单
    private void showAppOptionsMenu(View view, AppUsageInfo appUsageInfo) {
        // 创建弹出菜单
        androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(view.getContext(), view);
        
        // 添加菜单项
        popupMenu.getMenu().add(0, 1, 0, "添加到黑名单");
        popupMenu.getMenu().add(0, 2, 0, "添加到白名单");
        
        // 设置菜单项点击事件
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            AppFilterManager filterManager = new AppFilterManager(view.getContext());
            
            switch (itemId) {
                case 1:
                    // 添加到黑名单
                    filterManager.addToFilterList(AppFilterActivity.FILTER_TYPE_BLACKLIST, appUsageInfo.getPackageName());
                    androidx.appcompat.app.AlertDialog.Builder builder1 = new androidx.appcompat.app.AlertDialog.Builder(view.getContext());
                    builder1.setTitle("添加成功");
                    builder1.setMessage("已将应用添加到黑名单");
                    builder1.setPositiveButton("确定", (dialog, which) -> {
                        // 通知监听器应用过滤列表已变化
                        if (onAppFilterChangeListener != null) {
                            onAppFilterChangeListener.onAppFilterChanged();
                        }
                    });
                    builder1.show();
                    return true;
                case 2:
                    // 添加到白名单
                    filterManager.addToFilterList(AppFilterActivity.FILTER_TYPE_WHITELIST, appUsageInfo.getPackageName());
                    androidx.appcompat.app.AlertDialog.Builder builder2 = new androidx.appcompat.app.AlertDialog.Builder(view.getContext());
                    builder2.setTitle("添加成功");
                    builder2.setMessage("已将应用添加到白名单");
                    builder2.setPositiveButton("确定", (dialog, which) -> {
                        // 通知监听器应用过滤列表已变化
                        if (onAppFilterChangeListener != null) {
                            onAppFilterChangeListener.onAppFilterChanged();
                        }
                    });
                    builder2.show();
                    return true;
                default:
                    return false;
            }
        });
        
        // 显示弹出菜单
        popupMenu.show();
    }
    
    @Override
    public int getItemCount() {
        return appUsageInfoList.size();
    }
    
    public static class AppUsageViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivAppIcon;
        final TextView tvAppName;
        final TextView tvUsageTime;
        final ProgressBar pbUsageTime;
        
        public AppUsageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvUsageTime = itemView.findViewById(R.id.tv_usage_time);
            pbUsageTime = itemView.findViewById(R.id.pb_usage_time);
        }
    }
}