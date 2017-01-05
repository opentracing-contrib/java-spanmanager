package io.opentracing.contrib.spanmanager;

import io.opentracing.NoopSpan;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default {@link SpanManager} implementation using {@link ThreadLocal} storage,
 * maintaining a stack-like structure of linked managed spans.
 * <p>
 * The linked managed spans provide the following stack unwinding algorithm:
 * <ol>
 * <li>If the released <em>span</em> is not the managed span, the managed span is left alone.</li>
 * <li>Otherwise, the first parent that is <em>not yet released</em> is set as the new managed span.</li>
 * <li>If no managed parents remain, the <em>managed span</em> is cleared.</li>
 * <li>Consecutive <code>release()</code> calls for already-released spans will be ignored.</li>
 * </ol>
 */
final class ThreadLocalSpanManager implements SpanManager {

    private static final Logger LOGGER = Logger.getLogger(ThreadLocalSpanManager.class.getName());
    private static final ThreadLocal<ManagedSpan> MANAGED = new ThreadLocal<ManagedSpan>();

    ThreadLocalSpanManager() {
    }

    @Override
    public Span currentSpan() {
        ManagedSpan managedSpan = MANAGED.get();
        return managedSpan != null ? managedSpan.span : NoopSpan.INSTANCE;
    }

    @Override
    public io.opentracing.contrib.spanmanager.ManagedSpan manage(Span span) {
        ManagedSpan managedSpan = new ManagedSpan(span, MANAGED.get());
        MANAGED.set(managedSpan);
        return managedSpan;
    }

    @Override
    public void clear() {
        MANAGED.remove();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static final class ManagedSpan implements io.opentracing.contrib.spanmanager.ManagedSpan {
        private final ManagedSpan parent;
        private final Span span;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private ManagedSpan(Span span, ManagedSpan parent) {
            this.parent = parent;
            this.span = span;
        }

        @Override
        public Span getSpan() {
            return span;
        }

        /**
         * Please see {@link ThreadLocalSpanManager outer class description} for the stack-unwinding documentation.
         */
        public void release() {
            if (released.compareAndSet(false, true)) {
                ManagedSpan current = MANAGED.get();
                if (this == current) {
                    while (current != null && current.released.get()) {
                        current = current.parent;
                    }
                    if (current == null) MANAGED.remove();
                    else MANAGED.set(current);
                    LOGGER.log(Level.FINER, "Deactivated {0} and restored managed span to {1}.", new Object[]{this, current});
                } else {
                    LOGGER.log(Level.FINE, "Deactivated {0} without affecting managed span {1}.", new Object[]{this, current});
                }
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
            return "ManagedSpan{" + span + '}';
        }
    }

}
