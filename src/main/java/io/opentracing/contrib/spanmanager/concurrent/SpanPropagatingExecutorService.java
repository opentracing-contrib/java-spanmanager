/**
 * Copyright 2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.SpanManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Propagates the {@link SpanManager#currentSpan() current span} from the caller
 * into each call that is executed.
 * <p>
 * <em>Note:</em> The current span is merely propagated.
 * It is explicitly <b>not</b> finished when the calls end,
 * nor will new spans be automatically related to the propagated span.
 */
public class SpanPropagatingExecutorService implements ExecutorService {
    private final ExecutorService delegate;
    private final SpanManager spanManager;

    /**
     * Wraps the delegate ExecutorService to propagate the {@link SpanManager#currentSpan() current span}
     * of callers into the executed calls, using the specified {@link SpanManager}.
     *
     * @param delegate    The executorservice to forward calls to.
     * @param spanManager The manager to propagate spans with.
     */
    public SpanPropagatingExecutorService(ExecutorService delegate, SpanManager spanManager) {
        if (delegate == null) throw new NullPointerException("Delegate executor service is <null>.");
        if (spanManager == null) throw new NullPointerException("SpanManager is <null>.");
        this.delegate = delegate;
        this.spanManager = spanManager;
    }

    /**
     * Propagates the {@link SpanManager#currentSpan() custom current span} into the runnable
     * and performs cleanup afterwards.
     * <p>
     * <em>Note:</em> The <code>customCurrentSpan</code> is merely propagated.
     * The specified span is explicitly <b>not</b> finished by the runnable.
     *
     * @param runnable          The runnable to be executed.
     * @param customCurrentSpan The span to be propagated.
     * @return The wrapped runnable to execute with the custom span as current span.
     */
    private Runnable runnableWithCurrentSpan(Runnable runnable, Span customCurrentSpan) {
        return new RunnableWithManagedSpan(runnable, spanManager, customCurrentSpan);
    }

    /**
     * Propagates the {@link SpanManager#currentSpan() custom current span} into the callable
     * and performs cleanup afterwards.
     * <p>
     * <em>Note:</em> The <code>customCurrentSpan</code> is merely propagated.
     * The specified span is explicitly <b>not</b> finished by the callable.
     *
     * @param <T>               The callable result type.
     * @param callable          The callable to be executed.
     * @param customCurrentSpan The span to be propagated.
     * @return The wrapped callable to execute with the custom span as current span.
     */
    private <T> Callable<T> callableWithCurrentSpan(Callable<T> callable, Span customCurrentSpan) {
        return new CallableWithManagedSpan<T>(callable, spanManager, customCurrentSpan);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(runnableWithCurrentSpan(command, spanManager.currentSpan()));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(runnableWithCurrentSpan(task, spanManager.currentSpan()));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(runnableWithCurrentSpan(task, spanManager.currentSpan()), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(callableWithCurrentSpan(task, spanManager.currentSpan()));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasksWithCurrentSpan(tasks, spanManager.currentSpan()));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(tasksWithCurrentSpan(tasks, spanManager.currentSpan()), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasksWithCurrentSpan(tasks, spanManager.currentSpan()));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasksWithCurrentSpan(tasks, spanManager.currentSpan()), timeout, unit);
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

    private <T> Collection<? extends Callable<T>> tasksWithCurrentSpan(
            Collection<? extends Callable<T>> tasks, Span customCurrentSpan) {
        if (tasks == null) throw new NullPointerException("Collection of tasks is <null>.");
        Collection<Callable<T>> result = new ArrayList<Callable<T>>(tasks.size());
        for (Callable<T> task : tasks) result.add(callableWithCurrentSpan(task, customCurrentSpan));
        return result;
    }

}
