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
 * Only tracing-related code will need access to a SpanManager reference, provided as an ordinary dependency.
 */
public interface SpanManager {

    /**
     * Return the currently-managed {@link Span}.
     *
     * @return The current Span, or the <code>NoopSpan</code> if there is no managed span.
     * @see SpanManager#manage(Span)
     * @deprecated Use SpanManager#current() instead
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
     * Return the current {@link ManagedSpan}.
     *
     * @return The current ManagedSpan or the managed <code>NoopSpan</code> if there is no managed span.
     * @see SpanManager#manage(Span)
     */
    ManagedSpan current();

    /**
     * Unconditional cleanup of all managed spans including any parents.
     * <p>
     * This allows boundary filters to release all current spans
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
