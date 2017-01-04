package io.opentracing.contrib.spanmanager.tracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.spanmanager.ActiveSpanManager;
import io.opentracing.contrib.spanmanager.ManagedSpan;
import io.opentracing.propagation.Format;

/**
 * Convenience {@link Tracer} that automates managing the {@linkplain ActiveSpanManager#currentSpan() active span}:
 * <ol>
 * <li>It is a wrapper that forwards all calls to another {@link Tracer} implementation.</li>
 * <li>{@link Span} instances created with this tracer are {@link ActiveSpanManager#manage(Span) managed} when started
 * (automatically becoming the {@link ActiveSpanManager#currentSpan() currently active span})
 * </li>
 * <li>and {@link ManagedSpan#release() released} when finished.</li>
 * </ol>
 */
public class ManagedSpanTracer implements Tracer {

    protected final Tracer delegate;

    public ManagedSpanTracer(Tracer delegate) {
        if (delegate == null) throw new NullPointerException("The delegate Tracer implementation is <null>.");
        this.delegate = delegate;
    }

    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        // Weird that Builder extends Context!
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
        return getClass().getSimpleName() + "{delegate=" + delegate + '}';
    }

}
