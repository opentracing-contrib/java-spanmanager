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

import io.opentracing.NoopSpan;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default {@link SpanManager} implementation using {@link ThreadLocal} storage
 * maintaining a stack-like structure of linked managed spans.
 * <p>
 * The linked managed spans provide the following stack unwinding algorithm:
 * <ol>
 * <li>If the released span is not the <em>managed</em> span, the <em>current managed</em> span is left alone.</li>
 * <li>Otherwise, the first parent that is <em>not yet released</em> is set as the new managed span.</li>
 * <li>If no managed parents remain, the <em>managed span</em> is cleared.</li>
 * <li>Consecutive <code>release()</code> calls for already-released spans will be ignored.</li>
 * </ol>
 */
public final class DefaultSpanManager implements SpanManager {

    private static final Logger LOGGER = Logger.getLogger(DefaultSpanManager.class.getName());

    private final ManagedSpan NO_MANAGED_SPAN = new LinkedManagedSpan(null, null);

    private static final DefaultSpanManager INSTANCE = new DefaultSpanManager();
    private final ThreadLocal<LinkedManagedSpan> managed = new ThreadLocal<LinkedManagedSpan>();

    private DefaultSpanManager() {
    }

    /**
     * @return The singleton instance of the default span manager.
     */
    public static SpanManager getInstance() {
        return INSTANCE;
    }

    /**
     * Stack unwinding algorithm that refreshes the currently managed span.
     * <p>
     * See {@link DefaultSpanManager class javadoc} for a full description.
     *
     * @return The current non-released LinkedManagedSpan or <code>null</code> if none remained.
     */
    private LinkedManagedSpan refreshCurrent() {
        LinkedManagedSpan managedSpan = managed.get();
        LinkedManagedSpan current = managedSpan;
        while (current != null && current.released.get()) { // Unwind stack if necessary.
            current = current.parent;
        }
        if (current != managedSpan) { // refresh current if necessary.
            if (current == null) managed.remove();
            else managed.set(current);
        }
        return current;
    }

    @Override
    public Span currentSpan() {
        LinkedManagedSpan current = refreshCurrent();
        return current != null && current.span != null ? current.span : NoopSpan.INSTANCE;
    }

    @Override
    public SpanManager.ManagedSpan manage(Span span) {
        LinkedManagedSpan managedSpan = new LinkedManagedSpan(span, refreshCurrent());
        managed.set(managedSpan);
        return managedSpan;
    }

    @Override
    public ManagedSpan current() {
        LinkedManagedSpan current = refreshCurrent();
        return current != null && current.span != null ? current : NO_MANAGED_SPAN;
    }

    @Override
    public void clear() {
        managed.remove();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private final class LinkedManagedSpan implements SpanManager.ManagedSpan {
        private final LinkedManagedSpan parent;
        private final Span span;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private LinkedManagedSpan(Span span, LinkedManagedSpan parent) {
            this.parent = parent;
            this.span = span;
        }

        @Override
        public Span getSpan() {
            return span;
        }

        public void release() {
            if (released.compareAndSet(false, true)) {
                LinkedManagedSpan current = refreshCurrent(); // Trigger stack-unwinding algorithm.
                LOGGER.log(Level.FINER, "Released {0}, current span is {1}.", new Object[]{this, current});
            } else {
                LOGGER.log(Level.FINEST, "No action needed, {0} was already released.", this);
            }
        }

        @Override
        public void close() {
            release();
        }

        @Override
        public String toString() {
            return "LinkedManagedSpan{" + span + '}';
        }
    }

}
