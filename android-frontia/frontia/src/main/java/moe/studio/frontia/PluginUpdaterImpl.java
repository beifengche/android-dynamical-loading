/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package moe.studio.frontia;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import moe.studio.downloader.DownloadRequest;
import moe.studio.downloader.SyncDownloadProcessorImpl;
import moe.studio.downloader.core.DownloadListener;
import moe.studio.downloader.core.DownloadProcessor;
import moe.studio.frontia.core.PluginErrors;
import moe.studio.frontia.core.PluginManager;
import moe.studio.frontia.core.PluginRequest;
import moe.studio.frontia.core.PluginUpdater;
import moe.studio.frontia.error.CancelPluginException;
import moe.studio.frontia.error.UpdatePluginException;
import moe.studio.frontia.update.LocalPluginInfo;
import moe.studio.frontia.update.PluginController;
import moe.studio.frontia.update.RemotePluginInfo;

/**
 * 插件更新器
 * <p>
 * 之所以不叫“插件下载器”，是因为插件可以有许多获取的路径，不一定从服务器下载
 * 所以把获取目标插件的行为抽象成“更新插件”
 */
class PluginUpdaterImpl implements PluginUpdater {
    private static final String TAG = "plugin.update";

    private static final int RESPONSE_SUCCESS = 0;
    private static final int RESPONSE_ILLEGAL_ONLINE_PLUGIN = -1;

    private final Context mContext;
    private final PluginManager mManager;

    public PluginUpdaterImpl(Context context, PluginManager manager) {
        mContext = context.getApplicationContext();
        mManager = manager;
    }

    /**
     * ”更新插件“
     * 1. 根据更新状态执行插件的更新逻辑；
     * 2. 这里只更新从ASSETS解压插件或者从网络下载插件的逻辑；
     *
     * @param request 更新状态
     * @return 更新状态
     */
    @Override
    public PluginRequest updatePlugin(@NonNull PluginRequest request) {
        Logger.d(TAG, "[updatePlugin]");
        request.marker("Update");

        // Request remote plugin.
        requestPlugin(request);

        PluginController controller = request.getController();
        PluginController.UpdateListener listener = controller.getListener();
        if (listener != null) {
            listener.onPreUpdate(request.getState(), request);
        }

        if (request.getState() == PluginRequest.States.REQUEST_NEED_EXTRACT_ASSETS_PLUGIN) {
            // Extract plugin from assets.
            File tempFile;
            try {
                tempFile = mManager.getInstaller().createTempFile(request.getId());
            } catch (IOException e) {
                Logger.v(TAG, "Can not get temp file, error = " + e.getLocalizedMessage());
                e.printStackTrace();
                request.switchState(PluginRequest.States.REQUEST_UPDATE_PLUGIN_FAIL);
                request.markException(e);
                request.doUpdateFailPolicy(request,
                        new UpdatePluginException("Can not get temp file.", e));
                return request;
            }

            int retry = 0;
            Exception exception = null;
            while (true) {
                if (controller.isCanceled()) {
                    request.onCancelRequest(mContext, request);
                    return request;
                }

                try {
                    Internals.FileUtils.copyFileFromAsset(mContext, request.getAssetsPath(), tempFile);
                    request.switchState(PluginRequest.States.REQUEST_ALREADY_TO_LOAD_PLUGIN);
                    request.setPluginPath(tempFile.getAbsolutePath());
                    Logger.v(TAG, "Extract plugin from assets success.");
                    break;

                } catch (IOException e) {
                    exception = e;
                    e.printStackTrace();
                    try {
                        request.retry();
                        Logger.v(TAG, "Extract fail, retry " + (retry++));
                        request.marker("Retry extract " + retry);
                    } catch (PluginErrors.RetryError retryError) {
                        break;
                    }
                }
            }

            if (exception != null) {
                // Extract fail.
                Logger.v(TAG, "Extract plugin from assets fail, error = " + exception.getLocalizedMessage());
                request.switchState(PluginRequest.States.REQUEST_UPDATE_PLUGIN_FAIL);
                request.markException(exception);
                request.doUpdateFailPolicy(request,
                        new UpdatePluginException("Extract plugin from assets fail", exception));
            }

        } else if (request.getState() == PluginRequest.States.REQUEST_NEED_DOWNLOAD_ONLINE_PLUGIN) {
            // Download plugin from online.
            File tempFile;
            try {
                tempFile = mManager.getInstaller().createTempFile(request.getId());
            } catch (IOException e) {
                Logger.v(TAG, "Can not get temp file, error = " + e.getLocalizedMessage());
                e.printStackTrace();
                request.switchState(PluginRequest.States.REQUEST_UPDATE_PLUGIN_FAIL);
                request.markException(e);
                request.doUpdateFailPolicy(request,
                        new UpdatePluginException("Can not get temp file.", e));
                return request;
            }

            if (controller.isCanceled()) {
                request.onCancelRequest(mContext, request);
                return request;
            }

            try {
                downloadPlugin(request, tempFile);
                request.switchState(PluginRequest.States.REQUEST_ALREADY_TO_LOAD_PLUGIN);
                request.setPluginPath(tempFile.getAbsolutePath());
                Logger.v(TAG, "[updatePlugin]download plugin online success");

            } catch (UpdatePluginException e) {
                // Download fail.
                Logger.v(TAG, "Download plugin fail, error = " + e.getLocalizedMessage());
                e.printStackTrace();
                request.marker("Download fail.");
                request.switchState(PluginRequest.States.REQUEST_UPDATE_PLUGIN_FAIL);
                request.markException(e);
                request.doUpdateFailPolicy(request,
                        new UpdatePluginException("download plugin online fail", e));
            } catch (CancelPluginException e) {
                request.onCancelRequest(mContext, request);
                return request;
            }
        }

        if (listener != null) {
            listener.onPostUpdate(request.getState(), request);
        }
        return request;
    }

