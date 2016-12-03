/*
 * Copyright (c) 2016. Kaede
 */

package me.kaede.pluginpackage;

import moe.studio.plugin.video_behavior.ITencentVideo;
import moe.studio.frontia.core.PluginApp;

/**
 * @author kaede
 * @version date 2016/12/1
 */

public class PluginApplication extends PluginApp {

    @Override
    public ITencentVideo getBehavior() {
        ITencentVideo iTencentVideo = TencentVideoImpl.getInstance(mContext);
        iTencentVideo.onCreate();
        return iTencentVideo;
    }
}
