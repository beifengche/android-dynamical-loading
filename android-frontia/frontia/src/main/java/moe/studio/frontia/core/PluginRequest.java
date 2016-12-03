/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package moe.studio.frontia.core;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import moe.studio.frontia.BuildConfig;
import moe.studio.frontia.Frontia;
import moe.studio.frontia.error.UpdatePluginException;
import moe.studio.frontia.update.LocalPluginInfo;
import moe.studio.frontia.update.PluginController;
import moe.studio.frontia.update.RemotePluginInfo;

/**
 * 插件任务请求实体类。
 */
@SuppressWarnings("WeakerAccess")
public abstract class PluginRequest {

    private static final String TAG = "plugin.request";
    private static final int NO_VALUE = -2233;

    protected String mId;
    protected int retry;
    protected int mState;
    protected StringBuffer mLog;
    protected String mRemotePluginPath;
    protected String mLocalPluginPath;
    protected boolean mIsClearLocalPlugins;

    protected Plugin mPlugin;
    protected Frontia mManager;
    protected PluginSetting mSetting;
    protected List<Exception> mExceptions;
    protected final PluginController mController;

    // FOR ASSETS PLUGIN.
    protected int mAssetsVersion;
    protected String mAssetsPath;
    protected boolean mIsFromAssets;

    protected long mFileSize;
    // FOR ONLINE PLUGIN.
    protected String mDownloadUrl;
    protected boolean mIsForceUpdate;

    // PLUGIN INFO LIST.
    protected List<LocalPluginInfo> mLocalPlugins;
    protected List<? extends RemotePluginInfo> mRemotePlugins;

    public PluginRequest() {
        retry = NO_VALUE;
        mState = States.REQUEST_WTF;
        mLog = new StringBuffer(String.valueOf(mState));
        mController = new PluginController();
        mSetting = new PluginSetting.Builder()
                .setDebugMode(BuildConfig.DEBUG)
                .ignoreInstalledPlugin(BuildConfig.DEBUG)
                .build();
    }

    public PluginRequest attach(Frontia manager) {
        mManager = manager;
        mSetting = manager.getSetting();
        return this;
    }

    /**
     * 获取当前状态, 参考{@link States}
     */
    public int getState() {
        return mState;
    }

    /**
     * 获取插件任务的状态转换记录, 参考{@link States}
     */
    public String getLog() {
        return mLog.toString();
    }

    /**
     * 切换状态, 参考{@link States}
     */
    public PluginRequest switchState(int state) {
        this.mState = state;
        return marker(String.valueOf(state));
    }

    /**
     * 记录Log
     */
    public PluginRequest marker(String log) {
        if (!TextUtils.isEmpty(log)) {
            mLog.append(" --> ").append(log);
        }
        return this;
    }

    /**
     * 获取异常列表
     */
    @Nullable
    public List<Exception> getExceptions() {
        return mExceptions;
    }

    /**
     * 记录异常
     */
    public PluginRequest markException(@NonNull Exception exception) {
        if (mExceptions == null) {
            mExceptions = new ArrayList<>();
        }
        mExceptions.add(exception);
        return marker(exception.getLocalizedMessage());
    }

    /**
     * 重试
     *
     * @throws PluginErrors.RetryError 重试超标
     */
    public void retry() throws PluginErrors.RetryError {
        if (retry == NO_VALUE) {
            retry = mSetting.getRetryCount();
        }

        if (--retry < 0) {
            throw new PluginErrors.RetryError();
        }
    }

    /**
     * 插件请求是否失败
     */
    public boolean isUpdateFail() {
        return States.isUpdateFail(mState);
    }

    /**
     * 获取插件ID
     */
    @Nullable
    public String getId() {
        return mId;
    }

    /**
     * 设置插件ID
     */
    public void setId(String id) {
        mId = id;
    }

    /**
     * 是否清除本地已安装插件
     */
    public boolean isClearLocalPlugins() {
        return mIsClearLocalPlugins;
    }

    /**
     * 设置是否清除本地已安装插件
     */
    public void setClearLocalPlugins(boolean isClear) {
        mIsClearLocalPlugins = isClear;
    }

    /**
     * 获取插件路径
     */
    @Nullable
    public String getPluginPath() {
        if (!TextUtils.isEmpty(mRemotePluginPath)) {
            return mRemotePluginPath;
        }

        return mLocalPluginPath;
    }

    /**
     * 设置插件路径
     */
    public void setPluginPath(String path) {
        mRemotePluginPath = path;
    }

    /**
     * 获取插件本地路径(安装路径)
     */
    @Nullable
    public String getLocalPluginPath() {
        return mLocalPluginPath;
    }

    /**
     * 设置插件本地路径(安装路径)
     */
    public void setLocalPluginPath(String path) {
        mLocalPluginPath = path;
    }

