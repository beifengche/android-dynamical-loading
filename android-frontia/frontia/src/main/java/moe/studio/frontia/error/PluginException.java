/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package moe.studio.frontia.error;

/**
 * Created by Kaede on 2016/4/27.
 * 插件化框架用Exception类
 */
public class PluginException extends Exception {

    private int mCode;

    public PluginException() {
    }

    public PluginException(String detailMessage) {
        super(detailMessage);
    }

    public PluginException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PluginException(Throwable throwable) {
        super(throwable);
    }

    public PluginException attach(int errorCode) {
        mCode = errorCode;
        return this;
    }
}
