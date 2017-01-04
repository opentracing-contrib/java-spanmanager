package io.opentracing.contrib.spanmanager;

import io.opentracing.NoopSpan;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default ThreadLocal-based implementation of {@link SpanManager} that implements
 * the following stack unwinding algorithm upon deactivation:
 * <ol>
 * <li>If the deactivated <em>span</em> is not the managed span, the managed span is left alone.</li>
 * <li>Otherwise, the first parent that is <em>not yet deactivated</em> is set as the new managed span.</li>
 * <li>If no managed parents remain, the <em>managed span</em> {@link ThreadLocal} is cleared.</li>
 * <li>Consecutive <code>release()</code> calls for already-deactivated spans will be ignored.</li>
 * </ol>
 */
final class ThreadLocalSpanManager implements SpanManager {

    private static final Logger LOGGER = Logger.getLogger(ThreadLocalSpanManager.class.getName());
    private static final ThreadLocal<ManagedSpan> MANAGED = new ThreadLocal<ManagedSpan>();

    ThreadLocalSpanManager() {
    }

    @Override
    public Span currentSpan() {
        final ManagedSpan managedSpan = MANAGED.get();
        return managedSpan != null ? managedSpan.span : NoopSpan.INSTANCE;
    }

    @Override
    public io.opentracing.contrib.spanmanager.ManagedSpan manage(Span span) {
        ManagedSpan managedSpan = new ManagedSpan(span);
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
        private final ManagedSpan parent = MANAGED.get();
        private final Span span;
        private final AtomicBoolean deactivated = new AtomicBoolean(false);

        private ManagedSpan(final Span span) {
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
            if (deactivated.compareAndSet(false, true)) {
                ManagedSpan current = MANAGED.get();
                if (this == current) {
                    while (current != null && current.deactivated.get()) {
                        current = current.parent;
                    }
                    if (current == null) MANAGED.remove();
                    else MANAGED.set(current);
                    LOGGER.log(Level.FINER, "Deactivated {0} and restored managed span to {1}.", new Object[]{this, current});
                } else {
                    LOGGER.log(Level.FINE, "Deactivated {0} without affecting managed span {1}.", new Object[]{this, current});
                }
            } else {
                LOGGER.log(Level.FINEST, "No action needed, {0} was already deactivated.", this);
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
