/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia.bridge;

/**
 * 宿主API的基类，插件通过这些接口调用宿主的API；
 * 插件只能访问API接口，具体实现是透明的，由宿主通过ApiManager注册；
 */
// TODO: 16/12/2 Improve host api. 
public abstract class HostApi {
    
    protected int version;

    /**
     * Api 版本号
     */
    public int getVersion() {
        return version;
    }

    /**
     * 是否存在目标方法，插件项目调用前查询
     *
     * @param name 方法名
     * @return 是否存在
     */
    public boolean isMethodExist(String name) {
        return false;
    }
}
