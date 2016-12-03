/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.plugin.video_behavior;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import moe.studio.frontia.core.PluginBehavior;


/**
 * Created by kaede on 2016/4/8.
 */
public interface ITencentVideo extends PluginBehavior {
	void attach(Activity activity);
	public void toast(Context context, String msg);
	public void onCreate();
	public View getVideoView();
	public void play(String mVid, int mPlayType);
	public void toast2(Context context, String msg);
 }
