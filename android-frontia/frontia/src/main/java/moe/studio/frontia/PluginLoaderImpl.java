/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import moe.studio.frontia.Internals.ApkUtils;
import moe.studio.frontia.core.Plugin;
import moe.studio.frontia.core.PluginApp;
import moe.studio.frontia.core.PluginBehavior;
import moe.studio.frontia.core.PluginLoader;
import moe.studio.frontia.core.PluginManager;
import moe.studio.frontia.core.PluginRequest;
import moe.studio.frontia.ext.PluginApk;
import moe.studio.frontia.ext.PluginError;
import moe.studio.frontia.ext.PluginError.InstallError;
import moe.studio.frontia.ext.PluginError.LoadError;

import static moe.studio.frontia.Internals.FileUtils;
import static moe.studio.frontia.core.PluginRequest.State.CANCELED;
import static moe.studio.frontia.core.PluginRequest.State.LOA_PLUGIN_FAIL;
import static moe.studio.frontia.core.PluginRequest.State.LOA_SUCCESS;
import static moe.studio.frontia.core.PluginRequest.State.UPD_SUCCESS;
import static moe.studio.frontia.core.PluginRequest.State.WTF;
import static moe.studio.frontia.ext.PluginError.ERROR_INS_PACKAGE_INFO;
import static moe.studio.frontia.ext.PluginError.ERROR_LOA_CLASS;
import static moe.studio.frontia.ext.PluginError.ERROR_LOA_NOT_LOADED;
import static moe.studio.frontia.ext.PluginError.RetryError;

/**
 * 插件加载器。
 * <p>
 * 用于加载指定路径上的插件, 同时保存已经加载过的插件。
 */
