package me.kaede.pluginpackage;

import android.app.Activity;
import moe.studio.plugin.video_behavior.ITencentVideo;

/**
 * Created by kaede on 2016/4/11.
 */
public class Entry {

	public static ITencentVideo getTencentVideo(Activity activity){
		ITencentVideo iTencentVideo = TencentVideoImpl.getInstance(activity);
		iTencentVideo.onCreate();
		return iTencentVideo;
	}

}
