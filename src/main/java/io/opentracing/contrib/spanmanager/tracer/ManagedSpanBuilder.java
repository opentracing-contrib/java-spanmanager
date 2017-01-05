package io.opentracing.contrib.spanmanager.tracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.contrib.spanmanager.ActiveSpanManager;
import io.opentracing.contrib.spanmanager.ManagedSpan;

import java.util.Map;

/**
 * {@link SpanBuilder} that automatically {@link ActiveSpanManager#activate(Span) activates} newly started spans.
 * <p>
 * The activated ManagedSpan is wrapped in an {@linkplain AutoReleasingManagedSpan}
 * to automatically release when finished.<br>
 * All other methods are forwarded to the delegate span builder.
 *
 * @see ActiveSpanManager
 * @see AutoReleasingManagedSpan#finish()
 */
final class ManagedSpanBuilder implements SpanBuilder {

    protected SpanBuilder delegate;

    ManagedSpanBuilder(SpanBuilder delegate) {
        if (delegate == null) throw new NullPointerException("Delegate SpanBuilder was <null>.");
        this.delegate = delegate;
    }

    /**
     * Replaces the {@link #delegate} SpanBuilder by a delegated-method result.
     *
     * @param spanBuilder The builder returned from the delegate (normally '== delegate').
     * @return This re-wrapped ActiveSpanBuilder.
     */
    SpanBuilder rewrap(SpanBuilder spanBuilder) {
        if (spanBuilder != null) this.delegate = spanBuilder;
        return this;
    }

    /**
     * Starts the built Span and {@link ActiveSpanManager#activate(Span) activates} it.
     *
     * @return a new 'currently active' Span that deactivates itself upon <em>finish</em> or <em>close</em> calls.
     * @see ActiveSpanManager#activate(Span)
     * @see AutoReleasingManagedSpan#release()
     */
    @Override
    public Span start() {
        return new AutoReleasingManagedSpan(ActiveSpanManager.activate(delegate.start()));
    }

    // All other methods are forwarded to the delegate SpanBuilder.

    public SpanBuilder asChildOf(SpanContext parent) {
        if (parent instanceof ManagedSpanBuilder) parent = ((ManagedSpanBuilder) parent).delegate;
        return rewrap(delegate.asChildOf(parent));
    }

    public SpanBuilder asChildOf(Span parent) {
        if (parent instanceof ManagedSpan) parent = ((ManagedSpan) parent).getSpan();
        return rewrap(delegate.asChildOf(parent));
    }

    public SpanBuilder addReference(String referenceType, SpanContext context) {
        if (context instanceof ManagedSpanBuilder) context = ((ManagedSpanBuilder) context).delegate;
        return rewrap(delegate.addReference(referenceType, context));
    }

    public SpanBuilder withTag(String key, String value) {
        return rewrap(delegate.withTag(key, value));
    }

    public SpanBuilder withTag(String key, boolean value) {
        return rewrap(delegate.withTag(key, value));
    }

    public SpanBuilder withTag(String key, Number value) {
        return rewrap(delegate.withTag(key, value));
    }

    public SpanBuilder withStartTimestamp(long microseconds) {
        return rewrap(delegate.withStartTimestamp(microseconds));
    }

    public Iterable<Map.Entry<String, String>> baggageItems() {
        return delegate.baggageItems();
    }

}
