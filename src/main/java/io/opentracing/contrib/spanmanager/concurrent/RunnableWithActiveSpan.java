package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.ActiveSpanManager;
import io.opentracing.contrib.spanmanager.ManagedSpan;

/**
 * {@link Runnable} wrapper that will execute with the {@link ActiveSpanManager#currentSpan() currently active span}
 * from the scheduling thread.
 */
public class RunnableWithActiveSpan implements Runnable {

    protected final Runnable delegate;
    private final Span activeSpanOfScheduler;

    protected RunnableWithActiveSpan(Runnable delegate, Span parentSpan) {
        if (delegate == null) throw new NullPointerException("Runnable delegate is <null>.");
        this.delegate = delegate;
        this.activeSpanOfScheduler = parentSpan;
    }

    /**
     * Creates a new runnable that will execute with the {@link ActiveSpanManager#currentSpan() currently active span}
     * from the scheduling thread.
     *
     * @param delegate The delegate runnable to execute (required, non-<code>null</code>).
     * @return The wrapped runnable that propagates the currently active span to another thread.
     * @see ActiveSpanManager#currentSpan()
     */
    public static RunnableWithActiveSpan of(Runnable delegate) {
        return new RunnableWithActiveSpan(delegate, ActiveSpanManager.get().currentSpan());
    }

    /**
     * This method allows the caller to override the active span in the new thread.
     * <p>
     * <em>Please note:</em> it is <strong>not necessary</strong> to call this method with the
     * {@link ActiveSpanManager#currentSpan() currently active span} since
     * that will be used {@link #of(Runnable) by default}.
     *
     * @param activeSpan The span to become the active span when running the delegate.
     * @return A new runnable object that will propagate the given span to the executing thread.
     * @see #of(Runnable)
     */
    public RunnableWithActiveSpan withParent(Span activeSpan) {
        return new RunnableWithActiveSpan(delegate, activeSpan);
    }

    /**
     * Performs the runnable action with the specified parent span.
     */
    public void run() {
        ManagedSpan managedSpan = ActiveSpanManager.get().manage(activeSpanOfScheduler);
        try {
            delegate.run();
        } finally { // TODO: simulate try-with-resources (preferably using Guava's Closer)
            managedSpan.release();
        }
    }

}
