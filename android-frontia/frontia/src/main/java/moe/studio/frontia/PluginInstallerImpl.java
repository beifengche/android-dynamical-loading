/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package moe.studio.frontia;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

import moe.studio.frontia.core.PluginInstaller;
import moe.studio.frontia.core.PluginManager;
import moe.studio.frontia.core.PluginSetting;

import static moe.studio.frontia.Internals.SignatureUtils;

/**
 * 插件安装器，提供插件的安装和卸载策略。
 * <p>
 * 负责校验插件的安全, 获取插件的安装路径以及管理已安装的插件。
 */
class PluginInstallerImpl implements PluginInstaller {

    private static final String TAG = "plugin.installer";
    private static final String EXT_TEMP_FILE = ".tmp";
    private static final int MIN_REQUIRED_CAPACITY = 10 * 1000 * 1000; // 10MB

    private final File mRooDir;
    private final File mCacheDir;
    private final Context mContext;
    private final PluginSetting mSetting;
    private final PluginManager mManager;

    PluginInstallerImpl(Context context, PluginManager manager) {
        mContext = context.getApplicationContext();
        mManager = manager;
        mSetting = manager.getSetting();
        mRooDir = mContext.getDir(mSetting.getRootDir(), Context.MODE_PRIVATE);
        File cache = mContext.getExternalCacheDir();
        if (cache == null || cache.getFreeSpace() < MIN_REQUIRED_CAPACITY) {
            cache = mContext.getCacheDir();
        }
        mCacheDir = cache;
    }

    private boolean isDebugMode() {
        return mSetting.isDebugMode();
    }

    @Override
    public boolean checkPluginSafety(String apkPath) {
        Logger.d(TAG, "Check plugin's validation.");

        if (TextUtils.isEmpty(apkPath) || !(new File(apkPath).exists())) {
            Logger.w(TAG, "Plugin not found, path = " + String.valueOf(apkPath));
            return false;
        }

        if (isDebugMode()) {
            Logger.d(TAG, "Debug mode, skip validation, path = " + apkPath);
            return true;
        }

        Signature[] pluginSignatures = SignatureUtils.getSignatures(mContext, apkPath);
        if (pluginSignatures == null) {
            Logger.w(TAG, "Can not get plugin's signatures , path = " + apkPath);
            return false;
        }

        if (isDebugMode()) {
            Logger.v(TAG, "Dump plugin signatures:");
            SignatureUtils.printSignature(pluginSignatures);
        }

        if (mSetting.useCustomSignature()) {
            // 方案1 : 检验插件的签名是不是指定签名。
            if (!SignatureUtils.isSignaturesSame(mSetting.getCustomSignature(), pluginSignatures)) {
                Logger.w(TAG, "Plugin's signatures are different, path = " + apkPath);
                return false;
            }
        } else {
            // 方案2 : 检验插件的签名和宿主的签名是否一致。
            // 可选步骤，验证插件APK证书是否和宿主程序证书相同
            // 证书中存放的是公钥和算法信息，而公钥和私钥是1对1的
            // 公钥相同意味着是同一个作者发布的程序
            Signature[] mainSignatures = SignatureUtils.getSignatures(mContext);
            if (!SignatureUtils.isSignaturesSame(mainSignatures, pluginSignatures)) {
                Logger.w(TAG, "Plugin's signatures differ from the app's.");
                return false;
            }
        }
        Logger.v(TAG, "Check plugin's signatures success, path = " + apkPath);
        return true;
    }

    @Override
    public boolean checkPluginSafety(String apkPath, boolean deleteIfInvalid) {
        if (checkPluginSafety(apkPath)) {
            return true;
        }

        if (deleteIfInvalid) {
            deletePlugin(apkPath);
        }
        return false;
    }

    @Override
    public boolean checkPluginSafety(String pluginId, String version, boolean deleteIfInvalid) {
        String pluginPath = getPluginInstallPath(pluginId, version);
        if (checkPluginSafety(pluginPath)) {
            return true;
        }

        if (deleteIfInvalid) {
            deletePlugin(pluginId, version);
        }
        return false;
    }

    @Override
    public void deletePlugin(String apkPath) {
        Internals.FileUtils.deleteQuietly(new File(apkPath));
    }

    @Override
    public void deletePlugin(String pluginId, String version) {
        Internals.FileUtils.deleteQuietly(new File(getPluginInstallPath(pluginId, version)));
    }

    @Override
    public void deletePlugins(String pluginId) {
        File file = new File(getPluginPath(pluginId));
        if (!file.exists()) {
            Logger.w(TAG, "Delete fail, dir not found, path = " + file.getAbsolutePath());
            return;
        }

        Internals.FileUtils.deleteQuietly(file);
    }

    @Override
    public File createTempFile(String prefix) throws IOException {
        return File.createTempFile(prefix, EXT_TEMP_FILE, mCacheDir);
    }

    /**
     * 获取插件根目录。
     *
     * @return 所有插件存放的根目录
     */
    @Override
    public String getRootPath() {
        return mRooDir.getAbsolutePath();
    }

    /**
     * 获取指定插件的根目录。
     *
     * @param pluginId 插件包名
     * @return 指定插件的根目录
     */
    @Override
    public String getPluginPath(@NonNull String pluginId) {
        return getRootPath() + File.separator + pluginId;
    }

    @Override
    public String getPluginInstallPath(String pluginId, String version) {
        return getRootPath() + File.separator + pluginId + File.separator + version
                + File.separator + mSetting.getPluginName();
    }

    @Override
    public String getPluginInstallPath(String apkPath) {
        PackageInfo packageInfo = getPluginInfo(apkPath);
        if (packageInfo == null) {
            return null;
        }
        return getPluginInstallPath(packageInfo.packageName, String.valueOf(packageInfo.versionCode));
    }

    @Override
    public boolean isPluginInstalled(String pluginId, String version) {
        if (mSetting.ignoreInstalledPlugin()) {
            // Force to use external plugin by ignoring installed one.
            return false;
        }
        return checkPluginSafety(pluginId, version, true);
    }

    @Override
    public boolean isPluginInstalled(String apkPath) {
        if (mSetting.ignoreInstalledPlugin()) {
            // Force to use external plugin by ignoring installed one.
            return false;
        }
        PackageInfo packageInfo = getPluginInfo(apkPath);
        return packageInfo != null && checkPluginSafety(packageInfo.packageName,
                String.valueOf(packageInfo.versionCode),
                true);
    }

    @Override
    public PackageInfo getPluginInfo(String apkPath) {
        return Internals.ApkUtils.getPackageInfo(mContext, apkPath);
    }

}
