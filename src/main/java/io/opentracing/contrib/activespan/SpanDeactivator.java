package io.opentracing.contrib.activespan;

import io.opentracing.Span;

import java.io.Closeable;

/**
 * To {@linkplain #deactivate() deactivate} a previously-activated {@link #getSpan() span} with.
 */
public interface SpanDeactivator extends Closeable {

    /**
     * The span that is the active span or has been at some point.
     *
     * @return The contained span to be deactivated.
     */
    Span getSpan();

    /**
     * Makes the {@link #getSpan() contained span} no longer the active span.
     * <p>
     * Implementation notes:
     * <ol>
     * <li>It is encouraged to restore the active span as it was before the contained span was activated
     * (providing stack-like behaviour).</li>
     * <li>It must be possible to repeatedly call <code>deactivate</code> without side effects.</li>
     * </ol>
     */
    void deactivate();

    /**
     * Alias for {@link #deactivate()} to allow easy deactivation from try-with-resources.
     */
    void close();

}
