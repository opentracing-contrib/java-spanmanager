package io.opentracing.contrib.activespan.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static io.opentracing.contrib.activespan.concurrent.SpanAwareExecutorService.wrap;

/**
 * @author Sjoerd Talsma
 * @navassoc - delegatesTo - Executors
 */
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
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return wrap(Executors.newFixedThreadPool(nThreads));
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
    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return wrap(Executors.newFixedThreadPool(nThreads, threadFactory));
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor() single-threaded executor} that propagates
     * the active span into the started thread.
     *
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor()
     */
    public static ExecutorService newSingleThreadExecutor() {
        return wrap(Executors.newSingleThreadExecutor());
    }

    /**
     * This method returns a {@link Executors#newSingleThreadExecutor(ThreadFactory) single-threaded executor}
     * that propagates the active span into the started thread.
     *
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created single-theaded executor
     * @see Executors#newSingleThreadExecutor(ThreadFactory)
     */
    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return wrap(Executors.newSingleThreadExecutor(threadFactory));
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool() cached threadpool} that propagates
     * the active span into the started threads.
     *
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool()
     */
    public static ExecutorService newCachedThreadPool() {
        return wrap(Executors.newCachedThreadPool());
    }

    /**
     * This method returns a {@link Executors#newCachedThreadPool(ThreadFactory) cached threadpool} that propagates
     * the active span into the started threads.
     *
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created thread pool
     * @see Executors#newCachedThreadPool(ThreadFactory)
     */
    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return wrap(Executors.newCachedThreadPool(threadFactory));
    }

}
