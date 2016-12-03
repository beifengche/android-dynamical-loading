/*
 * Copyright (c) 2016. Kaede
 */

package moe.studio.frontia;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import moe.studio.frontia.core.Plugin;
import moe.studio.frontia.core.PluginBehavior;
import moe.studio.frontia.core.PluginInstaller;
import moe.studio.frontia.core.PluginLoader;
import moe.studio.frontia.core.PluginManager;
import moe.studio.frontia.core.PluginRequest;
import moe.studio.frontia.core.PluginUpdater;
import moe.studio.frontia.ext.PluginCallback;
import moe.studio.frontia.ext.PluginError.LoadError;
import moe.studio.frontia.ext.PluginSetting;

import static moe.studio.frontia.Frontia.Mode.MODE_LOAD;
import static moe.studio.frontia.Frontia.Mode.MODE_UPDATE;
import static moe.studio.frontia.ext.PluginError.ERROR_LOA_NOT_LOADED;

/**
 * Frontia
 * 1. 启动插件更新、安装、加载任务;
 * 2. 提供对任务的控制;
 * 3. 提供获取插件的方法;
 * <p>
 * 乗り越えれ、Frontier！我が目立ては「Star Oceans」！
 */
@SuppressWarnings({"WeakerAccess", "SuspiciousMethodCalls", "unchecked"})
public final class Frontia implements PluginManager {

    private static final String TAG = "plugin.manager";
    private static Frontia sInstance;

    public static Frontia instance() {
        if (sInstance == null) {
            synchronized (Frontia.class) {
                if (sInstance == null) {
                    sInstance = new Frontia();
                }
            }
        }
        return sInstance;
    }

    private boolean mHasInit = false;
    private final byte[] mLock = new byte[0];
    private PluginLoader mLoader;
    private PluginUpdater mUpdater;
    private PluginInstaller mInstaller;
    private PluginSetting mSetting;
    private PluginCallback mController;
    private ExecutorService mExecutorService;
    private Map<Class<? extends PluginBehavior>, Plugin> mLoadedPlugins;
    private Map<Class<? extends PluginRequest>, RequestState> mRequestStates;

    public Frontia() {

    }

    /**
     * 初始化
     */
    public void init(Context context) {
        if (!mHasInit) {
            synchronized (mLock) {
                if (!mHasInit) {
                    mHasInit = true;
                    mSetting = new PluginSetting.Builder()
                            .setDebugMode(Logger.DEBUG)
                            .ignoreInstalledPlugin(Logger.DEBUG)
                            .build();
                    mExecutorService = Executors.newSingleThreadExecutor();
                    mLoader = new PluginLoaderImpl(context, this);
                    mUpdater = new PluginUpdaterImpl(context, this);
                    mInstaller = new PluginInstallerImpl(context, this);
                    mController = new CallbackDelivery(this, new Handler(Looper.getMainLooper()));
                    printDebugInfo();
                    return;
                }
            }
        }

        throw new RuntimeException("Frontia has already been initialized.");
    }

    /**
     * 初始化
     *
     * @param context Context
     * @param setting 插件设置
     */
    public void init(Context context, @NonNull PluginSetting setting) {
        if (!mHasInit) {
            synchronized (mLock) {
                if (!mHasInit) {
                    mHasInit = true;
                    mSetting = setting;
                    mExecutorService = Executors.newSingleThreadExecutor();
                    mLoader = new PluginLoaderImpl(context, this);
                    mUpdater = new PluginUpdaterImpl(context, this);
                    mInstaller = new PluginInstallerImpl(context, this);
                    mController = new CallbackDelivery(this, new Handler(Looper.getMainLooper()));
                    printDebugInfo();
                    return;
                }
            }
        }

        throw new RuntimeException("Frontia has already been initialized.");
    }

    /**
     * 初始化
     *
     * @param context         Context
     * @param setting         插件设置
     * @param callbackHandler 监听器回调用Handler
     * @param executorService 异步加载插件任务用的线程池
     */
    public void init(Context context, @NonNull PluginSetting setting,
                     @NonNull Handler callbackHandler, @NonNull ExecutorService executorService) {
        if (!mHasInit) {
            synchronized (mLock) {
                if (!mHasInit) {
                    mHasInit = true;
                    mSetting = setting;
                    mExecutorService = executorService;
                    mLoader = new PluginLoaderImpl(context, this);
                    mUpdater = new PluginUpdaterImpl(context, this);
                    mInstaller = new PluginInstallerImpl(context, this);
                    mController = new CallbackDelivery(this, callbackHandler);
                    printDebugInfo();
                    return;
                }
            }
        }

        throw new RuntimeException("Frontia has already been initialized.");
    }


