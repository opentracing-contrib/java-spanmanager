package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.SpanManager;

/**
 * {@link Runnable} wrapper that will execute with a {@link SpanManager#manage(Span) managed span}
 * specified from the scheduling thread.
 *
 * @see SpanManager
 */
final class RunnableWithManagedSpan implements Runnable {

    private final Runnable delegate;
    private final SpanManager spanManager;
    private final Span spanToManage;

    RunnableWithManagedSpan(Runnable runnable, SpanManager spanManager, Span spanToManage) {
        if (runnable == null) throw new NullPointerException("Runnable is <null>.");
        if (spanManager == null) throw new NullPointerException("Span manager is <null>.");
        this.delegate = runnable;
        this.spanManager = spanManager;
        this.spanToManage = spanToManage;
    }

    /**
     * Performs the runnable action with the specified managed span.
     */
    public void run() {
        SpanManager.ManagedSpan managedSpan = spanManager.manage(spanToManage);
        try {
            delegate.run();
        } finally {
            managedSpan.release();
        }
    }

}
