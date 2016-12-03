package com.kaedea.frontia.demo;

import android.app.Application;

import me.kaede.mainapp.Plugin;
import moe.studio.frontia.Frontia;

/**
 * Created by Kaede on 16/6/28.
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Plugin.init(this);
        Frontia.instance().init(this);
    }
}
