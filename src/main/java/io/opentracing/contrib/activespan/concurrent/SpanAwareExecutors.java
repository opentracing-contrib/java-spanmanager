package io.opentracing.contrib.activespan.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class SpanAwareExecutors {

    /**
     * Private constructor to avoid instantiation of this utility class.
     */
    private SpanAwareExecutors() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method returns a {@link Executors#newFixedThreadPool(int) fixed threadpool} that propagates
     * the active span into the started threads.
     *
     * @param nThreads the number of threads in the pool
     * @return the newly created thread pool
     * @see Executors#newFixedThreadPool(int)
     */
    public static SpanAwareExecutorService newFixedThreadPool(int nThreads) {
        return SpanAwareExecutorService.wrap(Executors.newFixedThreadPool(nThreads));
    }

    /**
     * This method returns a {@link Executors#newFixedThreadPool(int, ThreadFactory) fixed threadpool} that propagates
     * the active span into the started threads.
     *
     * @param nThreads      the number of threads in the pool
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created thread pool
     * @see Executors#newFixedThreadPool(int, ThreadFactory)
     */
    public static SpanAwareExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return SpanAwareExecutorService.wrap(Executors.newFixedThreadPool(nThreads, threadFactory));
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor() single-threaded executor} that propagates
     * the active span into the started thread.
     *
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor()
     */
    public static SpanAwareExecutorService newSingleThreadExecutor() {
        return SpanAwareExecutorService.wrap(Executors.newSingleThreadExecutor());
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor(ThreadFactory) single-threaded executor}
     * that propagates the active span into the started thread.
     *
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor(ThreadFactory)
     */
    public static SpanAwareExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return SpanAwareExecutorService.wrap(Executors.newSingleThreadExecutor(threadFactory));
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool() cached threadpool} that propagates
     * the active span into the started threads.
     *
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool()
     */
    public static SpanAwareExecutorService newCachedThreadPool() {
        return SpanAwareExecutorService.wrap(Executors.newCachedThreadPool());
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool(ThreadFactory) cached threadpool} that propagates
     * the active span into the started threads.
     *
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool(ThreadFactory)
     */
    public static SpanAwareExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return SpanAwareExecutorService.wrap(Executors.newCachedThreadPool(threadFactory));
    }

}
