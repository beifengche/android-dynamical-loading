/*
 * Copyright (c) 2016. Kaede
 */

package com.kaedea.frontia.demo.tencent;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import moe.studio.plugin.video_behavior.TencentVideoPlugin;
import moe.studio.frontia.core.Plugin;
import moe.studio.frontia.core.PluginRequest;
import moe.studio.frontia.ext.PluginError.UpdateError;

/**
 * Created by Kaede on 16/6/28.
 */
public class OnlineVideoRequest extends PluginRequest {

    public static final String PLUGIN_ID = "me.kaede.videoplugin";

    @Override
    public void getRemotePluginInfo(Context context, @NonNull PluginRequest request) throws UpdateError {
        try {
            JSONObject jsonObject = loadJSONFromAsset(context);
            if (jsonObject.optInt("code") == 0){
                JSONObject data = jsonObject.optJSONObject("data");
                if (data != null) {
                    String id = data.optString("id");
                    request.setId(id);
                    request.setClearLocalPlugins(data.optInt("clear") == 1);
                    JSONArray versions = data.optJSONArray("versions");

                    if (versions != null && versions.length() > 0) {
                        List<VideoPluginInfo> videoPluginInfos = new ArrayList<>();
                        for (int i = 0; i < versions.length(); i++) {
                            JSONObject item = versions.optJSONObject(i);
                            VideoPluginInfo remotePluginInfo = new VideoPluginInfo();
                            remotePluginInfo.fileSize = item.optLong("size");
                            remotePluginInfo.minAppBuild = item.optInt("nub_build");
                            remotePluginInfo.enable = item.optInt("enable") == 1;
                            remotePluginInfo.isForceUpdate = item.optInt("force") == 1;
                            remotePluginInfo.downloadUrl = item.optString("url");
                            remotePluginInfo.pluginId = id;
                            remotePluginInfo.version = item.optInt("ver_code");
                            videoPluginInfos.add(remotePluginInfo);
                        }
                        request.setRemotePlugins(videoPluginInfos);
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            throw new UpdateError(e, 2233);
        }
    }

    // 模拟获取服务器插件信息
    private JSONObject loadJSONFromAsset(Context context) throws IOException, JSONException {
        JSONObject json = null;
        InputStream is = context.getAssets().open("onlineplugininfo.json");
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        String string = new String(buffer, "UTF-8");
        json = new JSONObject(string);
        return json;
    }

    @NonNull
    public String getId() {
        String id = getId();
        return TextUtils.isEmpty(id) ? PLUGIN_ID : id;
    }

    public Plugin createPlugin(String path) {
        return new TencentVideoPlugin(path);
    }
}
