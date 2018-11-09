package com.lxbnjupt.pluginactivitydemo.application;

import android.app.Application;
import android.content.Context;

import com.lxbnjupt.pluginactivitydemo.utils.HookHelper;

/**
 * Created by liuxiaobo on 2018/11/8.
 */

public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            // 通过Hook IActivityManager实现Activity插件化
//            HookHelper.hookAMS();
//            HookHelper.hookHandler();

            // 通过Hook Instrumentation实现Activity插件化
            HookHelper.hookInstrumentation(base);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
