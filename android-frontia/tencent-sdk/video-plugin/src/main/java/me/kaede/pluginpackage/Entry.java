/*
 * Copyright (c) 2016. Kaede
 */

package me.kaede.pluginpackage;

import android.content.Context;

import moe.studio.plugin.video_behavior.ITencentVideo;


public class Entry {

	public static ITencentVideo getTencentVideo(Context context){
		ITencentVideo iTencentVideo = TencentVideoImpl.getInstance(context);
		iTencentVideo.onCreate();
		return iTencentVideo;
	}

}
