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
package io.opentracing.contrib.spanmanager;

import io.opentracing.Span;

import java.io.Closeable;

/**
 * Defines <em>{@linkplain #currentSpan() current span}</em> management.
 * <p>
 * A SpanManager separates the creation of a {@linkplain Span} from its use later on.
 * This relieves application developers from passing the current span around through their code.
 * Only tracing-related code will need access to a SpanManager reference,
 * which can be provided as an ordinary dependency.
 */
public interface SpanManager {

    /**
     * Makes span the <em>current span</em> within the running process.
     *
     * @param span The span to become the current span.
     * @return A managed object containing the current span and a way to deactivate it again.
     * @see SpanManager#current()
     * @see ManagedSpan#deactivate()
     */
    ManagedSpan activate(Span span);

    /**
     * Retrieve the current {@link ManagedSpan managed span}.
     * <p>
     * If there is no current managed span, an 'empty' managed span instance is returned
     * for which {@link ManagedSpan#deactivate() deactivate()} has no effects
     * and whose {@link ManagedSpan#getSpan() getSpan()} method always returns {@code null}.
     *
     * @return The current managed span
     * @see SpanManager#activate(Span)
     */
    ManagedSpan current();

    /**
     * Unconditional cleanup of all managed spans including any parents.
     * <p>
     * This allows boundary filters to deactivate all current spans
     * before relinquishing control over their process,
     * which may end up repurposed by a threadpool.
     *
     * @see ManagedSpan#deactivate()
     */
    void clear();

    /**
     * Return the currently-managed {@link Span}.
     * <p>
     * Please note that this method is due to be removed in the next release.
     * The currently-managed span can also be obtained through
     * {@link #current()}.{@link ManagedSpan#getSpan() getSpan()}
     * which is safe, as curren
     *
     * @return The current Span, or the <code>NoopSpan</code> if there is no managed span.
     * @see SpanManager#activate(Span)
     * @see SpanManager#current()
     * @deprecated Please use <code>SpanManager.current().getSpan()</code> instead
     */
    Span currentSpan();

    /**
     * To {@linkplain #deactivate() deactivate} a {@link SpanManager#activate(Span) managed active span} with.
     * <p>
     * It must be possible to repeatedly call {@linkplain #deactivate()} without side effects.
     *
     * @see SpanManager
     */
    interface ManagedSpan extends Closeable {

        /**
         * The span that became the managed span at some point.
         *
         * @return The contained span that was activated, or <code>null</code> if no span is being managed.
         */
        Span getSpan();

        /**
         * Makes the {@link #getSpan() contained span} no longer the managed span.
         * <p>
         * Implementation notes:
         * <ol>
         * <li>It is encouraged to restore the managed span as it was before this span was activated
         * (providing stack-like behaviour).</li>
         * <li>It must be possible to repeatedly call <code>deactivate</code> without side effects.</li>
         * </ol>
         */
        void deactivate();

        /**
         * Alias for {@link #deactivate()} to allow easy use from try-with-resources.
         */
        void close();

    }
}
