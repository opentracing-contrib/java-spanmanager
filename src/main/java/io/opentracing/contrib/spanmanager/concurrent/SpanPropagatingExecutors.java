package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.contrib.spanmanager.SpanManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Factory-methods similar to standard java {@link Executors}:
 * <ul>
 * <li>{@link #newFixedThreadPool(int)}</li>
 * <li>{@link #newSingleThreadExecutor()}</li>
 * <li>{@link #newCachedThreadPool()}</li>
 * <li>Variants of the above with additional {@link ThreadFactory} and {@link SpanManager} arguments:
 * {@link #newFixedThreadPool(int, ThreadFactory, SpanManager)},
 * {@link #newSingleThreadExecutor(ThreadFactory, SpanManager)},
 * {@link #newCachedThreadPool(ThreadFactory, SpanManager)}
 * </li>
 * </ul>
 *
 * @see SpanPropagatingExecutorService#of(ExecutorService)
 */
public final class SpanPropagatingExecutors {

    /**
     * Private constructor to avoid instantiation of this utility class.
     */
    private SpanPropagatingExecutors() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method returns a {@link Executors#newFixedThreadPool(int) fixed threadpool} that propagates
     * the active span into the started threads, using the global span manager.
     *
     * @param nThreads the number of threads in the pool
     * @return the newly created thread pool
     * @see Executors#newFixedThreadPool(int)
     * @see io.opentracing.contrib.spanmanager.GlobalSpanManager
     */
    public static SpanPropagatingExecutorService newFixedThreadPool(int nThreads) {
        return SpanPropagatingExecutorService.of(Executors.newFixedThreadPool(nThreads));
    }

    /**
     * This method returns a {@link Executors#newFixedThreadPool(int, ThreadFactory) fixed threadpool} that propagates
     * the active span into the started threads.
     *
     * @param nThreads      the number of threads in the pool
     * @param threadFactory the factory to use when creating new threads
     * @param spanManager   the SpanManager to manage spans with.
     * @return the newly created thread pool
     * @see Executors#newFixedThreadPool(int, ThreadFactory)
     */
    public static SpanPropagatingExecutorService newFixedThreadPool(
            int nThreads, ThreadFactory threadFactory, SpanManager spanManager) {
        return SpanPropagatingExecutorService.of(Executors.newFixedThreadPool(nThreads, threadFactory), spanManager);
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor() single-threaded executor} that propagates
     * the active span into the started thread using the global span manager.
     *
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor()
     * @see io.opentracing.contrib.spanmanager.GlobalSpanManager
     */
    public static SpanPropagatingExecutorService newSingleThreadExecutor() {
        return SpanPropagatingExecutorService.of(Executors.newSingleThreadExecutor());
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor(ThreadFactory) single-threaded executor}
     * that propagates the active span into the started thread.
     *
     * @param threadFactory the factory to use when creating new threads
     * @param spanManager   the SpanManager to manage spans with.
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor(ThreadFactory)
     */
    public static SpanPropagatingExecutorService newSingleThreadExecutor(
            ThreadFactory threadFactory, SpanManager spanManager) {
        return SpanPropagatingExecutorService.of(Executors.newSingleThreadExecutor(threadFactory), spanManager);
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool() cached threadpool} that propagates
     * the active span into the started threads using the global span manager.
     *
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool()
     * @see io.opentracing.contrib.spanmanager.GlobalSpanManager
     */
    public static SpanPropagatingExecutorService newCachedThreadPool() {
        return SpanPropagatingExecutorService.of(Executors.newCachedThreadPool());
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool(ThreadFactory) cached threadpool} that propagates
     * the active span into the started threads.
     *
     * @param threadFactory the factory to use when creating new threads
     * @param spanManager   the SpanManager to manage spans with.
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool(ThreadFactory)
     */
    public static SpanPropagatingExecutorService newCachedThreadPool(ThreadFactory threadFactory, SpanManager spanManager) {
        return SpanPropagatingExecutorService.of(Executors.newCachedThreadPool(threadFactory), spanManager);
    }

}
