package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.ActiveSpanManager;
import io.opentracing.contrib.spanmanager.ManagedSpan;

/**
 * {@link Runnable} wrapper that will execute with a custom active span specified from the scheduling thread.
 *
 * @see ActiveSpanManager
 */
final class RunnableWithActiveSpan implements Runnable {

    private final Runnable delegate;
    private final Span activeSpanOfScheduler;

    RunnableWithActiveSpan(Runnable delegate, Span parentSpan) {
        if (delegate == null) throw new NullPointerException("Runnable delegate is <null>.");
        this.delegate = delegate;
        this.activeSpanOfScheduler = parentSpan;
    }

    /**
     * Performs the runnable action with the specified parent span.
     */
    public void run() {
        ManagedSpan managedSpan = ActiveSpanManager.activate(activeSpanOfScheduler);
        try {
            delegate.run();
        } finally { // TODO: simulate try-with-resources (preferably using Guava's Closer)
            managedSpan.release();
        }
    }

}
