package moe.studio.plugin.video_behavior;

import android.app.Activity;

import java.lang.reflect.Method;


import moe.studio.frontia.Frontia;
import moe.studio.frontia.SoLibPlugin;
import moe.studio.frontia.bridge.plugin.BaseBehaviour;
import moe.studio.frontia.error.IllegalPluginException;

/**
 * Created by kaede on 2016/4/14.
 */
public class TencentVideoPackage extends SoLibPlugin {

	public TencentVideoPackage(String pluginPath) {
		super(pluginPath);
	}

	@Override
	public BaseBehaviour getPluginBehaviour(Object... args) throws IllegalPluginException {
		try {
			Class clazz = Frontia.instance().loadClass(this, "me.kaede.pluginpackage.Entry");
			Method method = clazz.getMethod("getTencentVideo", Activity.class);
			return (ITencentVideo) method.invoke(null, args[0]);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}