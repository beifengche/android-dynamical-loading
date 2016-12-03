/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

import moe.studio.frontia.Internals.ApkUtils;
import moe.studio.frontia.Internals.FileUtils;
import moe.studio.frontia.core.PluginInstaller;
import moe.studio.frontia.core.PluginManager;
import moe.studio.frontia.ext.PluginError.InstallError;

import static moe.studio.frontia.Internals.SignatureUtils;
import static moe.studio.frontia.ext.PluginError.ERROR_INS_CAPACITY;
import static moe.studio.frontia.ext.PluginError.ERROR_INS_INSTALL;
import static moe.studio.frontia.ext.PluginError.ERROR_INS_INSTALL_PATH;
import static moe.studio.frontia.ext.PluginError.ERROR_INS_NOT_FOUND;
import static moe.studio.frontia.ext.PluginError.ERROR_INS_SIGNATURE;

/**
 * 插件安装器，提供插件的安装和卸载策略。
 * <p>
 * 负责校验插件的安全, 获取插件的安装路径以及管理已安装的插件。
 */
class PluginInstallerImpl implements PluginInstaller {

    private static final String TAG = "plugin.installer";
    private static final int MIN_REQUIRED_CAPACITY = 10 * 1000 * 1000; // 10MB

    private final File mRooDir;
    private final File mCacheDir;
    private final Context mContext;
    private final PluginManager mManager;

    PluginInstallerImpl(Context context, PluginManager manager) {
        mContext = context.getApplicationContext();
        mManager = manager;
        mRooDir = mContext.getDir(manager.getSetting().getRootDir(), Context.MODE_PRIVATE);

        File cache = mContext.getExternalCacheDir();
        if (cache == null || cache.getFreeSpace() < MIN_REQUIRED_CAPACITY) {
            cache = mContext.getCacheDir();
        }
        mCacheDir = cache;
    }

    private boolean isDebugMode() {
        return mManager.getSetting().isDebugMode();
    }

    @Override
    public boolean checkSafety(String apkPath) {
        Logger.d(TAG, "Check plugin's validation.");

        if (!FileUtils.exist(apkPath)) {
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

        if (mManager.getSetting().useCustomSignature()) {
            // Check if the plugin's signatures are the same with the given one.
            if (!SignatureUtils.isSignaturesSame(mManager.getSetting()
                    .getCustomSignature(), pluginSignatures)) {
                Logger.w(TAG, "Plugin's signatures are different, path = " + apkPath);
                return false;
            }
        } else {
            // Check if the plugin's signatures are the same with current app.
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
    public boolean checkSafety(String apkPath, boolean deleteIfInvalid) {
        if (checkSafety(apkPath)) {
            return true;
        }

        if (deleteIfInvalid) {
            delete(apkPath);
        }
        return false;
    }

    @Override
    public boolean checkSafety(String pluginId, String version, boolean deleteIfInvalid) {
        String pluginPath = getInstallPath(pluginId, version);
        if (checkSafety(pluginPath)) {
            return true;
        }

        if (deleteIfInvalid) {
            delete(pluginId, version);
        }
        return false;
    }

    @Override
    public void delete(String apkPath) {
        FileUtils.delete(apkPath);
    }

    @Override
    public void delete(String pluginId, String version) {
        FileUtils.delete(getInstallPath(pluginId, version));
    }

    @Override
    public void deletePlugins(String pluginId) {
        File file = new File(getPluginPath(pluginId));
        if (!file.exists()) {
            Logger.w(TAG, "Delete fail, dir not found, path = " + file.getAbsolutePath());
            return;
        }

        FileUtils.delete(file);
    }

    @Override
    public void checkCapacity() throws IOException {
        if (mRooDir.getFreeSpace() < MIN_REQUIRED_CAPACITY) {
            throw new IOException("No enough capacity.");
        }
    }

    @Override
    public File createTempFile(String prefix) throws IOException {
        return File.createTempFile(prefix, mManager.getSetting().getTempFileSuffix(), mCacheDir);
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
    public String getInstallPath(String pluginId, String version) {
        return getRootPath() + File.separator + pluginId + File.separator + version
                + File.separator + mManager.getSetting().getPluginName();
    }

    @Override
    @Nullable
    public String getInstallPath(String apkPath) {
        PackageInfo packageInfo = getPackageInfo(apkPath);
        if (packageInfo == null) {
            return null;
        }
        return getInstallPath(packageInfo.packageName, String.valueOf(packageInfo.versionCode));
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isInstalled(String pluginId, String version) {
        if (mManager.getSetting().ignoreInstalledPlugin()) {
            // Force to use external plugin by ignoring installed one.
            return false;
        }
        return checkSafety(pluginId, version, true);
    }

    @Override
    public boolean isInstalled(String apkPath) {
        if (mManager.getSetting().ignoreInstalledPlugin()) {
            // Force to use external plugin by ignoring installed one.
            return false;
        }
        PackageInfo packageInfo = getPackageInfo(apkPath);
        return packageInfo != null &&
                checkSafety(packageInfo.packageName,
                        String.valueOf(packageInfo.versionCode),
                        true);
    }

    @Override
    public String install(String apkPath) throws InstallError {
        Logger.i(TAG, "Install plugin, path = " + apkPath);
        File apkFile = new File(apkPath);

        if (!apkFile.exists()) {
            Logger.w(TAG, "Plugin path not exist");
            throw new InstallError("Plugin file not exist.", ERROR_INS_NOT_FOUND);
        }

        // Check plugin's signatures.
        Logger.v(TAG, "Check plugin's signatures.");
        if (!mManager.getInstaller().checkSafety(apkPath, true)) {
            Logger.w(TAG, "Check plugin's signatures fail.");
            throw new InstallError("Check plugin's signatures fail.",
                    ERROR_INS_SIGNATURE);
        }

        // Get install path.（"<id>/<version>/base-1.apk"）
        String installPath = mManager.getInstaller().getInstallPath(apkPath);
        if (TextUtils.isEmpty(installPath)) {
            throw new InstallError("Can not get install path.", ERROR_INS_INSTALL_PATH);
        }
        Logger.v(TAG, "Install path = " + installPath);

        // Install plugin file to install path.
        // Check if the plugin has already been installed.
        File destApk = new File(installPath);

        if (destApk.exists()) {
            if (!mManager.getSetting().ignoreInstalledPlugin() &&
                    mManager.getInstaller().checkSafety(destApk.getAbsolutePath(), true)) {
                Logger.d(TAG, "Plugin has been already installed.");
                return installPath;
            }
            Logger.d(TAG, "Ignore installed plugin.");
        }

        Logger.d(TAG, "Install plugin, from = " + apkPath + ", to = " + installPath);

        if (apkFile.renameTo(destApk)) {
            Logger.d(TAG, "Rename success.");
            return installPath;
        }

        try {
            checkCapacity();
        } catch (IOException e) {
            Logger.w(TAG, e);
            throw new InstallError(e, ERROR_INS_CAPACITY);
        }

        try {
            Logger.d(TAG, "Rename fail, try copy file.");
            FileUtils.copyFile(apkFile, destApk);

        } catch (IOException e) {
            Logger.w(TAG, e);
            throw new InstallError(e, ERROR_INS_INSTALL);
        }

        return installPath;
    }

    @Override
    public PackageInfo getPackageInfo(String apkPath) {
        return ApkUtils.getPackageInfo(mContext, apkPath);
    }

}
