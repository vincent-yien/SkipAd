package com.star.adskiphelper.utils;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 功能简述
 * 功能详细描述
 *
 * @author: wensheng.ran
 * @since: 2024-07-10
 * @version: 1.0.0
 */
public class ClickUtil {
    private static final String TAG = "ClickUtil";

    public static boolean clickNode(AccessibilityNodeInfo node){
        //尝试点击
        boolean focused = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        //打印点击按钮的结果
        LogUtil.i("focused result = " + focused);
        LogUtil.i("clicked result = " + clicked);
        return clicked;
    }


}
