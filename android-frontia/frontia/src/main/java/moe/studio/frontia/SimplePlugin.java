/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia;

import android.content.Context;

import java.io.File;
import java.io.IOException;

import moe.studio.frontia.core.Plugin;
import moe.studio.frontia.core.PluginBehavior;
import moe.studio.frontia.ext.PluginError.LoadError;

import static moe.studio.frontia.Internals.ApkUtils;
import static moe.studio.frontia.Internals.FileUtils;
import static moe.studio.frontia.Logger.DEBUG;
import static moe.studio.frontia.ext.PluginError.ERROR_LOA_NOT_FOUND;
import static moe.studio.frontia.ext.PluginError.ERROR_LOA_OPT_DIR;

/**
 * 简单的APK插件，用于一般的SDK插件
 */
@SuppressWarnings("WeakerAccess")
public abstract class SimplePlugin<B extends PluginBehavior> extends Plugin<B> {

    private static final String TAG = "plugin.simple.package";

    public SimplePlugin(String pluginPath) {
        super(pluginPath);
    }

    @Override
    public Plugin loadPlugin(Context context, String installPath) throws LoadError {
        Logger.d(TAG, "Create plugin package entity.");

        File apkFile = new File(installPath);
        checkApkFile(apkFile);

        try {
            mOptDexDir = createOptimizedDexDir(apkFile);
        } catch (IOException e) {
            throw new LoadError(e, ERROR_LOA_OPT_DIR);
        }

        if (DEBUG) {
            Logger.i(TAG, "-");
            Logger.i(TAG, "Create ClassLoader :");
            Logger.i(TAG, "installPath = " + installPath);
            Logger.i(TAG, "mOptDexDir = " + mOptDexDir.getAbsolutePath());
            Logger.i(TAG, "mSoLibDir = "
                    + (mSoLibDir == null ? "null" : mSoLibDir.getAbsolutePath()));
            if (mSoLibDir != null) {
                FileUtils.dumpFiles(mSoLibDir);
            }
            Logger.i(TAG, "-");
        }

        // GO!
        mPackage.classLoader = ApkUtils.createClassLoader(
                context,
                installPath,
                mOptDexDir.getAbsolutePath(),
                mSoLibDir == null ? null : mSoLibDir.getAbsolutePath(),
                false);
        mPackage.assetManager = ApkUtils.createAssetManager(installPath);
        mPackage.resources = ApkUtils.createResources(context, mPackage.assetManager);

        setLoaded();
        return this;
    }

    protected void checkApkFile(File apkFile) throws LoadError {
        if (apkFile == null || !apkFile.exists()) {
            Logger.w(TAG, "Apk file not exist.");
            throw new LoadError("Apk file not exist.", ERROR_LOA_NOT_FOUND);
        }

        if (!apkFile.getAbsolutePath().trim().startsWith("/data/")) {
            String warn = "Apk file seems to locate in external path (not executable), " +
                    "path = " + apkFile.getAbsolutePath();
            Logger.w(TAG, warn);

            if (DEBUG) {
                throw new RuntimeException(warn);
            }
        }
    }

    protected File createOptimizedDexDir(File apkFile) throws IOException {
        File file = new File(apkFile.getParentFile(), mSetting.getOptimizedDexDir());
        FileUtils.checkCreateDir(file);
        return file;
    }

}
