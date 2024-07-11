package com.star.adskiphelper.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.star.adskiphelper.BuildConfig;
import com.star.adskiphelper.config.Constant;
import com.star.adskiphelper.ProcessAction;
import com.star.adskiphelper.utils.AccessibilityHelper;
import com.star.adskiphelper.utils.LogUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 广告跳过具体实现类
 * */
public class AdSkipServiceImpl {

    private static final String TAG = "AdSkipServiceImpl";

    private final AccessibilityService service;

    /**
     * 广告跳过动作
     */
    public Handler processActionHandler;

    private ScheduledExecutorService taskExecutorService;

    /**
     * 是否已开启跳过进程
     * */
    private volatile boolean skipProcessRunning;
    /**
     * 当前打开应用的package和Activity
     * */
    private String currentPackageName;
    /**
     * 用户安装的应用的包名
     * */
    private Set<String> installPackages;
    /**
     * 节点描述集合
     * */
    private Set<String> nodeDescribes;

    public AdSkipServiceImpl(AccessibilityService service) {
        this.service = service;
    }

    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected: ");
        try {
            currentPackageName = "PackageName";

            installPackages = getInstallPackages();

            nodeDescribes = new HashSet<>();

            initActionHandler();

            taskExecutorService = Executors.newSingleThreadScheduledExecutor();
        } catch (Throwable e) {
            LogUtil.e(e.getMessage());
        }
    }

    public void onServiceUnBind(){
        stopSkipAdProcess();
        if(taskExecutorService != null){
            taskExecutorService.shutdown();
        }
    }

    /**
     * 初始化指令接收handler
     * */
    private void initActionHandler() {
        processActionHandler = new Handler(Looper.getMainLooper(), msg -> {
            switch (msg.what) {
                case ProcessAction.ACTION_REFRESH_PACKAGE:
                    installPackages = getInstallPackages();
                    break;
                case ProcessAction.ACTION_STOP_SERVICE:
                    service.disableSelf();
                    break;
                case ProcessAction.ACTION_START_SKIP_PROCESS:
                    startSkipAdProcess();
                    break;
                case ProcessAction.ACTION_STOP_SKIP_PROCESS:
                    stopSkipAdProcess();
                    break;
            }
            return true;
        });
    }

    /**
     * 处理手机界面变动的事件
     * */
    public void onAccessibilityEvent(AccessibilityEvent event) {
        LogUtil.e(TAG+ " onAccessibilityEvent: "+event);
        if (event.getPackageName() == null || event.getClassName() == null){
            return;
        }
        String actionPackageName = event.getPackageName().toString();
        String actionClassname = event.getClassName().toString();
        try {
            if(BuildConfig.DEBUG) {
                LogUtil.e(TAG+ " onAccessibilityEvent : " + event + "," + actionPackageName +","+ actionClassname);
            }
            if(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
                boolean isActivity = !actionClassname.startsWith("android.")
                        && !actionClassname.startsWith("androidx.");
                if(BuildConfig.DEBUG) {
                    LogUtil.i(TAG+ " onAccessibilityEvent : " + currentPackageName + "," + actionPackageName);
                }
                if (!currentPackageName.equals(actionPackageName)) {//打开的是一个新应用
                    if (isActivity) {
                        currentPackageName = actionPackageName;
                        stopSkipAdProcess();
                        if (installPackages.contains(actionPackageName)) {
                            startSkipAdProcess();
                        }
                    }
                }
                executeSkipTask(service.getRootInActiveWindow());
            } else if(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){
                if (installPackages.contains(actionPackageName)) {
                    executeSkipTask(service.getRootInActiveWindow());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行跳过任务
     * */
    private void executeSkipTask(final AccessibilityNodeInfo nodeInfo){
        if (!skipProcessRunning) {
            return;
        }
        taskExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                iterateNodesToSkipAd(nodeInfo);
            }
        });
    }

    /**
     * 遍历节点跳过广告
     * @param rootNodeInfo 根节点信息
     */
    private void iterateNodesToSkipAd(AccessibilityNodeInfo rootNodeInfo) {
        if(skipProcessRunning) {
            AccessibilityHelper.traverseNodes(rootNodeInfo, 0);
        }
    }

    /**
     * 开始扫描进程
     */
    public void startSkipAdProcess() {
        skipProcessRunning = true;
        nodeDescribes.clear();

        //定时结束进程(默认4秒)
        processActionHandler.removeMessages(ProcessAction.ACTION_STOP_SKIP_PROCESS);
        processActionHandler.sendEmptyMessageDelayed(ProcessAction.ACTION_STOP_SKIP_PROCESS, Constant.SCAN_TIME);
    }

    /**
     * 结束扫描进程
     */
    public void stopSkipAdProcess() {
        skipProcessRunning = false;
        processActionHandler.removeMessages(ProcessAction.ACTION_STOP_SKIP_PROCESS);
    }

    /**
     * 获取用户安装所有应用的报名
     * @return 安装的应用包的集合
     */
    private Set<String> getInstallPackages() {
        PackageManager packageManager = service.getPackageManager();
        Set<String> installPackages = new HashSet<>();
        Set<String> homeAppPackages = new HashSet<>();

        // 获取所有应用的包名
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        for (ResolveInfo e : resolveInfoList) {
            installPackages.add(e.activityInfo.packageName);
        }
        // 获取桌面应用的包名
        intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        for (ResolveInfo e : resolveInfoList) {
            homeAppPackages.add(e.activityInfo.packageName);
        }

        //加入当前应用的包名和设置的包名
        homeAppPackages.add(service.getPackageName());
        homeAppPackages.add("com.android.settings");

        installPackages.removeAll(homeAppPackages);
        return installPackages;
    }
}
