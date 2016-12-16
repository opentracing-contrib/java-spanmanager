package io.opentracing.contrib.activespan.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.activespan.ActiveSpanManager;
import io.opentracing.contrib.activespan.ActiveSpanManager.SpanDeactivator;

/**
 * {@link Runnable} wrapper that will execute with the {@link ActiveSpanManager#activeSpan() active span}
 * from the scheduling thread.
 *
 * @author Sjoerd Talsma
 */
public class SpanAwareRunnable implements Runnable {

    protected final Runnable delegate;
    private final Span parentSpan;

    protected SpanAwareRunnable(Runnable delegate, Span parentSpan) {
        if (delegate == null) throw new NullPointerException("Runnable delegate is <null>.");
        this.delegate = delegate;
        this.parentSpan = parentSpan;
    }

    /**
     * Creates a new runnable that will execute with the {@link ActiveSpanManager#activeSpan() active span}
     * from the scheduling thread.
     *
     * @param delegate The delegate runnable to execute (required, non-<code>null</code>).
     * @return The wrapped runnable that propagates the active span to another thread.
     * @see ActiveSpanManager#activeSpan()
     */
    public static SpanAwareRunnable of(Runnable delegate) {
        return new SpanAwareRunnable(delegate, ActiveSpanManager.activeSpan());
    }

    /**
     * This method allows the caller to override the active span in the new thread.
     * <p>
     * <em>Please note:</em> it is <strong>not</strong> necessary to call this method with the
     * {@link ActiveSpanManager#activeSpan() current active span} as that will be used {@link #of(Runnable) by default}.
     *
     * @param parentSpan The span to use as active parent in the new thread.
     * @return A new runnable object that will propagate the parent span to another thread.
     * @see #of(Runnable)
     */
    public SpanAwareRunnable withParent(Span parentSpan) {
        return new SpanAwareRunnable(delegate, parentSpan);
    }

    /**
     * Performs the runnable action with the specified parent span.
     */
    public void run() {
        SpanDeactivator deactivator = ActiveSpanManager.activate(parentSpan);
        try {
            delegate.run();
        } finally {
            ActiveSpanManager.deactivate(deactivator);
        }
    }

}
