/*
 * Copyright (c) 2016. Kaede
 */

package com.kaedea.frontia.demo;


import android.content.Context;

import com.kaedea.frontia.demo.bridge.LoginApiImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.gemini.tinyplayer.BuildConfig;
import moe.studio.plugin.video_behavior.LoginApi;
import moe.studio.frontia.Frontia;
import moe.studio.frontia.bridge.HostApi;
import moe.studio.frontia.bridge.HostApiManager;
import moe.studio.frontia.ext.PluginSetting;

/**
 * Created by kaede on 2015/12/7.
 */
public class PluginHelper {

	static Map<Class<? extends HostApi>, Class<? extends HostApi>> apisMap;

	static {
		apisMap = new HashMap<>();
		apisMap.put(LoginApi.class, LoginApiImpl.class);
		// apisMap.put(UserInfoApi.class, UserInfoApiImpl.class);
	}

	/**
	 * 初始化，启动插件前，要确保已经执行该初始化方法
	 */
	public static void init(Context context) {
		registerApis();
		PluginSetting setting = new PluginSetting.Builder()
				.setDebugMode(BuildConfig.DEBUG)
				.ignoreInstalledPlugin(BuildConfig.DEBUG)
				.build();
		Frontia.instance().init(context, setting);
	}

	private static void registerApis() {
		Set<Class<? extends HostApi>> keySet = apisMap.keySet();
		for (Class<? extends HostApi> key :
				keySet) {
			Class<? extends HostApi> value = apisMap.get(key);
			try {
				HostApiManager.getInstance().putApi(key, value.newInstance());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

}
