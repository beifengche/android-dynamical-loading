package com.kaedea.frontia.demo;

import android.content.Context;
import android.support.annotation.NonNull;

import moe.studio.frontia.core.Plugin;
import moe.studio.frontia.core.PluginRequest;
import moe.studio.frontia.error.UpdatePluginException;
import moe.studio.plugin.video_behavior.TencentVideoPackage;

/**
 * Created by Kaede on 16/6/28.
 */
public class AssetsVideoRequest extends PluginRequest {
    @Override
    public void getRemotePluginInfo(Context context, @NonNull PluginRequest pluginRequest) throws UpdatePluginException {
        pluginRequest.setId("me.kaede.videoplugin");
        pluginRequest.fromAssets("videoplugin.apk", 1);
    }

    public Plugin createPlugin(String pluginPath) {
        return new TencentVideoPackage(pluginPath);
    }
}
