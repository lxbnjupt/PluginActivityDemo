package com.lxbnjupt.pluginactivitydemo.utils;

import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * Created by liuxiaobo on 2018/11/8.
 */

public class HookHelper {

    private static final String TAG = "HookHelper";
    public static final String PLUGIN_INTENT = "plugin_intent";

    /**
     * Hook IActivityManager
     * @throws Exception
     */
    public static void hookAMS() throws Exception {
        Log.e(TAG, "hookAMS");
        Object singleton = null;
        if (Build.VERSION.SDK_INT >= 26) {
            Class<?> activityManageClazz = Class.forName("android.app.ActivityManager");
            // 获取ActivityManager中的IActivityManagerSingleton字段
            Field iActivityManagerSingletonField = ReflectUtils.getField(activityManageClazz, "IActivityManagerSingleton");
            singleton = iActivityManagerSingletonField.get(activityManageClazz);
        } else {
            Class<?> activityManagerNativeClazz = Class.forName("android.app.ActivityManagerNative");
            // 获取ActivityManagerNative中的gDefault字段
            Field gDefaultField = ReflectUtils.getField(activityManagerNativeClazz, "gDefault");
            singleton = gDefaultField.get(activityManagerNativeClazz);
        }

        Class<?> singletonClazz = Class.forName("android.util.Singleton");
        // 获取Singleton中mInstance字段
        Field mInstanceField = ReflectUtils.getField(singletonClazz, "mInstance");
        // 获取IActivityManager
        Object iActivityManager = mInstanceField.get(singleton);

        Class<?> iActivityManagerClazz = Class.forName("android.app.IActivityManager");
        // 获取IActivityManager代理对象
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{iActivityManagerClazz}, new IActivityManagerProxy(iActivityManager));

        // 将IActivityManager代理对象赋值给Singleton中mInstance字段
        mInstanceField.set(singleton, proxy);
    }

    /**
     * Hook ActivityThread中Handler成员变量mH
     * @throws Exception
     */
    public static void hookHandler() throws Exception {
        Log.e(TAG, "hookHandler");
        Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
        // 获取ActivityThread中成员变量sCurrentActivityThread字段
        Field sCurrentActivityThreadField = ReflectUtils.getField(activityThreadClazz, "sCurrentActivityThread");
        // 获取ActivityThread主线程对象
        Object currentActivityThread = sCurrentActivityThreadField.get(activityThreadClazz);

        // 获取ActivityThread中成员变量mH字段
        Field mHField = ReflectUtils.getField(activityThreadClazz, "mH");
        // 获取ActivityThread主线程中Handler对象
        Handler mH = (Handler) mHField.get(currentActivityThread);

        // 将我们自己的HCallback对象赋值给mH的mCallback
        ReflectUtils.setField(Handler.class, "mCallback", mH, new HCallback(mH));
    }
}
