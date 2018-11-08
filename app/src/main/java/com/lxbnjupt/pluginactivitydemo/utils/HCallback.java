package com.lxbnjupt.pluginactivitydemo.utils;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

/**
 * Created by liuxiaobo on 2018/11/8.
 */

public class HCallback implements Handler.Callback {

    private static final int LAUNCH_ACTIVITY = 100;
    Handler mHandler;

    public HCallback(Handler handler) {
        mHandler = handler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == LAUNCH_ACTIVITY) {
            Object obj = msg.obj;
            try {
                // 获取启动SubActivity的Intent
                Intent stubIntent = (Intent) ReflectUtils.getField(obj.getClass(), "intent", obj);

                // 获取启动PluginActivity的Intent(之前保存在启动SubActivity的Intent之中)
                Intent pluginIntent = stubIntent.getParcelableExtra(HookHelper.PLUGIN_INTENT);

                // 将启动SubActivity的Intent替换为启动PluginActivity的Intent
                stubIntent.setComponent(pluginIntent.getComponent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mHandler.handleMessage(msg);
        return true;
    }
}
