package io.opentracing.contrib.spanmanager;

import io.opentracing.Span;

import java.io.Closeable;

/**
 * To {@linkplain #release() release} a {@link SpanManager#manage(Span) managed span} with.
 * <p>
 * It must be possible to repeatedly call {@linkplain #release()} without side effects.
 *
 * @see SpanManager
 */
public interface ManagedSpan extends Closeable {

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
