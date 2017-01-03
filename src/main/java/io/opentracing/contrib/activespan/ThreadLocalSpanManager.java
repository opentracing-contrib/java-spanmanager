package io.opentracing.contrib.activespan;

import io.opentracing.NoopSpan;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default ThreadLocal-based implementation of the {@link ActiveSpanManager} class that implements the following
 * stack unwinding algorithm upon deactivation:
 * <ol>
 * <li>If the deactivated <em>managed span</em> is not the ACTIVE span, the ACTIVE span is left alone.</li>
 * <li>Otherwise, the first parent that is <em>not yet deactivated</em> is set as the new ACTIVE span.</li>
 * <li>If no ACTIVE parents remain, the <em>ACTIVE span</em> {@link ThreadLocal} is cleared.</li>
 * <li>Consecutive <code>deactivate()</code> calls for already deactivated spans will be ignored.</li>
 * </ol>
 */
final class ThreadLocalSpanManager extends ActiveSpanManager {

    private static final Logger LOGGER = Logger.getLogger(ActiveSpanManager.class.getName());
    private static final ThreadLocal<ManagedSpan> ACTIVE = new ThreadLocal<ManagedSpan>();

    ThreadLocalSpanManager() {
    }

    @Override
    public Span activeSpan() {
        final ManagedSpan activeSpan = ACTIVE.get();
        return activeSpan != null ? activeSpan.span : NoopSpan.INSTANCE;
    }

    @Override
    public SpanDeactivator activate(Span span) {
        ManagedSpan managedSpan = new ManagedSpan(span);
        ACTIVE.set(managedSpan);
        return managedSpan;
    }

    @Override
    public void clear() {
        ACTIVE.remove();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static final class ManagedSpan implements SpanDeactivator {
        private final ManagedSpan parent = ACTIVE.get();
        private final Span span;
        private final AtomicBoolean deactivated = new AtomicBoolean(false);

        private ManagedSpan(final Span span) {
            this.span = span;
        }

        @Override
        public Span getSpan() {
            return span;
        }

        public void deactivate() {
            if (deactivated.compareAndSet(false, true)) {
                ManagedSpan current = ACTIVE.get();
                if (this == current) {
                    while (current != null && current.deactivated.get()) {
                        current = current.parent;
                    }
                    if (current == null) ACTIVE.remove();
                    else ACTIVE.set(current);
                    LOGGER.log(Level.FINER, "Deactivated {0} and restored ACTIVE span to {1}.",
                            new Object[]{this, current});
                } else {
                    LOGGER.log(Level.FINE, "Deactivated {0} without affecting ACTIVE span {1}.",
                            new Object[]{this, current});
                }
            } else {
                LOGGER.log(Level.FINEST, "No action needed, {0} was already deactivated.", this);
            }
        }

        @Override
        public void close() {
            deactivate();
        }

        @Override
        public String toString() {
            return "ManagedSpan{" + span + '}';
        }
    }
}
