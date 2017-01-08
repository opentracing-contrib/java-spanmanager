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
 * <li>If the released span is not the <em>managed</em> span, the <em>current managed</em> span is left alone.</li>
 * <li>Otherwise, the first parent that is <em>not yet released</em> is set as the new managed span.</li>
 * <li>If no managed parents remain, the <em>managed span</em> is cleared.</li>
 * <li>Consecutive <code>release()</code> calls for already-released spans will be ignored.</li>
 * </ol>
 */
public final class DefaultSpanManager implements SpanManager {

    private static final Logger LOGGER = Logger.getLogger(DefaultSpanManager.class.getName());

    private static final DefaultSpanManager INSTANCE = new DefaultSpanManager();
    private final ThreadLocal<LinkedManagedSpan> managed = new ThreadLocal<LinkedManagedSpan>();

    private DefaultSpanManager() {
    }

    public static SpanManager getInstance() {
        return INSTANCE;
    }

    @Override
    public Span currentSpan() {
        LinkedManagedSpan managedSpan = managed.get();
        return managedSpan != null ? managedSpan.span : NoopSpan.INSTANCE;
    }

    @Override
    public SpanManager.ManagedSpan manage(Span span) {
        LinkedManagedSpan managedSpan = new LinkedManagedSpan(span, managed.get());
        managed.set(managedSpan);
        return managedSpan;
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

        /**
         * Please see {@link DefaultSpanManager outer class description} for the stack-unwinding documentation.
         */
        public void release() {
            if (released.compareAndSet(false, true)) {
                LinkedManagedSpan current = managed.get();
                if (this == current) {
                    while (current != null && current.released.get()) {
                        current = current.parent;
                    }
                    if (current == null) managed.remove();
                    else managed.set(current);
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
            return "LinkedManagedSpan{" + span + '}';
        }
    }

}
