package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.GlobalSpanManager;
import io.opentracing.contrib.spanmanager.SpanManager;

import java.util.concurrent.Callable;

/**
 * {@link Callable} wrapper that will execute with a {@link GlobalSpanManager#currentSpan() custom current span}
 * specified from the scheduling thread.
 *
 * @see GlobalSpanManager
 */
final class CallableWithCurrentSpan<T> implements Callable<T> {

    private final Callable<T> delegate;
    private final Span customCurrentSpan;

    CallableWithCurrentSpan(Callable<T> delegate, Span customCurrentSpan) {
        if (delegate == null) throw new NullPointerException("Callable delegate is <null>.");
        this.delegate = delegate;
        this.customCurrentSpan = customCurrentSpan;
    }

    /**
     * Performs the delegate call with the specified custom current span.
     *
     * @return The result from the original call.
     * @throws Exception if the original call threw an exception.
     */
    public T call() throws Exception {
        final SpanManager.ManagedSpan managedSpan = GlobalSpanManager.get().manage(customCurrentSpan);
        try {
            return delegate.call();
        } finally {
            managedSpan.release();
        }
    }

}
