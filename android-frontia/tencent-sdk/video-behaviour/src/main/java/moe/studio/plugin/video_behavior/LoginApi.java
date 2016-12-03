/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.plugin.video_behavior;

import android.content.Context;

import moe.studio.frontia.bridge.HostApi;

/**
 * Created by kaede on 2015/12/7.
 */
public abstract class LoginApi extends HostApi {

	abstract public boolean isLogined();

	abstract public void goToLogin(Context context);

}
