/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package moe.studio.frontia;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import moe.studio.frontia.bridge.plugin.BaseBehaviour;
import moe.studio.frontia.core.Plugin;
import moe.studio.frontia.core.PluginErrors;
import moe.studio.frontia.core.PluginLoader;
import moe.studio.frontia.core.PluginManager;
import moe.studio.frontia.core.PluginRequest;
import moe.studio.frontia.error.IllegalPluginException;
import moe.studio.frontia.error.LoadPluginException;
import moe.studio.frontia.update.PluginController;

import static moe.studio.frontia.Internals.FileUtils;

/**
 * 插件加载器。
 * <p>
 * 用于加载指定路径上的插件, 同时保存已经加载过的插件。
 */
class PluginLoaderImpl implements PluginLoader {
    public static final String TAG = "plugin.loader";

    private final Context mContext;
    private final PluginManager mManager;
    private final Map<String, Plugin> mPackageHolder;

    PluginLoaderImpl(Context context, PluginManager manager) {
        mContext = context.getApplicationContext();
        mManager = manager;
        mPackageHolder = new HashMap<>();
    }

    /**
     * "加载插件"
     *
     * @param request 更新状态
     * @return 更新状态
     */
    @Override
    public PluginRequest loadPlugin(@NonNull final PluginRequest request) {
        Logger.d(TAG, "Loading plugin, id = " + request.getId());
        request.marker("Load");

        PluginController controller = request.getController();
        final PluginController.UpdateListener listener = controller.getListener();

        if (listener != null) {
            listener.onPreLoad(request.getState(), request);
        }

        if (request.getState() == PluginRequest.States.REQUEST_ALREADY_TO_LOAD_PLUGIN) {
            String path = request.getPluginPath();
            if (!TextUtils.isEmpty(path)) {
                // Plugin was updated, start to load plugin.
                Plugin plugin = request.createPlugin(path);
                plugin.attach(mManager);
                int retry = 0;
                Exception exception = null;
                while (true) {
                    if (controller.isCanceled()) {
                        request.onCancelRequest(mContext, request);
                        return request;
                    }
                    try {
                        plugin = loadPlugin(plugin);
                        break;
                    } catch (LoadPluginException e) {
                        exception = e;
                        e.printStackTrace();
                        try {
                            request.retry();
                            Logger.v(TAG, "Load fail, retry " + (retry++));
                            request.marker("Retry load " + retry);
                        } catch (PluginErrors.RetryError retryError) {
                            break;
                        }
                    }
                }

                if (exception != null) {
                    // Load fail.
                    request.switchState(PluginRequest.States.REQUEST_LOAD_PLUGIN_FAIL);
                    request.markException(exception);
                } else {
                    // Success.
                    request.setPlugin(plugin);
                    request.switchState(PluginRequest.States.REQUEST_LOAD_PLUGIN_SUCCESS);
                }

            } else {
                // Should not have this state.
                request.switchState(PluginRequest.States.REQUEST_WTF);
            }
        }

        // Call back.
        if (listener != null) {
            if (mManager instanceof Frontia) {
                Frontia frontia = (Frontia) mManager;
                frontia.getCallbackHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onPostLoad(request.getState(), request);
                    }
                });
            }
        }
        return request;
    }

    @Override
    public Plugin loadPlugin(Plugin plugin) throws LoadPluginException {
        String apkPath = plugin.getApkPath();
        Logger.d(TAG, "Loading plugin, path = " + apkPath);

        if (TextUtils.isEmpty(apkPath) || !(new File(apkPath).exists())) {
            Logger.w(TAG, "Plugin path not exist");
            throw new LoadPluginException("Plugin file not exist.");
        }

        if (mManager.getInstaller().isPluginInstalled(apkPath)) {
            // The current version has been installed before.
            Logger.v(TAG, "The current version has been installed before.");
            String installPath = mManager.getInstaller().getPluginInstallPath(apkPath);
            PackageInfo packageInfo = Internals.ApkUtils.getPackageInfo(mContext, installPath);

            if (packageInfo != null) {
                plugin.setPackageInfo(packageInfo);
                plugin.setInstallPath(installPath);

                Plugin pluginPackage = getPluginPackage(packageInfo.packageName);
                if (pluginPackage != null) {
                    // The current plugin has been loaded.
                    Logger.v(TAG, "The current plugin has been loaded, get it from holder, id = "
                            + packageInfo.packageName);
                    return pluginPackage;
                }

                // Load plugin from installed path.
                Logger.v(TAG, "Load plugin from installed path.");
                plugin = plugin.loadPlugin(mContext, plugin.getInstallPath());
                putPluginPackage(packageInfo.packageName, plugin);
                return plugin;
            } else {
                Logger.w(TAG, "Can not get installed plugin's packageInfo, try target plugin.");
                // throw new LoadPluginException("Can not get plugin info.");
            }
        }

        // The current plugin version is not yet installed.
        Logger.v(TAG, "Plugin not installed, load it from target path.");

        // 复制到内部临时路径 (文件操作会增加插件加载的失败率，这一步没有必要)

//        if (!apkPath.startsWith(mContext.getCacheDir().getAbsolutePath())) {
//            String tempFilePath = mPluginManager.getInstaller().getTempPluginPath();
//            PluginLogUtil.v(TAG, "[loadPlugin][plugin not installed]copy to internal cache dir = " + tempFilePath);
//            try {
//                PluginFileUtil.copyFile(apkPath, tempFilePath);
//                basePluginPackage.apkPath = tempFilePath;
//                apkPath = tempFilePath;
//            } catch (IOException e) {
//                e.printStackTrace();
//                new File(tempFilePath).delete();
//                PluginLogUtil.w(TAG, "[loadPlugin][plugin not installed]copy to internal cache dir fail, " + apkPath + " to " + tempFilePath);
//                throw new LoadPluginException("loader copy plugin to temp dir fail");
//            }
//        }

        PackageInfo packageInfo = Internals.ApkUtils.getPackageInfo(mContext, apkPath);
        if (packageInfo == null) {
            Logger.w(TAG, "Can not get target plugin's packageInfo.");
            FileUtils.deleteQuietly(new File(apkPath));
            throw new LoadPluginException("Can not get target plugin's packageInfo.");
        }

        plugin.setPackageInfo(packageInfo);

        // Check plugin's signatures.
        Logger.v(TAG, "Check plugin's signatures.");
        if (!mManager.getInstaller().checkPluginSafety(apkPath)) {
            Logger.w(TAG, "Check plugin's signatures fail.");
            FileUtils.deleteQuietly(new File(apkPath));
            throw new LoadPluginException("Check plugin's signatures fail.");
        }

        Plugin pluginPackage = getPluginPackage(packageInfo.packageName);
        if (pluginPackage != null) {
            Logger.v(TAG, "The current plugin has been loaded, get it from holder, id = "
                    + packageInfo.packageName);
            return pluginPackage;
        }

        // Load plugin from installed path.
        Logger.v(TAG, "Load plugin from dest path.");
        plugin = plugin.loadPlugin(mContext);
        putPluginPackage(packageInfo.packageName, plugin);

        // Delete temp file.
        FileUtils.deleteQuietly(new File(apkPath));
        return plugin;
    }

    @Override
    public synchronized Plugin getPluginPackage(String packageName) {
        Plugin plugin = mPackageHolder.get(packageName);
        if (plugin != null && !plugin.isLoaded()) {
            return null;
        }
        return plugin;
    }

    @Override
    public synchronized void putPluginPackage(String id, Plugin plugin) {
        if (plugin != null && plugin.isLoaded()) {
            mPackageHolder.put(id, plugin);
        }
    }

    @Override
    public Class loadClass(@NonNull Plugin plugin, String className) throws IllegalPluginException {
        return Internals.ApkUtils.loadClass(plugin.getClassLoader(), className);

    }

    @Override
    public BaseBehaviour getBehaviour(Plugin plugin) throws IllegalPluginException {
        return plugin.getPluginBehaviour();
    }

}
