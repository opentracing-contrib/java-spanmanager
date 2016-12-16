package io.opentracing.contrib.activespan.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.activespan.ActiveSpanManager;
import io.opentracing.contrib.activespan.ActiveSpanManager.SpanDeactivator;

import java.util.concurrent.Callable;

/**
 * {@link Callable} wrapper that will execute with the {@link ActiveSpanManager#activeSpan() active span}
 * from the scheduling thread.
 *
 * @author Sjoerd Talsma
 */
public class SpanAwareCallable<T> implements Callable<T> {

    protected final Callable<T> delegate;
    private final Span activeSpanOfScheduler;

    protected SpanAwareCallable(Callable<T> delegate, Span activeSpanOfScheduler) {
        if (delegate == null) throw new NullPointerException("Callable delegate is <null>.");
        this.delegate = delegate;
        this.activeSpanOfScheduler = activeSpanOfScheduler;
    }

    /**
     * Creates a new callable that will execute with the {@link ActiveSpanManager#activeSpan() active span}
     * from the scheduling thread.
     *
     * @param delegate The delegate callable to execute (required, non-<code>null</code>).
     * @param <T>      The result type of the call.
     * @return The 'span aware' callable that will propagate the currently active span to the new thread.
     * @see ActiveSpanManager#activeSpan()
     */
    public static <T> SpanAwareCallable<T> of(Callable<T> delegate) {
        return new SpanAwareCallable<T>(delegate, ActiveSpanManager.activeSpan());
    }

    /**
     * This method allows the caller to override the active span in the new thread.
     * <p>
     * <em>Please note:</em> it is <strong>not</strong> necessary to call this method with the
     * {@link ActiveSpanManager#activeSpan() current active span} as that will be used {@link #of(Callable) by default}.
     *
     * @param parentSpan The span to use as active parent in the new thread.
     * @return A new runnable object that will propagate the parent span to another thread.
     * @see #of(Callable)
     */
    public SpanAwareCallable<T> withActiveSpan(Span parentSpan) {
        return new SpanAwareCallable<T>(delegate, parentSpan);
    }

    /**
     * Performs the delegate call with the specified parent span.
     *
     * @return The result from the original call.
     * @throws Exception if the original call threw an exception.
     */
    public T call() throws Exception {
        final SpanDeactivator parentContext = ActiveSpanManager.activate(activeSpanOfScheduler);
        try {
            return delegate.call();
        } finally {
            ActiveSpanManager.deactivate(parentContext);
        }
    }

}
