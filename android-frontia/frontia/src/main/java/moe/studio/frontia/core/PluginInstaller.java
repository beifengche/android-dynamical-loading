package moe.studio.frontia.core;

import android.content.pm.PackageInfo;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

/**
 * 插件安装器
 */
public interface PluginInstaller {
    /**
     * 插件插件是否安全
     */
    boolean checkPluginSafety(String apkPath);

    /**
     * 插件插件是否安全
     *
     * @param apkPath         插件路径
     * @param deleteIfInvalid 插件不安全时是否删除插件
     * @return 安全与否
     */
    boolean checkPluginSafety(String apkPath, boolean deleteIfInvalid);

    /**
     * 插件插件是否安全
     *
     * @param pluginId        插件ID
     * @param version         插件版本
     * @param deleteIfInvalid 插件不安全时是否删除插件
     * @return 安全与否
     */
    boolean checkPluginSafety(String pluginId, String version, boolean deleteIfInvalid);

    /**
     * 删除插件
     */
    void deletePlugin(String apkPath);

    /**
     * 删除插件
     *
     * @param pluginId 插件ID
     * @param version  插件版本
     */
    void deletePlugin(String pluginId, String version);

    /**
     * 删除指定ID的插件的所有版本
     */
    void deletePlugins(String pluginId);

    /**
     * 创建一个临时文件
     *
     * @param prefix 前缀
     * @return 临时文件
     * @throws IOException
     */
    File createTempFile(String prefix) throws IOException;

    /**
     * 获取所有插件的根目录
     */
    String getRootPath();

    /**
     * 删除指定ID的插件的根目录
     */
    String getPluginPath(@NonNull String pluginId);

    /**
     * 获取指定插件版本对应的安装路径
     *
     * @param pluginId 插件ID
     * @param version  插件版本
     * @return 安装路径
     */
    String getPluginInstallPath(String pluginId, String version);

    /**
     * 获取插件安装路径
     *
     * @param apkPath 插件包路径
     * @return 安装路径
     */
    String getPluginInstallPath(String apkPath);

    /**
     * 判断指定版本的插件是否已经安装
     *
     * @param pluginId 插件ID
     * @param version  插件版本
     * @return 安装与否
     */
    boolean isPluginInstalled(String pluginId, String version);

    /**
     * 判断插件是否已经安装
     */
    boolean isPluginInstalled(String apkPath);

    /**
     * 获取插件的PackageInfo
     */
    PackageInfo getPluginInfo(String apkPath);
}
