/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.plugin.video_behavior;

import android.content.Context;

import java.lang.reflect.Method;

import moe.studio.frontia.Frontia;
import moe.studio.frontia.SoLibPlugin;
import moe.studio.frontia.core.PluginBehavior;
import moe.studio.frontia.ext.PluginError.LoadError;

/**
 * Created by kaede on 2016/4/14.
 */
public class TencentVideoPlugin extends SoLibPlugin<ITencentVideo> {

    public TencentVideoPlugin(String pluginPath) {
        super(pluginPath);
    }

    @Override
    @Deprecated
    public ITencentVideo createBehavior(Context context) throws LoadError {
        PluginBehavior behavior = super.createBehavior(context);
        if (behavior != null) {
            return (ITencentVideo) behavior;
        }

        try {
            Class clazz = Frontia.instance().getLoader().loadClass(this, "me.kaede.pluginpackage.Entry");
            Method method = clazz.getMethod("getTencentVideo", Context.class);
            return (ITencentVideo) method.invoke(null, context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}