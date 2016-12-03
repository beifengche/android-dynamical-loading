/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package moe.studio.frontia.update;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import moe.studio.frontia.core.PluginRequest;

/**
 * 插件更新控制器：
 * 1. 控制更新过程，如取消更新；
 * 2. 更新过程回调，如通知更新进度；
 */
public class PluginController {
    public static final String TAG = "plugin.controller";

    private boolean mIsCanceled;
    private UpdateListener mListener;

    public void cancel() {
        mIsCanceled = true;
    }

    public boolean isCanceled() {
        return mIsCanceled || Thread.currentThread().isInterrupted();
    }

    public void notifyProgress(PluginRequest pluginRequest, float progress) {
        if (mListener != null) {
            mListener.onProgress(pluginRequest.getState(), pluginRequest, progress);
        }
    }

    public void setListener(@NonNull UpdateListener updateListener) {
        mListener = updateListener;
    }

    @Nullable
    public UpdateListener getListener() {
        return mListener;
    }

    public interface UpdateListener {
        void onCanceled(int sate, PluginRequest pluginRequest);

        void onPreUpdate(int sate, PluginRequest pluginRequest);

        void onProgress(int sate, PluginRequest pluginRequest, float progress);

        void onPostUpdate(int sate, PluginRequest pluginRequest);

        void onPreLoad(int sate, PluginRequest pluginRequest);

        void onPostLoad(int sate, PluginRequest pluginRequest);
    }

    public static class UpdateListenerImpl implements UpdateListener {

        @Override
        public void onCanceled(int sate, PluginRequest pluginRequest) {

        }

        @Override
        public void onPreUpdate(int sate, PluginRequest pluginRequest) {

        }

        @Override
        public void onProgress(int sate, PluginRequest pluginRequest, float progress) {

        }

        @Override
        public void onPostUpdate(int sate, PluginRequest pluginRequest) {

        }

        @Override
        public void onPreLoad(int sate, PluginRequest pluginRequest) {

        }

        @Override
        public void onPostLoad(int sate, PluginRequest pluginRequest) {

        }
    }
}
