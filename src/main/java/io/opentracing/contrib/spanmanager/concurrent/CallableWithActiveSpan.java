package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.ActiveSpanManager;
import io.opentracing.contrib.spanmanager.ManagedSpan;

import java.util.concurrent.Callable;

/**
 * {@link Callable} wrapper that will execute with the {@link ActiveSpanManager#currentSpan() currently active span}
 * from the scheduling thread.
 */
public class CallableWithActiveSpan<T> implements Callable<T> {

    protected final Callable<T> delegate;
    private final Span activeSpanOfScheduler;

    protected CallableWithActiveSpan(Callable<T> delegate, Span activeSpanOfScheduler) {
        if (delegate == null) throw new NullPointerException("Callable delegate is <null>.");
        this.delegate = delegate;
        this.activeSpanOfScheduler = activeSpanOfScheduler;
    }

    /**
     * Creates a new callable that will execute with the {@link ActiveSpanManager#currentSpan() currently active span}
     * from the scheduling thread.
     *
     * @param delegate The delegate callable to execute (required, non-<code>null</code>).
     * @param <T>      The result type of the call.
     * @return The 'span aware' callable that will propagate the currently active span to the new thread.
     * @see ActiveSpanManager#currentSpan()
     */
    public static <T> CallableWithActiveSpan<T> of(Callable<T> delegate) {
        return new CallableWithActiveSpan<T>(delegate, ActiveSpanManager.get().currentSpan());
    }

    /**
     * This method allows the caller to override the active span for the new thread.
     * <p>
     * <em>Please note:</em> it is <strong>not necessary</strong> to call this method with the
     * {@link ActiveSpanManager#currentSpan() currently active span} since
     * that will be used {@link #of(Callable) by default}.
     *
     * @param activeSpan The span to become the active span when calling the delegate.
     * @return A new runnable object that will propagate the active span to the thread executing the call.
     * @see #of(Callable)
     */
    public CallableWithActiveSpan<T> withActiveSpan(Span activeSpan) {
        return new CallableWithActiveSpan<T>(delegate, activeSpan);
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
