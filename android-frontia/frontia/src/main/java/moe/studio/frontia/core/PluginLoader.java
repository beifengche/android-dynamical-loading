package moe.studio.frontia.core;

import android.support.annotation.NonNull;

import moe.studio.frontia.bridge.plugin.BaseBehaviour;
import moe.studio.frontia.error.IllegalPluginException;
import moe.studio.frontia.error.LoadPluginException;

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
    PluginRequest loadPlugin(@NonNull PluginRequest request);

    /**
     * 加载插件
     *
     * @param plugin 插件
     * @return 插件
     * @throws LoadPluginException
     */
    Plugin loadPlugin(Plugin plugin) throws LoadPluginException;

    /**
     * 获取插件
     *
     * @param packageName 插件ID
     * @return 插件
     */
    Plugin getPluginPackage(String packageName);

    /**
     * 保存插件
     *
     * @param id     插件ID
     * @param plugin 插件
     */
    void putPluginPackage(String id, Plugin plugin);

    /**
     * 加载插件中指定的类
     *
     * @param plugin    插件
     * @param className 类名
     * @return 目标类
     * @throws IllegalPluginException
     */
    Class loadClass(@NonNull Plugin plugin, String className) throws IllegalPluginException;

    /**
     * 获取插件的行为接口
     *
     * @param plugin 插件
     * @return 插件行为接口(可根据需要向下转型)
     * @throws IllegalPluginException
     */
    BaseBehaviour getBehaviour(Plugin plugin) throws IllegalPluginException;
}
