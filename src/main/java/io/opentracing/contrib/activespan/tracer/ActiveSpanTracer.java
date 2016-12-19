package io.opentracing.contrib.activespan.tracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.activespan.ActiveSpanManager;
import io.opentracing.propagation.Format;

/**
 * Wrapper that forwards all calls to another {@link Tracer} implementation.<br>
 * {@link io.opentracing.Span Spans} created with this tracer are
 * {@link ActiveSpanManager#activate(Span) activated} when started and
 * {@link ActiveSpanManager#deactivate(ActiveSpanManager.SpanDeactivator) deactivated} when finished.
 * <p>
 * The {@link SpanBuilder} of this Tracer will short-circuit to the
 * {@link io.opentracing.NoopSpanBuilder NoopSpanBuilder}.
 * This means {@link io.opentracing.NoopSpan NoopSpan}
 * instances will <strong>not</strong> be activated or deactivated through this tracer.
 *
 * @author Sjoerd Talsma
 */
public class ActiveSpanTracer implements Tracer {

    protected final Tracer delegate;

    public ActiveSpanTracer(Tracer delegate) {
        if (delegate == null) throw new NullPointerException("The delegate Tracer implementation is <null>.");
        this.delegate = delegate;
    }

    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        if (spanContext instanceof ActiveSpanBuilder) { // Weird that Builder extends Context!
            spanContext = ((ActiveSpanBuilder) spanContext).delegate;
        }
        delegate.inject(spanContext, format, carrier);
    }

    public <C> SpanContext extract(Format<C> format, C carrier) {
        return delegate.extract(format, carrier);
    }

    public SpanBuilder buildSpan(String operationName) {
        return ActiveSpanBuilder.of(delegate.buildSpan(operationName));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{delegate=" + delegate + '}';
    }

}
