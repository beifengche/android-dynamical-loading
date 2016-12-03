package moe.studio.frontia.core;

import android.support.annotation.NonNull;

import moe.studio.frontia.Frontia;
import moe.studio.frontia.bridge.plugin.BaseBehaviour;
import moe.studio.frontia.error.IllegalPluginException;

/**
 * 插件管理器
 */

public interface PluginManager {
    /**
     * 同步加载一个插件
     *
     * @param request 插件请求
     * @param mode    加载模式, 参考{@linkplain Frontia.Mode}
     * @return 当前插件请求
     */
    PluginRequest add(@NonNull PluginRequest request, int mode);

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
     * 加载插件中指定的类
     */
    Class loadClass(@NonNull Plugin plugin, String className) throws IllegalPluginException;

    /**
     * 获取插件的行为接口
     */
    BaseBehaviour getBehaviour(Plugin plugin) throws IllegalPluginException;

    /**
     * 插件加载的模式
     */
    class Mode {
        /**
         * 更新插件, 从远程下载最新版本的插件并拷贝到对应的安装路径
         */
        public static final int MODE_UPDATE = 0x0001;
        /**
         * 从对应的安装路径加载插件
         */
        public static final int MODE_LOAD = 0x0010;
    }
}
