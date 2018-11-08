package com.lxbnjupt.pluginactivitydemo.utils;

import java.lang.reflect.Field;

/**
 * Created by liuxiaobo on 2018/11/8.
 */

public class ReflectUtils {

    public static Field getField(Class clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field;
    }

    public static Object getField(Class clazz, String fieldName, Object obj) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        return field.get(obj);
    }

    public static void setField(Class clazz, String fieldName, Object obj, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(obj, value);
    }
}
