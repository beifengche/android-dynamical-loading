package moe.studio.frontia.core;

/**
 * 插件异常
 */

public class PluginErrors {
    /**
     * 失败重试超标
     */
    public static class RetryError extends Exception {
        public RetryError() {
            super("Reach max retry.");
        }
    }
}
