/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.plugin.video;

import android.content.Context;

import moe.studio.plugin.video_behavior.ITencentVideo;


public class Entry {

	/**
	 * 旧插件入口类, 请用{@link PluginApplication#getBehavior()}代替。
     */
	public static ITencentVideo getTencentVideo(Context context){
		ITencentVideo iTencentVideo = TencentVideoImpl.getInstance(context);
		iTencentVideo.onCreate();
		return iTencentVideo;
	}

}