    /**
     * "请求插件信息"
     * 1. 需要实现获取远程插件信息的逻辑getRemotePluginInfo；
     * 2. 需要实现获取本地插件信息的逻辑getLocalPluginInfo；
     * 3. doUpdatePolicy根据远程和本地插件的信息，计算出最优的更新策略；
     * 4. 需要实现获取不到远程插件信息时本地的更新策略doIllegalRemotePluginInfoPolicy；
     */
    private PluginRequest requestPlugin(PluginRequest request) {
        Logger.d(TAG, "Request remote plugin info.");

        // Check clear existing plugins.
        if (request.isClearLocalPlugins()) {
            mManager.getInstaller().deletePlugins(request.getId());
        }

        // Get local existing plugin info.
        request.getLocalPluginInfo(mContext, request);

        List<LocalPluginInfo> localPlugins = request.getLocalPlugins();
        if (localPlugins != null && localPlugins.size() > 0) {
            LocalPluginInfo localPluginInfo = localPlugins.get(0);
            // Getting plugin installed path.
            String installPath = mManager.getInstaller().getPluginInstallPath(localPluginInfo.pluginId,
                    String.valueOf(localPluginInfo.version));
            request.setPluginPath(installPath);
            request.setLocalPluginPath(installPath);
        }

        try {
            // Request remote plugin info.
            request.getRemotePluginInfo(mContext, request);

            if (TextUtils.isEmpty(request.getId())) {
                doUpdatePolicy(RESPONSE_ILLEGAL_ONLINE_PLUGIN, request);
                return request;
            }

            List<? extends RemotePluginInfo> remotePlugins = request.getRemotePlugins();
            if (!request.isFromAssets() && (remotePlugins == null
                    || remotePlugins.size() == 0)) {
                doUpdatePolicy(RESPONSE_ILLEGAL_ONLINE_PLUGIN, request);
                return request;
            }

            // Success.
            doUpdatePolicy(RESPONSE_SUCCESS, request);

        } catch (UpdatePluginException e) {
            e.printStackTrace();
            Logger.w(TAG, "Request remote plugin info fail, error = " + e.getLocalizedMessage());
            request.switchState(PluginRequest.States.REQUEST_REQUEST_PLUGIN_INFO_FAIL);
            request.markException(e);
            request.doIllegalRemotePluginPolicy(request, e);
        }

        return request;
    }

