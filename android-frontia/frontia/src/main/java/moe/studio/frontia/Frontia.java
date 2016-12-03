/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package moe.studio.frontia;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import moe.studio.frontia.bridge.plugin.BaseBehaviour;
import moe.studio.frontia.core.Plugin;
import moe.studio.frontia.core.PluginInstaller;
import moe.studio.frontia.core.PluginLoader;
import moe.studio.frontia.core.PluginManager;
import moe.studio.frontia.core.PluginRequest;
import moe.studio.frontia.core.PluginSetting;
import moe.studio.frontia.core.PluginUpdater;
import moe.studio.frontia.error.IllegalPluginException;

/**
 * 插件管理器：
 * 1. 启动插件更新、安装、加载任务；
 * 2. 提供对任务的控制；
 * <p>
 * 乗り越えれ、Frontier！我が目立ては「Star Oceans」！
 */
public class Frontia implements PluginManager {

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
    private PluginLoader mLoader;
    private PluginUpdater mUpdater;
    private PluginInstaller mInstaller;
    private PluginSetting mSetting;
    private Handler mCallbackHandler;
    private ExecutorService mExecutorService;
    private final Map<Class<? extends PluginRequest>, RequestState> mRequestStates;

    private Frontia() {
        mRequestStates = new HashMap<>();
    }

    /**
     * 初始化
     */
    public void init(Context context) {
        if (!mHasInit) {
            synchronized (mRequestStates) {
                if (!mHasInit) {
                    mHasInit = true;
                    mSetting = new PluginSetting.Builder()
                            .setDebugMode(Logger.DEBUG)
                            .ignoreInstalledPlugin(Logger.DEBUG)
                            .build();
                    mCallbackHandler = new Handler(Looper.getMainLooper());
                    mExecutorService = Executors.newSingleThreadExecutor();
                    mLoader = new PluginLoaderImpl(context, this);
                    mUpdater = new PluginUpdaterImpl(context, this);
                    mInstaller = new PluginInstallerImpl(context, this);
                    printDebugInfo();
                    return;
                }
            }
        }
        Logger.w(TAG, "Frontia has already been initialized.");
    }


    /**
     * 初始化
     *
     * @param context Context
     * @param setting 插件设置
     */
    public void init(Context context, @NonNull PluginSetting setting) {
        if (!mHasInit) {
            synchronized (mRequestStates) {
                if (!mHasInit) {
                    mHasInit = true;
                    mSetting = setting;
                    mCallbackHandler = new Handler(Looper.getMainLooper());
                    mExecutorService = Executors.newSingleThreadExecutor();
                    mLoader = new PluginLoaderImpl(context, this);
                    mUpdater = new PluginUpdaterImpl(context, this);
                    mInstaller = new PluginInstallerImpl(context, this);
                    printDebugInfo();
                    return;
                }
            }
        }
        Logger.w(TAG, "Frontia has already been initialized.");
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
            synchronized (mRequestStates) {
                if (!mHasInit) {
                    mHasInit = true;
                    mSetting = setting;
                    mCallbackHandler = callbackHandler;
                    mExecutorService = executorService;
                    mLoader = new PluginLoaderImpl(context, this);
                    mUpdater = new PluginUpdaterImpl(context, this);
                    mInstaller = new PluginInstallerImpl(context, this);
                    printDebugInfo();
                    return;
                }
            }
        }
        Logger.w(TAG, "Frontia has already been initialized.");
    }


    @Override
    public PluginRequest add(@NonNull PluginRequest request, int mode) {
        Task.doing(this, mode).doing(request);
        Logger.i(TAG, "request id = " + request.getId()
                + ", state log = " + request.getLog());
        return request;
    }

    /**
     * 异步加载一个插件
     *
     * @param request 插件请求
     * @param mode    加载模式, 参考{@linkplain Frontia.Mode}
     * @return 当前插件请求
     */
    public RequestState addAsync(@NonNull final PluginRequest request, final int mode) {
        RequestState requestState = mRequestStates.get(request.getClass());

        // Cancel if exist.
        if (requestState != null) {
            requestState.cancel();
        }

        request.attach(this);
        Future<PluginRequest> future = mExecutorService.submit(new Callable<PluginRequest>() {
            @Override
            public PluginRequest call() throws Exception {
                Task.doing(Frontia.this, mode).doing(request);

                Logger.i(TAG, "request id = " + request.getId()
                        + ", state log = " + request.getLog());
                return request;
            }
        });

        requestState = new RequestState(request, future);
        mRequestStates.put(request.getClass(), requestState);
        return requestState;
    }

    /**
     * 获取插件加载任务的状态
     */
    public RequestState getRequestState(Class<? extends PluginRequest> clazz) {
        return mRequestStates.get(clazz);
    }


    @Override
    public PluginSetting getSetting() {
        return mSetting;
    }

    @Override
    public PluginLoader getLoader() {
        return mLoader;
    }

    @Override
    public PluginUpdater getUpdater() {
        return mUpdater;
    }

    @Override
    public PluginInstaller getInstaller() {
        return mInstaller;
    }

    public ExecutorService getExecutor() {
        return mExecutorService;
    }

    public Handler getCallbackHandler() {
        return mCallbackHandler;
    }

    @Override
    public Class loadClass(@NonNull Plugin plugin, String className) throws IllegalPluginException {
        return mLoader.loadClass(plugin, className);

    }

    @Override
    public BaseBehaviour getBehaviour(Plugin plugin) throws IllegalPluginException {
        return mLoader.getBehaviour(plugin);
    }

    private void printDebugInfo() {
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
         * 插件请求任务是否失败
         */
        public boolean isFailed() {
            return mRequest.isUpdateFail();
        }

        /**
         * 取消插件请求任务
         */
        public void cancel() {
            mRequest.getController().cancel();
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

            } catch (InterruptedException e) {
                e.printStackTrace();
                pluginRequest = mRequest;
                Logger.i(TAG, "Get future request fail, error = " + e.getMessage());
            } catch (ExecutionException | TimeoutException e) {
                e.printStackTrace();
                pluginRequest = mRequest.markException(e);
            }

            return pluginRequest;
        }

    }

    private abstract static class Task {
        static Task doing(Frontia manager, int mode) {
            Task task;
            switch (mode) {
                case Mode.MODE_UPDATE:                   // Only update plugin.
                    task = new Update(manager);
                    break;
                case Mode.MODE_LOAD:                     // Only load plugin.
                    task = new Load(manager);
                    break;
                case Mode.MODE_UPDATE | Mode.MODE_LOAD:  // Update and load plugin.
                default:
                    task = new UpdateAndLoad(manager);
                    break;
            }
            return task;
        }

        final Frontia mManager;

        Task(Frontia manager) {
            mManager = manager;
        }

        abstract void doing(PluginRequest request);


        /* Impl */
        static class Update extends Task {
            Update(Frontia manager) {
                super(manager);
            }

            @Override
            void doing(PluginRequest request) {
                mManager.getUpdater().updatePlugin(request);
            }
        }

        static class Load extends Task {
            Load(Frontia manager) {
                super(manager);
            }

            @Override
            void doing(PluginRequest request) {
                mManager.getLoader().loadPlugin(request);
            }
        }

        static class UpdateAndLoad extends Task {
            UpdateAndLoad(Frontia manager) {
                super(manager);
            }

            @Override
            void doing(PluginRequest request) {
                new Update(mManager).doing(request);
                new Load(mManager).doing(request);
            }
        }
    }
}
