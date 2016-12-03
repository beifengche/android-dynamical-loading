/*
 * Copyright (c) 2016. Kaede
 */

package com.kaedea.frontia.demo.tencent;

import android.content.Context;
import android.support.annotation.NonNull;

import moe.studio.plugin.video_behavior.TencentVideoPlugin;
import moe.studio.frontia.core.PluginRequest;

/**
 * Created by Kaede on 16/6/28.
 */
public class AssetsVideoRequest extends PluginRequest<TencentVideoPlugin> {
    @Override
    public void getRemotePluginInfo(Context context, @NonNull PluginRequest request) {
        request.setId("me.kaede.videoplugin");
        request.fromAssets("videoplugin.apk", 1);
    }

    public TencentVideoPlugin createPlugin(String path) {
        return new TencentVideoPlugin(path);
    }
}
