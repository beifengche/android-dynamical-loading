package moe.studio.plugin.video_behavior;

import android.content.Context;
import android.view.View;

import moe.studio.frontia.bridge.plugin.BaseBehaviour;

/**
 * Created by kaede on 2016/4/8.
 */
public interface ITencentVideo extends BaseBehaviour {
	public void toast(Context context,String msg);
	public void onCreate();
	public View getVideoView();
	public void play(String mVid, int mPlayType);
 }
