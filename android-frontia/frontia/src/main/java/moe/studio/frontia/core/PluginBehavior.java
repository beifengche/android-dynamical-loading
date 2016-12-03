/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia.core;

import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import moe.studio.frontia.BuildConfig;
import moe.studio.frontia.ext.PluginError;
import moe.studio.frontia.ext.PluginError.LoadError;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * 插件行为接口
 * <p>
 * 行为接口必须被具体的插件业务扩展，否则失去插件的意。
 * 在 Frontia 内 PluginBehavior 被泛型 B 替代(<B extends PluginBehavior>)。
 */
public interface PluginBehavior {

    class ProxyHandler implements InvocationHandler {

        private static final String TAG = "plugin.proxy";
        private final PluginBehavior mBehavior;

        ProxyHandler(PluginBehavior behavior) {
            mBehavior = behavior;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Proxy invoke, class = " + proxy.getClass().getName()
                        + ", method = " + method.getName());
            }

            try {
                return method.invoke(mBehavior, args);

            } catch (Throwable e) {
                Log.w(TAG, "Invoke plugin method fail.");
                Log.w(TAG, e);
                if (BuildConfig.DEBUG) {
                    throw new RuntimeException(e);
                }
            }

            // The plugin behavior interface is not capable with the Impl of the plugin.
            // Return the default value of the given type as to abort crash here.
            return getDefaultValue(method.getReturnType());
        }

        private static Object getDefaultValue(Class<?> type) {

            // Check primitive type.
            if (type == Boolean.class || type == boolean.class) {
                return false;
            } else if (type == Byte.class || type == byte.class) {
                return 0;
            } else if (type == Character.class || type == char.class) {
                return '\u0000';
            } else if (type == Short.class || type == short.class) {
                return 0;
            } else if (type == Integer.class || type == int.class) {
                return 0;
            } else if (type == Long.class || type == long.class) {
                return 0L;
            } else if (type == Float.class || type == float.class) {
                return 0.0F;
            } else if (type == Double.class || type == double.class) {
                return 0.0D;
            } else if (type == Void.class || type == void.class) {
                return null;
            }

            return null;
        }

        @Nullable
        public static PluginBehavior getProxy(PluginBehavior behavior) throws LoadError {
            try {
                Class<? extends PluginBehavior> behaviorClass = behavior.getClass();
                Class<?>[] interfaces = behaviorClass.getInterfaces();

                if (interfaces != null && interfaces.length > 0) {
                    for (Class clazz : interfaces) {
                        if (PluginBehavior.class.isAssignableFrom(clazz)) {
                            Object instance = newProxyInstance(behaviorClass.getClassLoader(),
                                    new Class[]{clazz},
                                    new ProxyHandler(behavior));
                            return (PluginBehavior) instance;
                        }
                    }
                }
            } catch (Throwable e) {
                Log.w(TAG, "Create behavior proxy object fail.");
                Log.w(TAG, e);
                return null;
            }

            throw new LoadError("Create behavior proxy fail.", PluginError.ERROR_LOA_INVOKE_PROXY);
        }
    }
}
