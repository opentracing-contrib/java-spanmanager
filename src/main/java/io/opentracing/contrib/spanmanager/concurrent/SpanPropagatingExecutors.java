package io.opentracing.contrib.spanmanager.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Factory-methods similar to standard java {@link Executors}:
 * <ul>
 * <li>{@link #newFixedThreadPool(int)}</li>
 * <li>{@link #newSingleThreadExecutor()}</li>
 * <li>{@link #newCachedThreadPool()}</li>
 * <li>Variants of the above with additional {@link ThreadFactory} argument:
 * {@link #newFixedThreadPool(int, ThreadFactory)},
 * {@link #newSingleThreadExecutor(ThreadFactory)},
 * {@link #newCachedThreadPool(ThreadFactory)}
 * </li>
 * </ul>
 *
 * @see SpanPropagatingExecutorService#wrap(ExecutorService)
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
     * @param nThreads the number of threads in the pool
     * @return the newly created thread pool
     * @see Executors#newFixedThreadPool(int)
     */
    public static SpanPropagatingExecutorService newFixedThreadPool(int nThreads) {
        return SpanPropagatingExecutorService.wrap(Executors.newFixedThreadPool(nThreads));
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
    public static SpanPropagatingExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return SpanPropagatingExecutorService.wrap(Executors.newFixedThreadPool(nThreads, threadFactory));
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor() single-threaded executor} that propagates
     * the active span into the started thread.
     *
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor()
     */
    public static SpanPropagatingExecutorService newSingleThreadExecutor() {
        return SpanPropagatingExecutorService.wrap(Executors.newSingleThreadExecutor());
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor(ThreadFactory) single-threaded executor}
     * that propagates the active span into the started thread.
     *
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor(ThreadFactory)
     */
    public static SpanPropagatingExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return SpanPropagatingExecutorService.wrap(Executors.newSingleThreadExecutor(threadFactory));
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool() cached threadpool} that propagates
     * the active span into the started threads.
     *
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool()
     */
    public static SpanPropagatingExecutorService newCachedThreadPool() {
        return SpanPropagatingExecutorService.wrap(Executors.newCachedThreadPool());
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool(ThreadFactory) cached threadpool} that propagates
     * the active span into the started threads.
     *
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool(ThreadFactory)
     */
    public static SpanPropagatingExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return SpanPropagatingExecutorService.wrap(Executors.newCachedThreadPool(threadFactory));
    }

}
