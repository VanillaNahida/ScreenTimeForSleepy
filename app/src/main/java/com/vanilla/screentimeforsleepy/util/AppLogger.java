package com.vanilla.screentimeforsleepy;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AppLogger {

    private static final String TAG = "AppLogger";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final int MAX_LOG_DAYS = 7;

    private static List<String> logBuffer = new ArrayList<>();
    private static final int MAX_BUFFER_SIZE = 100;

    // 日志级别
    public static final String INFO = "INFO";
    public static final String DEBUG = "DEBUG";
    public static final String ERROR = "ERROR";

    /**
     * 记录INFO级别日志
     */
    public static void i(String tag, String message) {
        log(INFO, tag, message);
    }

    /**
     * 记录DEBUG级别日志
     */
    public static void d(String tag, String message) {
        log(DEBUG, tag, message);
    }

    /**
     * 记录ERROR级别日志
     */
    public static void e(String tag, String message) {
        log(ERROR, tag, message);
    }

    /**
     * 记录ERROR级别日志（带异常）
     */
    public static void e(String tag, String message, Throwable throwable) {
        log(ERROR, tag, message + "\n" + Log.getStackTraceString(throwable));
    }

    /**
     * 记录日志
     */
    private static void log(String level, String tag, String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        
        // 获取线程信息
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        long threadId = currentThread.getId();
        
        // 获取调用栈信息
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String className = "Unknown";
        String methodName = "Unknown";
        int lineNumber = 0;
        
        if (stackTrace.length > 4) {
            StackTraceElement element = stackTrace[4];
            className = element.getClassName();
            methodName = element.getMethodName();
            lineNumber = element.getLineNumber();
        }
        
        // 构建详细的日志消息
        String logMessage = timestamp + " [" + level + "] " + tag + " - " + message + "\n" +
                           "  Thread: " + threadName + " (ID: " + threadId + ")\n" +
                           "  Location: " + className + "." + methodName + "():" + lineNumber;
        
        // 输出到Android日志
        switch (level) {
            case INFO:
                Log.i(tag, message);
                break;
            case DEBUG:
                Log.d(tag, message);
                break;
            case ERROR:
                Log.e(tag, message);
                break;
        }
        
        // 添加到缓冲区
        logBuffer.add(logMessage);
        if (logBuffer.size() > MAX_BUFFER_SIZE) {
            logBuffer.remove(0);
        }
        
        // 写入文件
        writeToFile(logMessage);
    }

    /**
     * 写入日志到文件
     */
    private static void writeToFile(String logMessage) {
        try {
            // 获取应用的私有数据目录
            File dir = new File(android.os.Environment.getDataDirectory(), "data/com.vanilla.screentimeforsleepy/logs");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 按照当日日期命名文件名
            String fileName = FILE_DATE_FORMAT.format(new Date()) + ".txt";
            File logFile = new File(dir, fileName);
            
            // 写入日志
            FileWriter fw = new FileWriter(logFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(logMessage);
            bw.newLine();
            bw.close();
            
            // 清理超过七天的日志文件
            cleanOldLogs(dir);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }

    /**
     * 清理超过七天的日志文件
     */
    private static void cleanOldLogs(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        
        // 计算七天前的日期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -MAX_LOG_DAYS);
        Date sevenDaysAgo = calendar.getTime();
        
        for (File file : files) {
            try {
                // 解析文件名中的日期
                String fileName = file.getName();
                if (fileName.endsWith(".txt")) {
                    String dateStr = fileName.substring(0, fileName.length() - 4);
                    Date fileDate = FILE_DATE_FORMAT.parse(dateStr);
                    
                    // 如果文件日期早于七天前，则删除
                    if (fileDate.before(sevenDaysAgo)) {
                        file.delete();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing log file date: " + file.getName(), e);
            }
        }
    }

    /**
     * 获取所有日志
     */
    public static List<String> getAllLogs() {
        return new ArrayList<>(logBuffer);
    }

    /**
     * 根据级别获取日志
     */
    public static List<String> getLogsByLevel(String level) {
        List<String> filteredLogs = new ArrayList<>();
        for (String log : logBuffer) {
            if (level.equals(INFO) && log.contains("[INFO]")) {
                filteredLogs.add(log);
            } else if (level.equals(DEBUG) && (log.contains("[DEBUG]") || log.contains("[INFO]"))) {
                filteredLogs.add(log);
            } else if (level.equals(ERROR) && log.contains("[ERROR]")) {
                filteredLogs.add(log);
            }
        }
        return filteredLogs;
    }

    /**
     * 清空日志
     */
    public static void clearLogs() {
        logBuffer.clear();
    }
}