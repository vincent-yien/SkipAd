package com.star.adskiphelper.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.star.adskiphelper.utils.SkipUtil;

import java.lang.ref.WeakReference;

/**
 * 广告跳过service
 */
public class AdSkipService extends AccessibilityService {
    private static final String TAG = "AdSkipService";
    private static WeakReference<AdSkipService> serviceWeakReference;
    private AdSkipServiceImpl serviceImpl;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "onServiceConnected: ");
        serviceWeakReference = new WeakReference<>(this);
        if (serviceImpl == null) {
            serviceImpl = new AdSkipServiceImpl(this);
        }
        serviceImpl.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (serviceImpl != null) {
            serviceImpl.onAccessibilityEvent(event);
        }
    }

    @Override
    public void onInterrupt() {
        if (serviceImpl != null) {
            serviceImpl.stopSkipAdProcess();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (serviceImpl != null) {
//            serviceImpl.onServiceUnBind();
            serviceImpl = null;
        }
        serviceWeakReference = null;
        return super.onUnbind(intent);
    }

    public static boolean isServiceRunning() {
        final AdSkipService service = serviceWeakReference != null ? serviceWeakReference.get() : null;
        return service != null && service.serviceImpl != null;
    }

    public static boolean touchNode(AccessibilityNodeInfo node){
        if(serviceWeakReference == null || serviceWeakReference.get() == null) return false;
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = SkipUtil.getPath(node);
        // 实际上不需要移动，因为这是一个点击操作，但为了符合GestureDescription的要求，我们还是添加了一个点
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 10)); // 持续时间非常短，以模拟点击
        GestureDescription gesture = builder.build();
         return serviceWeakReference.get().dispatchGesture(gesture, null, null);
    }
}
