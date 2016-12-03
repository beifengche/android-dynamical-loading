/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia.ext;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import moe.studio.frontia.BuildConfig;
import moe.studio.frontia.core.Plugin;
import moe.studio.frontia.core.PluginBehavior;
import moe.studio.frontia.core.PluginManager;
import moe.studio.frontia.core.PluginRequest;

import static moe.studio.frontia.ext.PluginError.LoadError;

/**
 * 回调监听器，参考 {@linkplain PluginListener};
 */
@SuppressWarnings("unchecked")
public class PluginCallback {

    protected static final String TAG = "plugin.callback";

    private final PluginManager mManager;

    @SuppressWarnings("WeakerAccess")
    public PluginCallback(PluginManager manager) {
        mManager = manager;
    }


    @Nullable
    protected PluginListener getListener(PluginRequest request) {
        return request.getListener();
    }

    public void onCancel(PluginRequest request) {
        PluginListener listener = getListener(request);
        if (listener != null) {
            listener.onCanceled(request);
        }
    }

    public void notifyProgress(PluginRequest request, float progress) {
        PluginListener listener = getListener(request);
        if (listener != null) {
            listener.onProgress(request, progress);
        }
    }

    public void preUpdate(PluginRequest request) {
        PluginListener listener = getListener(request);
        if (listener != null) {
            listener.onPreUpdate(request);
        }
    }

    public void postUpdate(PluginRequest request) {
        PluginListener listener = getListener(request);
        if (listener != null) {
            listener.onPostUpdate(request);
        }
    }

    public void preLoad(PluginRequest request) {
        PluginListener listener = getListener(request);

        if (listener != null) {
            listener.onPreLoad(request);
        }
    }

    public void postLoad(PluginRequest request) {
        PluginListener listener = getListener(request);
        Plugin plugin = request.getPlugin();

        if (listener != null && plugin != null) {
            listener.onPostLoad(request, plugin);
        }
    }

    public void loadSuccess(Context context, PluginRequest request) {
        PluginListener listener = getListener(request);
        Plugin plugin = request.getPlugin();

        if (listener != null && plugin != null) {
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "Create behavior.");
            }

            try {
                PluginBehavior behavior = plugin.createBehavior(context);

                if (behavior == null) {
                    behavior = mManager.getLoader().createPluginBehavior(plugin);
                }

                if (behavior != null) {
                    // Create invocation proxy for behavior.
                    behavior = PluginBehavior.ProxyHandler.getProxy(behavior);
                    plugin.setBehavior(behavior);
                    listener.onGetBehavior(request, behavior);
                }

            } catch (LoadError error) {
                Log.w(TAG, "Create behavior fail.");
                Log.w(TAG, error);
            }
        }
    }

}
