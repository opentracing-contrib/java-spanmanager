/**
 * Copyright 2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spanmanager.tracer;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.contrib.spanmanager.SpanManager;

import java.util.Map;

/**
 * {@link SpanBuilder} that automatically {@link SpanManager#manage(Span) activates} newly started spans.
 * <p>
 * The activated ManagedSpan is wrapped in an {@linkplain AutoReleasingManagedSpan}
 * to automatically release when finished.<br>
 * All other methods are forwarded to the delegate span builder.
 *
 * @see SpanManager
 * @see AutoReleasingManagedSpan#finish()
 */
final class ManagedSpanBuilder implements SpanBuilder {

    SpanBuilder delegate;
    private final SpanManager spanManager;

    ManagedSpanBuilder(SpanBuilder delegate, SpanManager spanManager) {
        if (delegate == null) throw new NullPointerException("Delegate SpanBuilder was <null>.");
        if (spanManager == null) throw new NullPointerException("Span manager was <null>.");
        this.delegate = delegate;
        this.spanManager = spanManager;
    }

    /**
     * Replaces the {@link #delegate} SpanBuilder by a delegated-method result.
     *
     * @param spanBuilder The builder returned from the delegate (normally '== delegate').
     * @return This re-wrapped ManagedSpanBuilder.
     */
    SpanBuilder rewrap(SpanBuilder spanBuilder) {
        if (spanBuilder != null) {
            this.delegate = spanBuilder;
        }
        return this;
    }

    /**
     * Starts the built Span and {@link SpanManager#manage(Span) activates} it.
     *
     * @return a new 'current' Span that releases itself upon <em>finish</em> or <em>close</em> calls.
     * @see SpanManager#manage(Span)
     * @see AutoReleasingManagedSpan#release()
     */
    @Override
    public Span start() {
        return new AutoReleasingManagedSpan(spanManager.manage(delegate.start()));
    }

    // All other methods are forwarded to the delegate SpanBuilder.

    public SpanBuilder asChildOf(SpanContext parent) {
        return addReference(References.CHILD_OF, parent);
    }

    public SpanBuilder asChildOf(Span parent) {
        return addReference(References.CHILD_OF, parent.context());
    }

    public SpanBuilder addReference(String referenceType, SpanContext context) {
        if (context instanceof ManagedSpanBuilder) { // Weird that SpanBuilder extends Context!
            context = ((ManagedSpanBuilder) context).delegate;
        }
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