    /**
     * 同步加载一个插件
     *
     * @param request 插件请求
     * @param mode    加载模式, 参考{@link Mode}
     * @return 当前插件请求
     */
    public PluginRequest add(@NonNull PluginRequest request, int mode) {
        return add(request, JobToDo.doing(this, mode));
    }

    /**
     * 同步加载一个插件
     *
     * @param request 插件请求
     * @param job     做神马, 参考{@link Mode}
     * @return 当前插件请求
     */
    public PluginRequest add(@NonNull PluginRequest request, @NonNull JobToDo job) {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        Logger.i(TAG, "request id = " + request.getId() + ", state log = " + request.getLog());
        job.doing(request.attach(this));
        return request;
    }

    /**
     * 异步加载一个插件
     * <p>
     * 如果需要获取一个已经正在运行的插件加载任务的状态, 可以通过{@link #getRequestState(Class)}获得。
     *
     * @param request 插件请求
     * @param mode    加载模式, 参考{@linkplain Frontia.Mode}
     * @return 当前插件请求
     */
    public RequestState addAsync(@NonNull PluginRequest request, int mode) {
        return addAsync(request, JobToDo.doing(this, mode));
    }

    /**
     * 异步加载一个插件
     * <p>
     * 如果需要获取一个已经正在运行的插件加载任务的状态, 可以通过{@link #getRequestState(Class)}获得。
     *
     * @param request 插件请求
     * @param job     做神马, 参考{@link Mode}
     * @return 当前插件请求
     */
    public RequestState addAsync(@NonNull final PluginRequest request, @NonNull final JobToDo job) {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        mRequestStates = ensureHashMap(mRequestStates);
        RequestState requestState = mRequestStates.get(request.getClass());

        // Cancel if exist.
        if (requestState != null) {
            requestState.cancel();
        }

        request.attach(this);
        Future<PluginRequest> future = mExecutorService.submit(new Callable<PluginRequest>() {
            @Override
            public PluginRequest call() throws Exception {
                return add(request, job);
            }
        });

        requestState = new RequestState(request, future);
        mRequestStates.put(request.getClass(), requestState);
        return requestState;
    }

    /**
     * 获取插件加载任务的状态
     */
    @Nullable
    public RequestState getRequestState(Class<? extends PluginRequest> clazz) {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        return mRequestStates == null || mRequestStates == Collections.EMPTY_MAP ?
                null : mRequestStates.get(clazz);
    }

    @Override
    public Class getClass(Class<? extends Plugin> clazz, String className) throws LoadError {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        if (mLoadedPlugins == null || mLoadedPlugins == Collections.EMPTY_MAP) {
            return null;
        }

        Plugin plugin = mLoadedPlugins.get(clazz);

        if (plugin == null) {
            throw new LoadError("Plugin has not yet been loaded.", ERROR_LOA_NOT_LOADED);
        }

        return mLoader.loadClass(plugin, className);
    }

    @Override
    public <B extends PluginBehavior, P extends Plugin<B>> B getBehavior(P clazz) throws LoadError {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        if (mLoadedPlugins == null || mLoadedPlugins == Collections.EMPTY_MAP) {
            return null;
        }

        Plugin plugin = mLoadedPlugins.get(clazz);

        if (plugin != null) {
            PluginBehavior behavior = plugin.getBehavior();
            if (behavior != null) {
                return (B) behavior;
            }
        }

        throw new LoadError("Plugin has not yet been loaded.", ERROR_LOA_NOT_LOADED);
    }

    @Override
    public <B extends PluginBehavior, P extends Plugin<B>> P getPlugin(P clazz) {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        return mLoadedPlugins == null || mLoadedPlugins == Collections.EMPTY_MAP ?
                null : (P) mLoadedPlugins.get(clazz);
    }

    @Override
    public void addLoadedPlugin(Class<? extends PluginBehavior> clazz, Plugin plugin) {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        mLoadedPlugins = ensureHashMap(mLoadedPlugins);
        mLoadedPlugins.put(clazz, plugin);
    }

