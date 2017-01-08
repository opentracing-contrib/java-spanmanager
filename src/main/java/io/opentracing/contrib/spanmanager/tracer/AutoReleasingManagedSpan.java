package io.opentracing.contrib.spanmanager.tracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.contrib.spanmanager.SpanManager;

import java.util.Map;

/**
 * A {@link Span} that automatically {@link #release() releases}
 * when {@link #finish() finished} or {@link #close() closed}.
 * <p>
 * All other methods are forwarded to the actual managed Span.
 */
final class AutoReleasingManagedSpan implements Span, SpanManager.ManagedSpan {

    private final SpanManager.ManagedSpan managedSpan;

    AutoReleasingManagedSpan(SpanManager.ManagedSpan managedSpan) {
        if (managedSpan == null) throw new NullPointerException("Managed span was <null>.");
        this.managedSpan = managedSpan;
    }

    @Override
    public Span getSpan() {
        return managedSpan.getSpan();
    }

    /**
     * Releases this currently active ManagedSpan.
     */
    @Override
    public void release() {
        managedSpan.release();
    }

    /**
     * {@link Span#finish() Finishes} the delegate and {@link SpanManager.ManagedSpan#release() releases} this ManagedSpan.
     */
    @Override
    public void finish() {
        try {
            getSpan().finish();
        } finally {
            release();
        }
    }

    /**
     * {@link Span#finish(long) Finishes} the delegate and {@link SpanManager.ManagedSpan#release() releases} this ManagedSpan.
     */
    @Override
    public void finish(long finishMicros) {
        try {
            getSpan().finish(finishMicros);
        } finally {
            release();
        }
    }

    /**
     * {@link Span#close() Closes} the delegate and {@link SpanManager.ManagedSpan#release() releases} this ManagedSpan.
     */
    @Override
    public void close() {
        try {
            getSpan().close();
        } finally {
            release();
        }
    }

    // Default behaviour is forwarded to the actual managed Span:

    @Override
    public SpanContext context() {
        return getSpan().context();
    }

    @Override
    public Span setTag(String key, String value) {
        getSpan().setTag(key, value);
        return this;
    }

    @Override
    public Span setTag(String key, boolean value) {
        getSpan().setTag(key, value);
        return this;
    }

    @Override
    public Span setTag(String key, Number value) {
        getSpan().setTag(key, value);
        return this;
    }

    @Override
    public Span log(Map<String, ?> fields) {
        getSpan().log(fields);
        return this;
    }

    @Override
    public Span log(long timestampMicroseconds, Map<String, ?> fields) {
        getSpan().log(timestampMicroseconds, fields);
        return this;
    }

    @Override
    public Span log(String event) {
        getSpan().log(event);
        return this;
    }

    @Override
    public Span log(long timestampMicroseconds, String event) {
        getSpan().log(timestampMicroseconds, event);
        return this;
    }

    @Override
    public Span setBaggageItem(String key, String value) {
        getSpan().setBaggageItem(key, value);
        return this;
    }

    @Override
    public String getBaggageItem(String key) {
        return getSpan().getBaggageItem(key);
    }

    @Override
    public Span setOperationName(String operationName) {
        getSpan().setOperationName(operationName);
        return this;
    }

    @SuppressWarnings("deprecation") // We simply delegate this method as we're told.
    @Override
    public Span log(String eventName, Object payload) {
        getSpan().log(eventName, payload);
        return this;
    }

    @SuppressWarnings("deprecation") // We simply delegate this method as we're told.
    @Override
    public Span log(long timestampMicroseconds, String eventName, Object payload) {
        getSpan().log(timestampMicroseconds, eventName, payload);
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + managedSpan + '}';
    }

}