    /**
     * 获取插件
     */
    @Nullable
    public Plugin getPlugin() {
        return mPlugin;
    }

    /**
     * 设置插件
     */
    public void setPlugin(Plugin plugin) {
        mPlugin = plugin;
    }

    /**
     * 获取插件请求控制器
     */
    public PluginController getController() {
        return mController;
    }

    /**
     * 设置插件请求任务监听器
     */
    public void setListener(PluginController.UpdateListener listener) {
        mController.setListener(listener);
    }

    /**
     * 是否是内置插件(存放在Assets)
     */
    public boolean isFromAssets() {
        return mIsFromAssets;
    }

    /**
     * 获取内置路径
     */
    @Nullable
    public String getAssetsPath() {
        return mAssetsPath;
    }

    /**
     * 获取内置插件版本
     */
    public int getAssetsVersion() {
        return mAssetsVersion;
    }

    /**
     * 设置内置插件信息
     *
     * @param path    Assets路径
     * @param version 版本
     */
    public void fromAssets(String path, int version) {
        mIsFromAssets = true;
        mAssetsPath = path;
        mAssetsVersion = version;
    }

    /**
     * 获取插件下载URL
     */
    @Nullable
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    /**
     * 设置插件下载URL
     */
    public void setDownloadUrl(String downloadUrl) {
        this.mDownloadUrl = downloadUrl;
    }

    /**
     * 获取插件大小(弃用, 插件下载器能自动获取在线插件文件大小)
     */
    @Deprecated
    public long getFileSize() {
        return mFileSize;
    }

    /**
     * 设置插件大小(弃用, 插件下载器能自动获取在线插件文件大小)
     */
    @Deprecated
    public void setFileSize(long fileSize) {
        mFileSize = fileSize;
    }

    /**
     * 是否强制使用最新插件
     */
    public boolean forceUpdate() {
        return mIsForceUpdate;
    }

    /**
     * 设置是否强制使用最新插件
     * <p>
     * 在更新插件失败的情况下, 如果该项为true, 则不会使用本地以安装(如果有)的旧版本插件,
     * 结果就是导致插件加载失败。
     */
    public void setForUpdate(boolean force) {
        mIsForceUpdate = force;
    }

    /**
     * 当前插件本地已安装的所有版本信息
     */
    @Nullable
    public List<LocalPluginInfo> getLocalPlugins() {
        return mLocalPlugins;
    }

    /**
     * 设置当前插件本地已安装的所有版本信息
     */
    public void setLocalPlugins(List<LocalPluginInfo> infos) {
        mLocalPlugins = infos;
    }

    /**
     * 当前插件在线的所有版本信息
     */
    @Nullable
    public List<? extends RemotePluginInfo> getRemotePlugins() {
        return mRemotePlugins;
    }

    /**
     * 设置当前插件在线的所有版本信息
     */
    public void setRemotePlugins(List<? extends RemotePluginInfo> infos) {
        this.mRemotePlugins = infos;
    }

    /**
     * 获取当前插件在线的所有版本信息
     * (必须由用户继承并实现该接口, 因为我压根不知道你的在线插件长什么样。)
     *
     * @param context       Context
     * @param pluginRequest 插件任务请求实体类(this)
     * @throws UpdatePluginException 用户取消插件任务
     */
    public abstract void getRemotePluginInfo(Context context, @NonNull PluginRequest pluginRequest)
            throws UpdatePluginException;

    /**
     * 获取当前插件本地已安装的所有版本信息
     */
    public void getLocalPluginInfo(Context context, @NonNull PluginRequest pluginRequest) {
        String pluginId = getId();
        if (!TextUtils.isEmpty(pluginId)) {
            pluginRequest.setLocalPlugins(getLocalPluginInfoById(context, pluginId));
        }
    }

