package io.opentracing.contrib.spanmanager;

import io.opentracing.Span;

import java.io.Closeable;

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

    /**
     * To {@linkplain #release() release} a {@link SpanManager#manage(Span) managed span} with.
     * <p>
     * It must be possible to repeatedly call {@linkplain #release()} without side effects.
     *
     * @see SpanManager
     */
    interface ManagedSpan extends Closeable {

        /**
         * The span that became the managed span at some point.
         *
         * @return The contained span to be released.
         */
        Span getSpan();

        /**
         * Makes the {@link #getSpan() contained span} no longer the managed span.
         * <p>
         * Implementation notes:
         * <ol>
         * <li>It is encouraged to restore the managed span as it was before this span became managed
         * (providing stack-like behaviour).</li>
         * <li>It must be possible to repeatedly call <code>release</code> without side effects.</li>
         * </ol>
         */
        void release();

        /**
         * Alias for {@link #release()} to allow easy use from try-with-resources.
         */
        void close();

    }
}
