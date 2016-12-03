/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia.core;

import android.support.annotation.NonNull;

import moe.studio.frontia.ext.PluginError;
import moe.studio.frontia.ext.PluginError.LoadError;

/**
 * 插件加载器
 */
public interface PluginLoader {

    /**
     * 加载插件
     *
     * @param request 插件请求
     * @return 插件请求
     */
    PluginRequest load(@NonNull PluginRequest request);

    /**
     * 加载插件
     * <p>
     * 前提是你得有个可用的插件{@linkplain Plugin}，没有的话看看{@linkplain #load(PluginRequest)}。
     *
     * @param plugin 插件
     * @return 插件
     * @throws LoadError 插件加载异常
     */
    Plugin load(Plugin plugin) throws LoadError, PluginError.InstallError;

    /**
     * 获取插件
     *
     * @param packageName 插件ID
     * @return 插件
     */
    Plugin getPlugin(String packageName);

    /**
     * 保存插件
     *
     * @param id     插件ID
     * @param plugin 插件
     */
    void putPlugin(String id, Plugin plugin);

    /**
     * 加载插件中指定的类
     *
     * @param plugin    插件
     * @param className 类名
     * @return 目标类
     * @throws LoadError 加载插件类异常
     */
    Class loadClass(@NonNull Plugin plugin, String className) throws LoadError;

    PluginBehavior createPluginBehavior(Plugin plugin);
}
