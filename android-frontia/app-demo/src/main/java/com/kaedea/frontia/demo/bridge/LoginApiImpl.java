/*
 * Copyright (c) 2016. Kaede
 */

package com.kaedea.frontia.demo.bridge;

import android.content.Context;

import moe.studio.plugin.video_behavior.LoginApi;


/**
 * Created by kaede on 2015/12/7.
 */
public class LoginApiImpl extends LoginApi {
	@Override
	public boolean isLogined() {
		return false;
	}

	@Override
	public void goToLogin(Context context) {

	}
}