    /**
     * 获取当前插件本地已安装的所有版本信息
     *
     * @param context  Context
     * @param pluginId 插件ID
     * @return 已安装的所有版本列表
     */
    protected List<LocalPluginInfo> getLocalPluginInfoById(@NonNull Context context, @NonNull String pluginId) {
        List<LocalPluginInfo> localPluginInfoList = new ArrayList<>();
        String pluginDir = mManager.getInstaller().getPluginPath(pluginId);

        File file = new File(pluginDir);
        if (!file.exists()) {
            Log.d(TAG, "No local plugin, path = " + file.getAbsolutePath());
            return localPluginInfoList;
        }

        String[] versions = file.list();
        for (String version : versions) {
            if (TextUtils.isDigitsOnly(version)) {
                // 版本文件夹只能是数字
                if (mManager.getInstaller().isPluginInstalled(pluginId, version)) {
                    // 插件版本已经安装,且合法
                    LocalPluginInfo item = new LocalPluginInfo();
                    item.pluginId = pluginId;
                    item.version = Integer.valueOf(version);
                    item.isValid = true;
                    localPluginInfoList.add(item);
                }
            } else {
                // 删除版本外文件
                new File(pluginDir + File.separator + version).delete();
            }
        }

        Collections.sort(localPluginInfoList);

        // Dump existing plugin versions.
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "-");
            Log.v(TAG, "Found local plugin \"" + pluginId + "\" :");
            for (LocalPluginInfo item : localPluginInfoList) {
                Log.v(TAG, "Version =  " + item.version + ", path = "
                        + mManager.getInstaller().getPluginInstallPath(pluginId,
                        String.valueOf(item.version)));
            }
            Log.v(TAG, "-");
        }
        return localPluginInfoList;
    }

    /**
     * 当无法获取远程插件信息时候执行的回调, 默认使用本地最优插件(如果有)。
     */
    public void doIllegalRemotePluginPolicy(@NonNull PluginRequest pluginRequest,
                                            UpdatePluginException exception) {
        pluginRequest.setId(getId());
        useLocalAvailablePlugin(pluginRequest);
    }

    /**
     * 当插件更新失败时候执行的回调, 默认使用本地最优插件(如果有)。
     * 如果远程插件信息里配置了强制升级, 则丢弃本地插件, 插件加载失败。
     */
    public void doUpdateFailPolicy(@NonNull PluginRequest pluginRequest, UpdatePluginException exception) {
        if (!pluginRequest.forceUpdate()) {
            // 非强制升级，使用本地可用插件
            useLocalAvailablePlugin(pluginRequest);
        } else {
            // 强制升级，无插件可用
            pluginRequest.switchState(States.REQUEST_NO_AVAILABLE_PLUGIN);
        }
    }

    /**
     * 使用版本可用的插件
     */
    protected void useLocalAvailablePlugin(@NonNull PluginRequest pluginRequest) {
        // 使用本地最优插件
        String localPlugin = pluginRequest.getLocalPluginPath();
        if (!TextUtils.isEmpty(localPlugin)) {
            pluginRequest.setPluginPath(localPlugin);
            pluginRequest.switchState(States.REQUEST_ALREADY_TO_LOAD_PLUGIN);
        }
    }

    /**
     * 加载插件时, 使用的插件实体类型。
     * (必须由用户继承并实现该接口, 只有你才知道你需要什么用的插件不是吗。)
     */
    public abstract Plugin createPlugin(String pluginPath);

    /**
     * 插件请求认为被取消
     */
    public void onCancelRequest(Context context, PluginRequest pluginRequest) {
        pluginRequest.switchState(States.REQUEST_CANCEL);
        PluginController.UpdateListener listener = pluginRequest.getController().getListener();
        if (listener != null) {
            listener.onCanceled(pluginRequest.getState(), pluginRequest);
        }
    }

    /**
     * 插件请求状态类
     */
    public static class States {

        // Request state code.
        public static final int REQUEST_WTF = -1;                           // 诡异的错误
        public static final int REQUEST_REQUEST_PLUGIN_INFO_FAIL = -2;      // 无法获取远程插件信息
        public static final int REQUEST_NO_AVAILABLE_PLUGIN = -3;           // 没有插件可用(远程和本地均没有)
        public static final int REQUEST_UPDATE_PLUGIN_FAIL = -4;            // 插件更新失败
        public static final int REQUEST_LOAD_PLUGIN_FAIL = -5;              // 插件加载失败
        public static final int REQUEST_GET_BEHAVIOUR_FAIL = -6;            // 插件加载完成, 但是无法获取插件行为接口(无法控制插件)
        public static final int REQUEST_CANCEL = -7;                        // 任务被取消

        public static final int REQUEST_LOAD_PLUGIN_SUCCESS = 0;            // 插件已经加载成功
        public static final int REQUEST_ALREADY_TO_LOAD_PLUGIN = 1;         // 插件更新完成、准备加载
        public static final int REQUEST_NEED_EXTRACT_ASSETS_PLUGIN = 2;     // 准备从ASSETS释放插件
        public static final int REQUEST_NEED_DOWNLOAD_ONLINE_PLUGIN = 3;    // 准备下载插件

        /**
         * 当前请求任务是否失败
         */
        public static boolean isUpdateFail(int state) {
            return state == States.REQUEST_CANCEL
                    || state == States.REQUEST_REQUEST_PLUGIN_INFO_FAIL
                    || state == States.REQUEST_UPDATE_PLUGIN_FAIL
                    || state == States.REQUEST_LOAD_PLUGIN_FAIL
                    || state == States.REQUEST_GET_BEHAVIOUR_FAIL
                    || state == States.REQUEST_WTF;
        }
    }

}
