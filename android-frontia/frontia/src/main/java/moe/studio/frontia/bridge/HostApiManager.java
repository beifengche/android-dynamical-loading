/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia.bridge;

import java.util.HashMap;

/**
 * 宿主API的管理类，宿主通过此管理类注册API的实现
 */
public final class HostApiManager {

    private static HostApiManager instance = new HostApiManager();

    HashMap<Class<? extends HostApi>, HostApi> apiMap;

    public static HostApiManager getInstance() {
        return instance;
    }

    private HostApiManager() {
        apiMap = new HashMap<>();
    }

    public boolean containsApi(Class<? extends HostApi> clazz) {
        return apiMap.containsKey(clazz);
    }

    public <T extends HostApi> T getApi(Class<T> clazz) {
        return (T) apiMap.get(clazz);
    }

    public void putApi(Class<? extends HostApi> key, HostApi value) {
        apiMap.put(key, value);
    }

}
