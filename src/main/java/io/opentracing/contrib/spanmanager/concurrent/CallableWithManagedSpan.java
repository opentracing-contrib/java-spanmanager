package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.SpanManager;

import java.util.concurrent.Callable;

/**
 * {@link Callable} wrapper that will execute with a {@link SpanManager#manage(Span) managed span}
 * specified from the scheduling thread.
 *
 * @see SpanManager
 */
final class CallableWithManagedSpan<T> implements Callable<T> {

    private final Callable<T> delegate;
    private final SpanManager spanManager;
    private final Span spanToManage;

    CallableWithManagedSpan(Callable<T> callable, SpanManager spanManager, Span spanToManage) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        if (spanManager == null) throw new NullPointerException("Span manager is <null>.");
        this.delegate = callable;
        this.spanManager = spanManager;
        this.spanToManage = spanToManage;
    }

    /**
     * Performs the delegate call with the specified managed span.
     *
     * @return The result from the original call.
     * @throws Exception if the original call threw an exception.
     */
    public T call() throws Exception {
        final SpanManager.ManagedSpan managedSpan = spanManager.manage(spanToManage);
        try {
            return delegate.call();
        } finally {
            managedSpan.release();
        }
    }

}
