/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.plugin.video_behavior;

import android.content.Context;

import moe.studio.frontia.core.PluginBehavior;

/**
 * Created by kaede on 2016/4/8.
 */
public interface IToast extends PluginBehavior {
	public void toast(Context context, String msg);
}
