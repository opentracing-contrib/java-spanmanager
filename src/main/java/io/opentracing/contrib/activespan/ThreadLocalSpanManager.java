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
 * <li>If the deactivated <em>managed span</em> is not the active span, the active span is left alone.</li>
 * <li>Otherwise, the first parent that is <em>not yet deactivated</em> is set as the new active span.</li>
 * <li>If no active parents remain, the <em>active span</em> {@link ThreadLocal} is cleared.</li>
 * <li>Consecutive <code>deactivate()</code> calls for already deactivated spans will be ignored.</li>
 * </ol>
 *
 * @author Sjoerd Talsma
 */
final class ThreadLocalSpanManager extends ActiveSpanManager {

    private static final Logger LOGGER = Logger.getLogger(ActiveSpanManager.class.getName());
    private static final ThreadLocal<ManagedSpan> ACTIVE = new ThreadLocal<ManagedSpan>();

    ThreadLocalSpanManager() {
    }

    public Span getActiveSpan() {
        final ManagedSpan activeSpan = ACTIVE.get();
        return activeSpan != null ? activeSpan.span : NoopSpan.INSTANCE;
    }

    public SpanDeactivator setActiveSpan(Span span) {
        final ManagedSpan managedSpan = new ManagedSpan(span);
        ACTIVE.set(managedSpan);
        return managedSpan;
    }

    public void deactivateSpan(SpanDeactivator deactivator) {
        if (!(deactivator instanceof ManagedSpan)) {
            throw new IllegalArgumentException(
                    "Cannot deactivate " + deactivator + ". It was not issued by this ActiveSpanManager!");
        }
        ((ManagedSpan) deactivator).deactivate();
    }

    public boolean deactivateAllSpans() {
        boolean deactivated = ACTIVE.get() != null;
        ACTIVE.remove();
        return deactivated;
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

        private void deactivate() {
            if (deactivated.compareAndSet(false, true)) {
                ManagedSpan current = ACTIVE.get();
                if (this == current) {
                    while (current != null && current.deactivated.get()) {
                        current = current.parent;
                    }
                    if (current == null) ACTIVE.remove();
                    else ACTIVE.set(current);
                    LOGGER.log(Level.FINER, "Deactivated {0} and restored active span to {1}.",
                            new Object[]{this, current});
                } else {
                    LOGGER.log(Level.FINE, "Deactivated {0} without affecting active span {1}.",
                            new Object[]{this, current});
                }
            } else {
                LOGGER.log(Level.FINEST, "No action needed, {0} was already deactivated.", this);
            }
        }

        @Override
        public String toString() {
            return "ManagedSpan{" + span + '}';
        }
    }
}
