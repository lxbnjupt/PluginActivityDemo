package com.lxbnjupt.pluginactivitydemo.utils;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by liuxiaobo on 2018/11/9.
 */

public class InstrumentationProxy extends Instrumentation {

    private Instrumentation mInstrumentation;
    private PackageManager mPackageManager;

    public InstrumentationProxy(Instrumentation instrumentation, PackageManager packageManager) {
        this.mInstrumentation = instrumentation;
        this.mPackageManager = packageManager;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        // 查找要启动的Activity是否已经在AndroidManifest.xml中注册
        List<ResolveInfo> infos = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        if (infos == null || infos.size() == 0) {
            // 要启动的Activity没有注册，则将启动它的Intent保存在Intent中，便于之后还原
            intent.putExtra(HookHelper.PLUGIN_INTENT, intent.getComponent().getClassName());
            // 替换要启动的Activity为StubActivity
            intent.setClassName(who, "com.lxbnjupt.pluginactivitydemo.activity.StubActivity");
        }
        try {
            Method execMethod = Instrumentation.class.getDeclaredMethod("execStartActivity",
                    Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class);
            // 通过反射调用execStartActivity方法，将启动目标变为StubActivity,以此达到通过AMS校验的目的
            return (ActivityResult) execMethod.invoke(mInstrumentation, who, contextThread, token,
                    target, intent, requestCode, options);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        String intentName = intent.getStringExtra(HookHelper.PLUGIN_INTENT);
        if (!TextUtils.isEmpty(intentName)) {
            // 还原启动目标Activity
            return super.newActivity(cl, intentName, intent);
        }
        return super.newActivity(cl, className, intent);
    }
}
