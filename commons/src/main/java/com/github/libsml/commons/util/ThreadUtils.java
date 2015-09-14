package com.github.libsml.commons.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Created by huangyu on 15/9/10.
 */
public class ThreadUtils {
    /**
     * Wrapper over newSingleThreadScheduledExecutor.
     */
    public static ScheduledExecutorService newDaemonSingleThreadScheduledExecutor(String threadName) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).
                setNameFormat(threadName).build();
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }
}
