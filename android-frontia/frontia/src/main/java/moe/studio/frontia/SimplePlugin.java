package moe.studio.frontia;

import android.content.Context;

import java.io.File;
import java.io.IOException;

import moe.studio.frontia.core.Plugin;
import moe.studio.frontia.error.LoadPluginException;

import static moe.studio.frontia.Internals.FileUtils;
import static moe.studio.frontia.Internals.ApkUtils;

/**
 * 简单的APK插件，用于一般的SDK插件
 */
public abstract class SimplePlugin extends Plugin {
    private static final String TAG = "plugin.simple.package";

    public SimplePlugin(String pluginPath) {
        super(pluginPath);
    }

    @Override
    protected String installPlugin(Context context, String apkPath) throws LoadPluginException {
        Logger.d(TAG, "Install plugin to internal path.");

        File apkFile = new File(apkPath);
        checkApkFile(apkFile);

        if (mPackageInfo == null) {
            if ((mPackageInfo = ApkUtils.getPackageInfo(context, apkPath)) == null) {
                throw new LoadPluginException("Can not get plugin info");
            }
        }

        // Get install path.（"<id>/<version>/base-1.apk"）
        mInstallPath = mManager.getInstaller()
                .getPluginInstallPath(mPackageInfo.packageName,
                        String.valueOf(mPackageInfo.versionCode));

        Logger.v(TAG, "Install path = " + mInstallPath);

        // Install plugin file to install path.
        File destApk = new File(mInstallPath);
        if (destApk.exists() && mManager.getInstaller().checkPluginSafety(destApk.getAbsolutePath())) {
            Logger.d(TAG, "Plugin has been already installed.");

        } else {
            Logger.d(TAG, "Install plugin.");
            try {
                FileUtils.copyFile(apkFile, destApk);
            } catch (IOException e) {
                e.printStackTrace();
                throw new LoadPluginException("Install plugin fail.", e);
            }
        }

        return mInstallPath;
    }

    @Override
    public Plugin loadPlugin(Context context, String apkPath) throws LoadPluginException {
        Logger.d(TAG, "Create plugin package entity.");

        File apkFile = new File(apkPath);
        checkApkFile(apkFile);

        if (!apkPath.startsWith(File.separator + "data" + File.separator + "data")) {
            Logger.w(TAG, "Apk path is not executable, path = " + apkPath);
        }

        try {
            mOptDexDir = createOptimizedDexDir(apkFile);
        } catch (IOException e) {
            throw new LoadPluginException("Create opt dex dir fail.", e);
        }

        if (Logger.DEBUG) {
            Logger.i(TAG, "-");
            Logger.i(TAG, "Create ClassLoader :");
            Logger.i(TAG, "apkPath = " + apkPath);
            Logger.i(TAG, "mOptDexDir = " + mOptDexDir.getAbsolutePath());
            Logger.i(TAG, "mSoLibDir = "
                    + (mSoLibDir == null ? "null" : mSoLibDir.getAbsolutePath()));
            if (mSoLibDir != null) {
                FileUtils.dumpFiles(mSoLibDir);
            }
            Logger.i(TAG, "-");
        }
        mClassLoader = ApkUtils.createClassLoader(
                context,
                apkPath,
                mOptDexDir.getAbsolutePath(),
                mSoLibDir == null ? null : mSoLibDir.getAbsolutePath(),
                false);
        mAssetManager = ApkUtils.createAssetManager(apkPath);
        mResources = ApkUtils.createResources(context, mAssetManager);
        mIsLoaded = true;
        return this;
    }

    protected void checkApkFile(File apkFile) throws LoadPluginException {
        if (apkFile == null || !apkFile.exists()) {
            Logger.w(TAG, "Apk file not exist.");
            throw new LoadPluginException("Apk file not exist.");
        }
    }

    protected File createOptimizedDexDir(File apkFile) throws IOException {
        File file = new File(apkFile.getParentFile(), mSetting.getOptimizedDexDir());
        FileUtils.checkCreateDir(file);
        return file;
    }
}
