package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.ActiveSpanManager;
import io.opentracing.contrib.spanmanager.ManagedSpan;

import java.util.concurrent.Callable;

/**
 * {@link Callable} wrapper that will execute with the {@link ActiveSpanManager#currentSpan() currently active span}
 * from the scheduling thread.
 */
final class CallableWithActiveSpan<T> implements Callable<T> {

    private final Callable<T> delegate;
    private final Span activeSpanOfScheduler;

    CallableWithActiveSpan(Callable<T> delegate, Span activeSpanOfScheduler) {
        if (delegate == null) throw new NullPointerException("Callable delegate is <null>.");
        this.delegate = delegate;
        this.activeSpanOfScheduler = activeSpanOfScheduler;
    }

    /**
     * Performs the delegate call with the specified active span.
     *
     * @return The result from the original call.
     * @throws Exception if the original call threw an exception.
     */
    public T call() throws Exception {
        final ManagedSpan managedSpan = ActiveSpanManager.get().manage(activeSpanOfScheduler);
        try {
            return delegate.call();
        } finally { // TODO: simulate try-with-resources (preferably using Guava's Closer)
            managedSpan.release();
        }
    }

}
