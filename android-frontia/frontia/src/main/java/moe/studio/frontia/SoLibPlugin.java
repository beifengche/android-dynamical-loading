package moe.studio.frontia;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import moe.studio.frontia.error.LoadPluginException;

import static moe.studio.frontia.Internals.FileUtils;
import static moe.studio.frontia.Internals.SoLibUtils;

/**
 * 带有SO库的APK插件，用于满足带有SO库的SDK插件化。
 */
public abstract class SoLibPlugin extends SimplePlugin {
    public static final String TAG = "plugin.simple.SoLib";

    public SoLibPlugin(String apkPath) {
        super(apkPath);
    }

    @Override
    public SoLibPlugin loadPlugin(Context context, String apkPath) throws LoadPluginException {
        Logger.d(TAG, "Install plugin so libs.");

        File apkFile = new File(apkPath);
        checkApkFile(apkFile);

        try {
            mSoLibDir = createSoLibDir(apkFile);
        } catch (IOException e) {
            throw new LoadPluginException("Create SoLib dir fail.", e);
        }

        try {
            installSoLib(apkFile, mSoLibDir);
        } catch (IOException e) {
            throw new LoadPluginException("Install soLibs fail.", e);
        }

        super.loadPlugin(context, apkPath);
        return this;
    }

    protected File createSoLibDir(File apkFile) throws IOException {
        File file = new File(apkFile.getParentFile(), mSetting.getSoLibDir());
        FileUtils.checkCreateDir(file);
        return file;
    }

    private void installSoLib(File apkFile, File soLibDir) throws IOException {
        Logger.d(TAG, "Install plugin so libs, destDir = " + soLibDir);

        // 解压SO库，并根据当前CPU的类型选择正确的SO库。
        // 有必要每次都重新解压一边SO库吗，目前看起来这样做是比较便捷的。
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
