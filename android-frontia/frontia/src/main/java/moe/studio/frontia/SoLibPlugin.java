/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import moe.studio.frontia.core.PluginBehavior;

import static moe.studio.frontia.Internals.FileUtils;
import static moe.studio.frontia.Internals.SoLibUtils;
import static moe.studio.frontia.ext.PluginError.ERROR_LOA_SO_DIR;
import static moe.studio.frontia.ext.PluginError.ERROR_LOA_SO_INSTALL;
import static moe.studio.frontia.ext.PluginError.LoadError;

/**
 * 带有SO库的APK插件，用于满足带有SO库的SDK插件化。
 */
@SuppressWarnings("WeakerAccess")
public abstract class SoLibPlugin<B extends PluginBehavior> extends SimplePlugin<B> {

    public static final String TAG = "plugin.simple.SoLib";

    public SoLibPlugin(String apkPath) {
        super(apkPath);
    }

    @Override
    public SoLibPlugin loadPlugin(Context context, String installPath) throws LoadError {
        Logger.d(TAG, "Install plugin so libs.");

        File apkFile = new File(installPath);
        checkApkFile(apkFile);

        try {
            mSoLibDir = createSoLibDir(apkFile);
        } catch (IOException e) {
            throw new LoadError(e, ERROR_LOA_SO_DIR);
        }

        try {
            installSoLib(apkFile, mSoLibDir);
        } catch (IOException e) {
            throw new LoadError(e, ERROR_LOA_SO_INSTALL);
        }

        super.loadPlugin(context, installPath);
        return this;
    }

    protected File createSoLibDir(File apkFile) throws IOException {
        File file = new File(apkFile.getParentFile(), mSetting.getSoLibDir());
        FileUtils.checkCreateDir(file);
        return file;
    }

    private void installSoLib(File apkFile, File soLibDir) throws IOException {
        Logger.d(TAG, "Install plugin so libs, destDir = " + soLibDir);

        // TODO: 2016/11/30 Optimize so libs installation.
        File tempDir = new File(soLibDir.getParentFile(), mSetting.getTempSoLibDir());
        FileUtils.checkCreateDir(tempDir);
        Set<String> soList = SoLibUtils.extractSoLib(apkFile, tempDir);

        if (soList != null) {
            for (String soName : soList) {
                SoLibUtils.copySoLib(tempDir, soName, soLibDir);
            }
            FileUtils.delete(tempDir);
        }
    }
}
