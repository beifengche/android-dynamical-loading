package moe.studio.frontia.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import dalvik.system.DexClassLoader;
import moe.studio.frontia.BuildConfig;
import moe.studio.frontia.bridge.plugin.BaseBehaviour;
import moe.studio.frontia.error.IllegalPluginException;
import moe.studio.frontia.error.LoadPluginException;

/**
 * Plugin entity class.
 * 君の望んでいたすべてはここにいる。
 */
public abstract class Plugin {

    public static final String TAG = "plugin.abs.package";

    protected boolean mIsLoaded;
    protected File mOptDexDir;
    protected File mSoLibDir;
    protected PackageInfo mPackageInfo;
    protected Resources mResources;
    protected AssetManager mAssetManager;
    protected DexClassLoader mClassLoader;
    protected String mInstallPath;
    protected PluginManager mManager;
    protected PluginSetting mSetting;
    protected final String mApkPath;

    public Plugin(String apkPath) {
        mApkPath = apkPath;
        mInstallPath = apkPath;
        mIsLoaded = false;
        mSetting = new PluginSetting.Builder()
                .setDebugMode(BuildConfig.DEBUG)
                .ignoreInstalledPlugin(BuildConfig.DEBUG)
                .build();
    }

    @Override
    public String toString() {
        return "Plugin {" +
                "Id = " + (mPackageInfo == null ? "null" : mPackageInfo.packageName) +
                ", ApkPath = '" + mApkPath + '\'' +
                '}';
    }

    public Plugin attach(@NonNull PluginManager manager) {
        mManager = manager;
        mSetting = manager.getSetting();
        return this;
    }

    /**
     * 插件是否已经加载
     */
    public boolean isLoaded() {
        return mIsLoaded;
    }

    /**
     * 获取插件外部路径, 如果没有, 返回Null
     */
    public String getApkPath() {
        return mApkPath;
    }

    /**
     * 获取插件安装路径(内部), 如果没有, 返回Null
     */
    public String getInstallPath() {
        return mInstallPath;
    }

    /**
     * 设置插件安装路径
     */
    public void setInstallPath(String installPath) {
        mInstallPath = installPath;
    }

    /**
     * 获取插件PackageInfo
     */
    @Nullable
    public PackageInfo getPackageInfo() {
        return mPackageInfo;
    }

    /**
     * 设置插件PackageInfo
     */
    public void setPackageInfo(PackageInfo packageInfo) {
        mPackageInfo = packageInfo;
    }

    /**
     * 获取插件so库安装目录
     */
    @Nullable
    public File getSoLibDir() {
        return mSoLibDir;
    }

    /**
     * 获取插件OptDex存放路径
     */
    @Nullable
    public File getOptimizedDexDir() {
        return mOptDexDir;
    }

    /**
     * 获取插件的ClassLoader
     */
    @Nullable
    public DexClassLoader getClassLoader() {
        return mClassLoader;
    }

    /**
     * 获取插件的AssetManager
     */
    @Nullable
    public AssetManager getAssetManager() {
        return mAssetManager;
    }

    /**
     * 获取插件的Resources
     */
    @Nullable
    public Resources getResources() {
        return mResources;
    }

    /**
     * 安装插件到安装目录, 并加载插件
     */
    public Plugin loadPlugin(Context context) throws LoadPluginException {
        return loadPlugin(context, installPlugin(context, mApkPath));
    }

    /**
     * 安装指定路径上的插件，其实就是把插件复制到特定的内部目录
     */
    protected abstract String installPlugin(Context context, String packagePath)
            throws LoadPluginException;

    /**
     * 加载指定路径上的插件，具体实现根据需求的不同由不同的继承类完成
     */
    public abstract Plugin loadPlugin(Context context, String packagePath)
            throws LoadPluginException;

    /**
     * 获取插件的行为接口，用于控制插件
     *
     * @param args 初始化插件行为接口需要用到的参数（这里用到了不定数参数，是否能优化？）
     * @return 行为接口
     */
    public abstract BaseBehaviour getPluginBehaviour(Object... args)
            throws IllegalPluginException;
}
