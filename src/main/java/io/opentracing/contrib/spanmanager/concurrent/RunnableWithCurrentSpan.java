package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.GlobalSpanManager;
import io.opentracing.contrib.spanmanager.SpanManager;

/**
 * {@link Runnable} wrapper that will execute with a {@link GlobalSpanManager#currentSpan() custom current span}
 * specified from the scheduling thread.
 *
 * @see GlobalSpanManager
 */
final class RunnableWithCurrentSpan implements Runnable {

    private final Runnable delegate;
    private final Span customCurrentSpan;

    RunnableWithCurrentSpan(Runnable delegate, Span customCurrentSpan) {
        if (delegate == null) throw new NullPointerException("Runnable delegate is <null>.");
        this.delegate = delegate;
        this.customCurrentSpan = customCurrentSpan;
    }

    /**
     * Performs the runnable action with the specified custom current span.
     */
    public void run() {
        SpanManager.ManagedSpan managedSpan = GlobalSpanManager.get().manage(customCurrentSpan);
        try {
            delegate.run();
        } finally {
            managedSpan.release();
        }
    }

}
