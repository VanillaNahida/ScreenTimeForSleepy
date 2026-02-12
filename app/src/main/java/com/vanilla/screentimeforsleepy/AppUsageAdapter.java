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
    
    public AppUsageAdapter(List<AppUsageInfo> appUsageInfoList, long maxUsageTime) {
        this.appUsageInfoList = appUsageInfoList;
        this.maxUsageTime = maxUsageTime;
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