package com.github.libsml.commons.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * Created by huangyu on 15/9/10.
 */
public class ThreadUtils {

    /**
     * Create a thread factory that names threads with a prefix and also sets the threads to daemon.
     */
    public static ThreadFactory namedThreadFactory(String prefix)

    {
        return new ThreadFactoryBuilder().setDaemon(true).setNameFormat(prefix + "-%d").build();
    }

    /**
     * Wrapper over newCachedThreadPool. Thread names are formatted as prefix-ID, where ID is a
     * unique, sequentially assigned integer.
     */
    public ThreadPoolExecutor newDaemonCachedThreadPool(String prefix)

    {
        ThreadFactory threadFactory = namedThreadFactory(prefix);
        return (ThreadPoolExecutor) Executors.newCachedThreadPool(threadFactory);
    }

    /**
     * Create a cached thread pool whose max number of threads is `maxThreadNumber`. Thread names
     * are formatted as prefix-ID, where ID is a unique, sequentially assigned integer.
     */
    public static ThreadPoolExecutor newDaemonCachedThreadPool(String prefix, int maxThreadNumber)

    {
        ThreadFactory threadFactory = namedThreadFactory(prefix);
        return new ThreadPoolExecutor(
                0, maxThreadNumber, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), threadFactory);
    }

    /**
     * Wrapper over newFixedThreadPool. Thread names are formatted as prefix-ID, where ID is a
     * unique, sequentially assigned integer.
     */
    public static ThreadPoolExecutor newDaemonFixedThreadPool(int nThreads, String prefix) {
        ThreadFactory threadFactory = namedThreadFactory(prefix);
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads, threadFactory);
    }

    /**
     * Wrapper over newSingleThreadExecutor.
     */
    public static ExecutorService newDaemonSingleThreadExecutor(String threadName) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat(threadName).build();
        return Executors.newSingleThreadExecutor(threadFactory);
    }

    /**
     * Wrapper over newSingleThreadScheduledExecutor.
     */
    public static ScheduledExecutorService newDaemonSingleThreadScheduledExecutor(String threadName) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).
                setNameFormat(threadName).build();
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }
}
