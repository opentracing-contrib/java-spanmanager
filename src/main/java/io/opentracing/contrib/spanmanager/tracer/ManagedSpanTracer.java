package io.opentracing.contrib.spanmanager.tracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.spanmanager.GlobalSpanManager;
import io.opentracing.contrib.spanmanager.SpanManager;
import io.opentracing.propagation.Format;

/**
 * Convenience {@link Tracer} that automates managing the {@linkplain GlobalSpanManager#currentSpan() currently active span}:
 * <ol>
 * <li>It is a wrapper that forwards all calls to another {@link Tracer} implementation.</li>
 * <li>{@linkplain Span Spans} created with this tracer are
 * automatically {@link GlobalSpanManager#manage(Span) managed} when started,</li>
 * <li>and automatically {@link SpanManager.ManagedSpan#release() released} when they finish.</li>
 * </ol>
 * <p>
 * Implementation note: This {@link Tracer} wraps the {@linkplain SpanBuilder} and {@linkplain Span}
 * in {@linkplain ManagedSpanBuilder} and {@linkplain AutoReleasingManagedSpan} respectively
 * <em>because no {@link Span} lifecycle callbacks are available in the opentracing API</em>.<br>
 * If there were, the {@linkplain ManagedSpanTracer} could be a lot simpler.<br>
 * However, lifecycle callbacks in the API form a considerable performance risk.
 *
 * @see GlobalSpanManager
 */
public final class ManagedSpanTracer implements Tracer {

    private final Tracer delegate;

    public ManagedSpanTracer(Tracer delegate) {
        if (delegate == null) throw new NullPointerException("The delegate Tracer implementation is <null>.");
        this.delegate = delegate;
    }

    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        // Weird that SpanBuilder extends Context!
        if (spanContext instanceof ManagedSpanBuilder) spanContext = ((ManagedSpanBuilder) spanContext).delegate;
        delegate.inject(spanContext, format, carrier);
    }

    public <C> SpanContext extract(Format<C> format, C carrier) {
        return delegate.extract(format, carrier);
    }

    public SpanBuilder buildSpan(String operationName) {
        return new ManagedSpanBuilder(delegate.buildSpan(operationName));
    }

    @Override
    public String toString() {
        return "ManagedSpanTracer{" + delegate + '}';
    }

}
