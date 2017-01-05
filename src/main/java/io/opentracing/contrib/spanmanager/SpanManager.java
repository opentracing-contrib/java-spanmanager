package io.opentracing.contrib.spanmanager;

import io.opentracing.Span;

/**
 * Manager to {@link #manage(Span) manage} and {@link ManagedSpan#release() release}
 * {@linkplain Span spans} and accessing the {@link #currentSpan() currently-managed span}.
 */
public interface SpanManager {

    /**
     * Return the currently-managed {@link Span}.
     *
     * @return The current Span, or the <code>NoopSpan</code> if there is no managed span.
     * @see SpanManager#manage(Span)
     */
    Span currentSpan();

    /**
     * Makes span the <em>current span</em> within the running process.
     *
     * @param span The span to become the current span.
     * @return A managed object to release the current span with.
     * @see SpanManager#currentSpan()
     * @see ManagedSpan#release()
     */
    ManagedSpan manage(Span span);

    /**
     * Unconditional cleanup of all managed spans including any parents.
     * <p>
     * This allows boundary filters to release all active spans
     * before relinquishing control over their process,
     * which may end up repurposed by a threadpool.
     *
     * @see ManagedSpan#release()
     */
    void clear();

}
