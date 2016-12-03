/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia.ext;

import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;

import dalvik.system.DexClassLoader;

/**
 * @author kaede
 * @version date 2016/12/2
 */

public class PluginApk {

    public String application;
    public String packageName;
    public String versionCode;
    public String versionName;

    public PackageInfo packageInfo;
    public Resources resources;
    public AssetManager assetManager;
    public DexClassLoader classLoader;

    @Override
    public String toString() {
        return "PluginApk{" +
                "application='" + application + '\'' +
                ", packageName='" + packageName + '\'' +
                ", versionCode='" + versionCode + '\'' +
                ", versionName='" + versionName + '\'' +
                ", packageInfo=" + packageInfo +
                ", resources=" + resources +
                ", assetManager=" + assetManager +
                ", classLoader=" + classLoader +
                '}';
    }
}
