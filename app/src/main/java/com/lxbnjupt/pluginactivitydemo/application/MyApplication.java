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
            HookHelper.hookAMS();
            HookHelper.hookHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
