package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.ActiveSpanManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Propagates the {@link ActiveSpanManager#activeSpan() active span} from the caller into each call that is executed.
 * <p>
 * <em>Note:</em> The active span is merely propagated. It is explicitly <b>not</b> finished by the calls.
 */
public class SpanPropagatingExecutorService implements ExecutorService {
    private final ExecutorService delegate;

    private SpanPropagatingExecutorService(ExecutorService delegate) {
        if (delegate == null) throw new NullPointerException("Delegate executor service is <null>.");
        this.delegate = delegate;
    }

    /**
     * Wraps the delegate ExecutorService to propagate the {@link ActiveSpanManager#activeSpan() active span}
     * of callers into the executed calls.
     *
     * @param delegate The executorservice to forward calls to.
     * @return An ExecutorService that propagates active spans from callers into executed calls.
     */
    public static SpanPropagatingExecutorService wrap(final ExecutorService delegate) {
        if (delegate instanceof SpanPropagatingExecutorService) return (SpanPropagatingExecutorService) delegate;
        return new SpanPropagatingExecutorService(delegate);
    }

    /**
     * Propagates the {@link ActiveSpanManager#activeSpan() currently active span} into the runnable
     * and performs cleanup afterwards.
     * <p>
     * <em>Note:</em> The <code>activeSpan</code> is merely propagated.
     * The specified span is explicitly <b>not</b> finished by the runnable.
     *
     * @param runnable   The runnable to be executed.
     * @param activeSpan The span to be propagated.
     * @return The wrapped runnable to execute with the active span.
     */
    public static Runnable runnableWithActiveSpan(Runnable runnable, Span activeSpan) {
        return new RunnableWithActiveSpan(runnable, activeSpan);
    }

    /**
     * Propagates the {@link ActiveSpanManager#activeSpan() currently active span} into the callable
     * and performs cleanup afterwards.
     * <p>
     * <em>Note:</em> The <code>activeSpan</code> is merely propagated.
     * The specified span is explicitly <b>not</b> finished by the callable.
     *
     * @param <T>        The callable result type.
     * @param callable   The callable to be executed.
     * @param activeSpan The span to be propagated.
     * @return The wrapped callable to execute with the active span.
     */
    public static <T> Callable<T> callableWithActiveSpan(Callable<T> callable, Span activeSpan) {
        return new CallableWithActiveSpan<T>(callable, activeSpan);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(runnableWithActiveSpan(command, ActiveSpanManager.activeSpan()));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(runnableWithActiveSpan(task, ActiveSpanManager.activeSpan()));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(runnableWithActiveSpan(task, ActiveSpanManager.activeSpan()), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(callableWithActiveSpan(task, ActiveSpanManager.activeSpan()));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasksWithActiveSpan(tasks, ActiveSpanManager.activeSpan()));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(tasksWithActiveSpan(tasks, ActiveSpanManager.activeSpan()), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasksWithActiveSpan(tasks, ActiveSpanManager.activeSpan()));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasksWithActiveSpan(tasks, ActiveSpanManager.activeSpan()), timeout, unit);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    private static <T> Collection<? extends Callable<T>> tasksWithActiveSpan(
            Collection<? extends Callable<T>> tasks, Span activeSpan) {
        if (tasks == null) throw new NullPointerException("Collection of tasks is <null>.");
        Collection<Callable<T>> result = new ArrayList<Callable<T>>(tasks.size());
        for (Callable<T> task : tasks) result.add(callableWithActiveSpan(task, activeSpan));
        return result;
    }

}
