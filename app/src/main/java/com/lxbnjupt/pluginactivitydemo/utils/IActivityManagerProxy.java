package com.lxbnjupt.pluginactivitydemo.utils;

import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by liuxiaobo on 2018/11/8.
 */

public class IActivityManagerProxy implements InvocationHandler {

    private static final String TAG = "IActivityManagerProxy";
    private Object mActivityManager;

    public IActivityManagerProxy(Object activityManager) {
        this.mActivityManager = activityManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("startActivity".equals(method.getName())) {
            Log.e(TAG, "invoke startActivity");
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            // 获取启动PluginActivity的Intent
            Intent pluginIntent = (Intent) args[index];

            // 新建用来启动StubActivity的Intent
            Intent stubIntent = new Intent();
            stubIntent.setClassName("com.lxbnjupt.pluginactivitydemo",
                    "com.lxbnjupt.pluginactivitydemo.activity.StubActivity");
            // 将启动PluginActivity的Intent保存在subIntent中，便于之后还原
            stubIntent.putExtra(HookHelper.PLUGIN_INTENT, pluginIntent);

            // 通过stubIntent赋值给args，从而将启动目标变为StubActivity，以此达到通过AMS校验的目的
            args[index] = stubIntent;
        }
        return method.invoke(mActivityManager, args);
    }
}
