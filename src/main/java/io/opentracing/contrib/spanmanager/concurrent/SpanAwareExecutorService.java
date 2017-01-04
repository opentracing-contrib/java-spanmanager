package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.ActiveSpanManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * {@link ExecutorService} wrapper that propagates the {@link ActiveSpanManager#currentSpan() currently active span}
 * into the calls that are scheduled.
 */
public class SpanAwareExecutorService implements ExecutorService {
    protected final ExecutorService delegate;

    protected SpanAwareExecutorService(ExecutorService delegate) {
        if (delegate == null) throw new NullPointerException("Delegate executor service is <null>.");
        this.delegate = delegate;
    }

    public static SpanAwareExecutorService wrap(final ExecutorService delegate) {
        return delegate instanceof SpanAwareExecutorService ? (SpanAwareExecutorService) delegate
                : new SpanAwareExecutorService(delegate);
    }

    public void execute(Runnable command) {
        delegate.execute(spanAware(command));
    }

    public Future<?> submit(Runnable task) {
        return delegate.submit(spanAware(task));
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(spanAware(task), result);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(spanAware(task));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(spanAware(tasks));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(spanAware(tasks), timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(spanAware(tasks));
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(spanAware(tasks), timeout, unit);
    }

    public void shutdown() {
        delegate.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    private static Runnable spanAware(Runnable runnable) {
        return new RunnableWithActiveSpan(runnable, ActiveSpanManager.get().currentSpan());
    }

    private static <T> Callable<T> spanAware(Callable<T> callable) {
        return new CallableWithActiveSpan<T>(callable, ActiveSpanManager.get().currentSpan());
    }

    private static <T> Collection<? extends Callable<T>> spanAware(final Collection<? extends Callable<T>> tasks) {
        if (tasks == null) throw new NullPointerException("Collection of scheduled tasks is <null>.");
        Span activeSpan = ActiveSpanManager.get().currentSpan();
        Collection<Callable<T>> result = new ArrayList<Callable<T>>(tasks.size());
        for (Callable<T> task : tasks) {
            result.add(new CallableWithActiveSpan<T>(task, activeSpan));
        }
        return result;
    }

}
