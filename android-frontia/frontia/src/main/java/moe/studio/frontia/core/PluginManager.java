/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia.core;

import moe.studio.frontia.ext.PluginCallback;
import moe.studio.frontia.ext.PluginSetting;

import static moe.studio.frontia.ext.PluginError.LoadError;

/**
 * 插件管理器
 */

public interface PluginManager {

    /**
     * 获取插件设置类
     */
    PluginSetting getSetting();

    /**
     * 获取当前插件加载器
     */
    PluginLoader getLoader();

    /**
     * 获取当前插件更新器
     */
    PluginUpdater getUpdater();

    /**
     * 获取当前插件安装器
     */
    PluginInstaller getInstaller();

    /**
     * 获取当前插件回调通知器
     */
    PluginCallback getCallback();

    Class getClass(Class<? extends Plugin> clazz, String className) throws LoadError;

    <B extends PluginBehavior, P extends Plugin<B>> B getBehavior(P clazz) throws LoadError;

    <B extends PluginBehavior, P extends Plugin<B>> P getPlugin(P clazz);

    void addLoadedPlugin(Class<? extends PluginBehavior> clazz, Plugin plugin);

}
