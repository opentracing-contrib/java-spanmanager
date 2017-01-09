package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.contrib.spanmanager.SpanManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Factory-methods similar to standard java {@link Executors}:
 * <ul>
 * <li>{@link #newFixedThreadPool(int, SpanManager)}</li>
 * <li>{@link #newSingleThreadExecutor(SpanManager)}</li>
 * <li>{@link #newCachedThreadPool(SpanManager)}</li>
 * <li>Variants of the above with additional {@link ThreadFactory} argument:
 * {@link #newFixedThreadPool(int, ThreadFactory, SpanManager)},
 * {@link #newSingleThreadExecutor(ThreadFactory, SpanManager)},
 * {@link #newCachedThreadPool(ThreadFactory, SpanManager)}
 * </li>
 * </ul>
 *
 * @see SpanPropagatingExecutorService
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
     * the active span into the started threads.
     *
     * @param nThreads    the number of threads in the pool
     * @param spanManager the manager for span propagation.
     * @return the newly created thread pool
     * @see Executors#newFixedThreadPool(int)
     */
    public static SpanPropagatingExecutorService newFixedThreadPool(int nThreads, SpanManager spanManager) {
        return new SpanPropagatingExecutorService(Executors.newFixedThreadPool(nThreads), spanManager);
    }

    /**
     * This method returns a {@link Executors#newFixedThreadPool(int, ThreadFactory) fixed threadpool} that propagates
     * the active span into the started threads.
     *
     * @param nThreads      the number of threads in the pool
     * @param threadFactory the factory to use when creating new threads
     * @param spanManager   the manager for span propagation.
     * @return the newly created thread pool
     * @see Executors#newFixedThreadPool(int, ThreadFactory)
     */
    public static SpanPropagatingExecutorService newFixedThreadPool(
            int nThreads, ThreadFactory threadFactory, SpanManager spanManager) {
        return new SpanPropagatingExecutorService(Executors.newFixedThreadPool(nThreads, threadFactory), spanManager);
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor() single-threaded executor} that propagates
     * the active span into the started thread.
     *
     * @param spanManager the manager for span propagation.
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor()
     */
    public static SpanPropagatingExecutorService newSingleThreadExecutor(SpanManager spanManager) {
        return new SpanPropagatingExecutorService(Executors.newSingleThreadExecutor(), spanManager);
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor(ThreadFactory) single-threaded executor}
     * that propagates the active span into the started thread.
     *
     * @param threadFactory the factory to use when creating new threads
     * @param spanManager   the manager for span propagation.
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor(ThreadFactory)
     */
    public static SpanPropagatingExecutorService newSingleThreadExecutor(
            ThreadFactory threadFactory, SpanManager spanManager) {
        return new SpanPropagatingExecutorService(Executors.newSingleThreadExecutor(threadFactory), spanManager);
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool() cached threadpool} that propagates
     * the active span into the started threads.
     *
     * @param spanManager the manager for span propagation.
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool()
     */
    public static SpanPropagatingExecutorService newCachedThreadPool(SpanManager spanManager) {
        return new SpanPropagatingExecutorService(Executors.newCachedThreadPool(), spanManager);
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool(ThreadFactory) cached threadpool} that propagates
     * the active span into the started threads.
     *
     * @param threadFactory the factory to use when creating new threads
     * @param spanManager   the manager for span propagation.
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool(ThreadFactory)
     */
    public static SpanPropagatingExecutorService newCachedThreadPool(ThreadFactory threadFactory, SpanManager spanManager) {
        return new SpanPropagatingExecutorService(Executors.newCachedThreadPool(threadFactory), spanManager);
    }

}