    private void doUpdatePolicy(int responseCode, @NonNull PluginRequest request) {
        if (responseCode == RESPONSE_SUCCESS) {
            if (request.isFromAssets()) {
                // Using plugin from assets.
                Logger.v(TAG, "Using plugin from assets");

                String apkPath = mManager.getInstaller()
                        .getPluginInstallPath(request.getId(),
                                String.valueOf(request.getAssetsVersion()));

                if (mManager.getInstaller().isPluginInstalled(apkPath)) {
                    // The current version of plugin has been installed before.
                    request.switchState(PluginRequest.States.REQUEST_ALREADY_TO_LOAD_PLUGIN);
                    request.setPluginPath(apkPath);

                } else {
                    // Should extract plugin form assets.
                    request.switchState(PluginRequest.States.REQUEST_NEED_EXTRACT_ASSETS_PLUGIN);
                    Logger.v(TAG, "Extract plugin from assets, path = " + request.getAssetsPath());
                }

            } else {
                // Using online plugin.
                Logger.v(TAG, "Using online plugin.");

                // 执行升级策略。
                // 获取最佳的在线插件信息（版本最新，且最低APP_BUILD要求小于本APP版本）。
                List<? extends RemotePluginInfo> remotePluginInfoList = request.getRemotePlugins();
                RemotePluginInfo bestPlugin = null;
                int appBuild = Integer.MAX_VALUE;
                PackageInfo localPackageInfo = Internals.ApkUtils.getLocalPackageInfo(mContext);

                if (mManager.getSetting().isDebugMode() && localPackageInfo != null) {
                    appBuild = localPackageInfo.versionCode;
                }
                Logger.v(TAG, "App build = " + appBuild);

                // Get the best plugin version.
                for (RemotePluginInfo pluginInfo : remotePluginInfoList) {
                    if (pluginInfo.enable && pluginInfo.minAppBuild <= appBuild) {
                        bestPlugin = pluginInfo;
                        break;
                    }
                }

                if (bestPlugin == null) {
                    Logger.v(TAG, "No available plugin, abort.");
                    request.switchState(PluginRequest.States.REQUEST_NO_AVAILABLE_PLUGIN);

                } else {
                    LocalPluginInfo bestLocalPlugin = chooseBestPluginFromLocal(
                            request.getLocalPlugins(), bestPlugin);
                    if (bestLocalPlugin == null) {
                        // No local best plugin, should download from remote.
                        Logger.v(TAG, "Download new plugin, version = "
                                + bestPlugin.version + ", url = "
                                + bestPlugin.downloadUrl);

                        request.switchState(PluginRequest.States.REQUEST_NEED_DOWNLOAD_ONLINE_PLUGIN);
                        request.setDownloadUrl(bestPlugin.downloadUrl);
                        request.setFileSize(bestPlugin.fileSize);
                        request.setForUpdate(bestPlugin.isForceUpdate);

                    } else {
                        // The best plugin version has been installed before.
                        Logger.v(TAG, "Use local plugin, version = " + bestLocalPlugin.version);
                        String apkPath = mManager.getInstaller().getPluginInstallPath(bestLocalPlugin.pluginId,
                                String.valueOf(bestLocalPlugin.version));

                        request.switchState(PluginRequest.States.REQUEST_ALREADY_TO_LOAD_PLUGIN);
                        request.setPluginPath(apkPath);
                    }
                }
            }

        } else if (responseCode == RESPONSE_ILLEGAL_ONLINE_PLUGIN) {
            Logger.v(TAG, "Request remote plugin info fail, illegal online plugin.");
            request.switchState(PluginRequest.States.REQUEST_NO_AVAILABLE_PLUGIN);
            request.doIllegalRemotePluginPolicy(request, null);
        }
    }

    @Nullable
    private LocalPluginInfo chooseBestPluginFromLocal(List<LocalPluginInfo> localPlugins,
                                                      @NonNull RemotePluginInfo bestPlugin) {
        for (int i = 0; i < localPlugins.size(); i++) {
            LocalPluginInfo item = localPlugins.get(i);
            // Getting the latest version of plugin, which is not disabled.
            if (item.version == bestPlugin.version) {
                return item;
            }
        }
        return null;
    }

    private void downloadPlugin(final PluginRequest request, File destFile)
            throws UpdatePluginException, CancelPluginException {
        // Using FileDownloader to complete the download task.
        final PluginController controller = request.getController();
        final long fileSize = request.getFileSize();

        final String[] errorMsg = {null};

        DownloadRequest downloadRequest = new DownloadRequest(request.getDownloadUrl())
                .setContentLength(fileSize)
                .setDestFile(destFile)
                .setDeleteDestFileOnFailure(true)
                .setListener(new DownloadListener() {
                    @Override
                    public void onComplete(DownloadRequest request) {
                        Logger.v(TAG, "Download complete, original fileSize = " + fileSize
                                + ", downloadedSize = " + request.getCurrentBytes());
                    }

                    @Override
                    public void onFailed(DownloadRequest request, int errorCode, String errorMessage) {
                        errorMsg[0] = errorMessage;
                    }

                    @Override
                    public void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes,
                                           int progress, long bytesPerSecond) {
                        // notify progress
                        if (fileSize > 0) {
                            Logger.v(TAG, "Notify progress  = " + progress);
                            controller.notifyProgress(request, (float) progress / 100F);
                        }
                    }

                    @Override
                    public boolean isCanceled() {
                        return controller.isCanceled();
                    }

                });

        // Downloading asynchronously.
        DownloadProcessor processor = new SyncDownloadProcessorImpl();
        processor.attach(mContext);
        processor.add(downloadRequest);

        if (controller.isCanceled()) {
            throw new CancelPluginException("Download is canceled");

        } else if (!TextUtils.isEmpty(errorMsg[0])) {
            throw new UpdatePluginException(errorMsg[0]);
        }
    }
}