    private Map ensureHashMap(Map map) {
        if (map == null || map == Collections.EMPTY_MAP) {
            map = new HashMap();
        }

        return map;
    }

    @Override
    public PluginSetting getSetting() {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        return mSetting;
    }

    @Override
    public PluginLoader getLoader() {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        return mLoader;
    }

    @Override
    public PluginUpdater getUpdater() {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        return mUpdater;
    }

    @Override
    public PluginInstaller getInstaller() {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        return mInstaller;
    }

    @Override
    public PluginCallback getCallback() {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        return mController;
    }

    public ExecutorService getExecutor() {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        return mExecutorService;
    }

    private void printDebugInfo() {
        if (!mHasInit) {
            throw new RuntimeException("Frontia has not yet been init.");
        }

        if (Logger.DEBUG) {
            Logger.v(TAG, "-");
            Logger.v(TAG, "Frontia init");
            Logger.v(TAG, "Debug Mode = " + mSetting.isDebugMode());
            Logger.v(TAG, "Ignore Installed Plugin = " + mSetting.ignoreInstalledPlugin());
            Logger.v(TAG, "Use custom signature = " + mSetting.useCustomSignature());
            Logger.v(TAG, "--");
            Internals.FileUtils.dumpFiles(new File(mInstaller.getRootPath()));
            Logger.v(TAG, "--");
            Logger.v(TAG, "-");
        }
    }

    /**
     * 插件加载任务的状态类
     */
    public static class RequestState {

        private final PluginRequest mRequest;
        private final Future<PluginRequest> mFuture;

        public RequestState(PluginRequest request, Future<PluginRequest> future) {
            mRequest = request;
            mFuture = future;
        }

        /**
         * 取消插件请求任务
         */
        public void cancel() {
            mRequest.cancel();
            mFuture.cancel(true);
        }

        /**
         * 获取插件请求任务
         */
        public PluginRequest getRequest() {
            return mRequest;
        }

        /**
         * 同步等待插件任务执行结束并返回插件请求任务
         *
         * @param timeout 超时时间
         * @return 插件请求任务
         */
        public PluginRequest getFutureRequest(long timeout) {
            PluginRequest pluginRequest;
            try {
                pluginRequest = mFuture.get(timeout, TimeUnit.MILLISECONDS);

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Logger.i(TAG, "Get future request fail, error = " + e.getMessage());
                Logger.w(TAG, e);
                pluginRequest = mRequest.markException(e);
            }
            return pluginRequest;
        }

    }

    /**
     * 插件加载的模式
     */
    public static class Mode {
        /**
         * 更新插件, 从远程下载最新版本的插件并拷贝到对应的安装路径
         */
        public static final int MODE_UPDATE = 0x0001;
        /**
         * 从对应的安装路径加载插件
         */
        public static final int MODE_LOAD = 0x0010;
    }

    /**
     * 告诉Frontia当前任务要做什么
     */
    public abstract static class JobToDo {

        private static JobToDo doing(Frontia manager, int mode) {
            JobToDo task;
            switch (mode) {
                case MODE_UPDATE:               // Only update plugin.
                    task = new Update(manager);
                    break;
                case MODE_LOAD:                 // Only load plugin.
                    task = new Load(manager);
                    break;
                case MODE_UPDATE | MODE_LOAD:   // Update and load plugin.
                default:
                    task = new UpdateAndLoad(manager);
                    break;
            }
            return task;
        }

        final PluginManager mManager;

        public JobToDo(PluginManager manager) {
            mManager = manager;
        }

        public abstract void doing(PluginRequest request);

        /* Impl */
        private static class Update extends JobToDo {

            Update(PluginManager manager) {
                super(manager);
            }

            @Override
            public void doing(PluginRequest request) {
                mManager.getUpdater().updatePlugin(request);
            }

        }

        private static class Load extends JobToDo {

            Load(PluginManager manager) {
                super(manager);
            }

            @Override
            public void doing(PluginRequest request) {
                mManager.getLoader().load(request);
            }

        }

        private static class UpdateAndLoad extends JobToDo {

            UpdateAndLoad(PluginManager manager) {
                super(manager);
            }

            @Override
            public void doing(PluginRequest request) {
                new Update(mManager).doing(request);
                new Load(mManager).doing(request);
            }
        }
    }
}