@SuppressWarnings("unchecked")
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
    public PluginRequest load(@NonNull final PluginRequest request) {
        Logger.i(TAG, "Loading plugin, id = " + request.getId());
        request.marker("Load");

        onPreLoad(request);

        if (request.isCanceled()) {
            onCanceled(request);
            return request;
        }

        if (request.getState() == UPD_SUCCESS) {
            String path = request.getPluginPath();
            if (!TextUtils.isEmpty(path)) {
                // Plugin was updated, start to load plugin.
                Plugin plugin = request.createPlugin(path);
                plugin.attach(mManager);

                int retry = 0;
                request.setRetry(mManager.getSetting().getRetryCount());

                while (true) {
                    if (request.isCanceled()) {
                        onCanceled(request);
                        return request;
                    }

                    try {
                        request.setPlugin(load(plugin));
                        Logger.v(TAG, "Load plugin success, path = " + path);
                        request.switchState(LOA_SUCCESS);
                        onPostLoad(request);
                        return request;

                    } catch (LoadError | InstallError error) {
                        Logger.w(TAG, error);
                        try {
                            request.retry();
                            Logger.v(TAG, "Load fail, retry " + (retry++));
                            request.marker("Retry load " + retry);
                        } catch (RetryError retryError) {
                            Logger.v(TAG, "Load plugin fail, error = "
                                    + error.toString());
                            onError(request, error);
                            return request;
                        }
                    }
                }

            } else {
                // Should not have this state.
                request.switchState(WTF);
                onPostLoad(request);
                return request;
            }

        } else {
            onPostLoad(request);
            return request;
        }
    }

    @Override
    public Plugin load(Plugin plugin) throws LoadError, InstallError {
        String apkPath = plugin.getApkPath();
        Logger.d(TAG, "Loading plugin, path = " + apkPath);

        // Check if the current version has been installed before.
        if (mManager.getInstaller().isInstalled(apkPath)) {
            String installPath = mManager.getInstaller()
                    .getInstallPath(apkPath);

            if (FileUtils.exist(installPath)) {
                Logger.v(TAG, "The current version has been installed before.");
                PackageInfo packageInfo = ApkUtils
                        .getPackageInfo(mContext, installPath);

                if (packageInfo != null) {
                    plugin.setPackageInfo(packageInfo);
                    plugin.setInstallPath(installPath);
                    Plugin loaded = getPlugin(packageInfo.packageName);

                    if (loaded != null) {
                        // The current plugin has been loaded.
                        Logger.v(TAG, "The current plugin has been loaded, id = "
                                + packageInfo.packageName);
                        return loaded;
                    }

                    // Load plugin from installed path.
                    Logger.v(TAG, "Load plugin from installed path.");
                    plugin = plugin.loadPlugin(mContext, installPath);
                    putPlugin(packageInfo.packageName, plugin);
                    return plugin;

                } else {
                    Logger.w(TAG, "Can not get installed plugin's packageInfo, " +
                            "try target plugin.");
                }
            }
        }

        // The current plugin version is not yet installed.
        Logger.v(TAG, "Plugin not installed, load it from target path.");
        PackageInfo packageInfo = ApkUtils.getPackageInfo(mContext, apkPath);

        if (packageInfo == null) {
            Logger.w(TAG, "Can not get target plugin's packageInfo.");
            FileUtils.delete(apkPath);
            throw new InstallError("Can not get target plugin's packageInfo.",
                    ERROR_INS_PACKAGE_INFO);
        }

        plugin.setPackageInfo(packageInfo);
        Plugin loaded = getPlugin(packageInfo.packageName);

        if (loaded != null) {
            Logger.v(TAG, "The current plugin has been loaded, id = "
                    + packageInfo.packageName);
            return loaded;
        }

        Logger.v(TAG, "Load plugin from dest path.");

        // Install the dest file into inner install dir.
        String install = mManager.getInstaller().install(apkPath);
        plugin.setInstallPath(install);

        plugin = plugin.loadPlugin(mContext, install);
        putPlugin(packageInfo.packageName, plugin);

        // Delete temp file.
        if (apkPath.endsWith(mManager.getSetting().getTempFileSuffix())) {
            FileUtils.delete(apkPath);
        }

        return plugin;
    }

    @Override
    public synchronized Plugin getPlugin(String packageName) {
        Plugin plugin = mPackageHolder.get(packageName);
        if (plugin != null && !plugin.isLoaded()) {
            return null;
        }
        return plugin;
    }

    @Override
    public synchronized void putPlugin(String id, Plugin plugin) {
        if (plugin != null && plugin.isLoaded()) {
            mPackageHolder.put(id, plugin);
        }
    }

    @Override
    public Class loadClass(@NonNull Plugin plugin, String className) throws LoadError {
        if (!plugin.isLoaded()) {
            throw new LoadError("Plug is not yet loaded.", ERROR_LOA_NOT_LOADED);
        }

        try {
            return ApkUtils.loadClass(plugin.getPackage().classLoader, className);
        } catch (Exception e) {
            throw new LoadError(e, ERROR_LOA_CLASS);
        }

    }

    private void onPreLoad(PluginRequest request) {
        Logger.i(TAG, "onPreLoad state = " + request.getState());
        mManager.getCallback().preLoad(request);
    }

    private void onError(PluginRequest request, PluginError error) {
        Logger.i(TAG, "onError state = " + request.getState());
        request.switchState(LOA_PLUGIN_FAIL);
        request.markException(error);
        onPostLoad(request);
    }

    private void onCanceled(PluginRequest request) {
        Logger.i(TAG, "onCanceled state = " + request.getState());
        request.switchState(CANCELED);
        mManager.getCallback().onCancel(request);
    }

    private void onPostLoad(PluginRequest request) {
        Logger.i(TAG, "onPostLoad state = " + request.getState());
        mManager.getCallback().postLoad(request);

        if (request.getState() == LOA_SUCCESS) {
            onLoadSuccess(request);
        }
    }

    private void onLoadSuccess(PluginRequest request) {
        Logger.i(TAG, "onLoadSuccess state = " + request.getState());
        mManager.getCallback().loadSuccess(mContext, request);

    }

    @Override
    public PluginBehavior createPluginBehavior(Plugin plugin) {
        String apkPath = plugin.getInstallPath();

        try {
            PluginApk apk = ManifestUtils.parse(new File(apkPath),
                    plugin.getPackage());

            if (!TextUtils.isEmpty(apk.application)) {
                // Create plugin's behavior via Manifest entry (PluginApplication).
                Class entry = loadClass(plugin, apk.application);
                PluginApp app = (PluginApp) entry.newInstance();
                app.setAppContext(mContext);
                return app.getBehavior();

            } else {
                Logger.w(TAG, "Cat not find plugin's app.");
                return null;
            }
        } catch (Throwable e) {
            Logger.w(TAG, e);
            return null;
        }
    }


}
