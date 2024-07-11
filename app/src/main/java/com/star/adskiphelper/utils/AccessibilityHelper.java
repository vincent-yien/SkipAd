package com.star.adskiphelper.utils;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.star.adskiphelper.BuildConfig;
import com.star.adskiphelper.config.Constant;
import com.star.adskiphelper.service.AdSkipService;

import java.util.HashSet;
import java.util.Set;

public class AccessibilityHelper {
    private static final String TAG = "AccessibilityHelper";
    private static final Set<String> nodeDescribes = new HashSet<>();

    /**
     * 遍历AccessibilityNodeInfo节点
     *
     * @param node  当前节点
     * @param depth 当前深度，用于打印时显示层级
     */
    public static void traverseNodes(AccessibilityNodeInfo node, int depth) {
        if (node == null) {
            return;
        }

        if (BuildConfig.DEBUG) {
            // 打印节点信息缩进
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                indent.append("  "); // 增加缩进，用于表示层级
            }

            // 打印节点信息
            LogUtil.i(TAG+ " traverseNodes: " + indent + "Class Name : " + node.getClassName());
            LogUtil.i(TAG+ " traverseNodes: Text: " + node.getText() + "," + node.getContentDescription());
        }

        boolean clickedSuc = false;
        if (SkipUtil.isKeywords(node, Constant.SCAN_KEYWORD)) {
            clickedSuc = clickSkipNode(node);
        }

        if (clickedSuc) {
            node.recycle();
            return;
        }

        // 遍历子节点  
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                traverseNodes(child, depth + 1); // 递归调用，深度+1  
                child.recycle(); // 遍历完成后回收节点，防止内存泄漏  
            }
        }

        // 回收当前节点，注意要在子节点遍历完成后  
        node.recycle();
    }

    public static boolean clickSkipNode(AccessibilityNodeInfo node) {
        //如果不包含"跳过",则不进行点击
        if (!SkipUtil.isKeywords(node, Constant.SCAN_KEYWORD)) {
            return false;
        }
        String nodeDesc = SkipUtil.generateNodeDescribe(node);
        Log.i(TAG, "clickSkipNode: " + nodeDesc);
        //保证不重复点击
        if (!nodeDescribes.contains(nodeDesc)) {
            boolean focused = false;
            boolean clicked;
            //尝试点击
            if (node.isCheckable()) {
                focused = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } else {
                clicked = AdSkipService.touchNode(node);
            }
            //打印点击按钮的描述
            LogUtil.i("try to click " + nodeDesc);
            //打印点击按钮的结果
            LogUtil.i("clicked result = " + clicked);
            LogUtil.i("focused result = " + focused);
            if (clicked) {
                nodeDescribes.add(nodeDesc);
            }
            return clicked;
        }
        return false;
    }
}