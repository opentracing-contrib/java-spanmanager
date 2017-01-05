package io.opentracing.contrib.spanmanager;

import io.opentracing.NoopSpan;
import io.opentracing.Span;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static utility to access the <em>currently active</em> {@link Span}.
 * <p>
 * The {@link SpanManager} responsible for managing the <em>currently active</em> span is lazily resolved:
 * <ol type="a">
 * <li>The last-{@link #register(SpanManager) registered} span manager always takes precedence.</li>
 * <li>If no manager was registered, one is looked up from the {@link ServiceLoader}.<br>
 * The ActiveSpanManager will not attempt to choose between implementations:</li>
 * <li>If no single implementation is found, a default implementation will be used.</li>
 * </ol>
 * <p>
 * The {@linkplain ThreadLocalSpanManager default implementation} uses {@link ThreadLocal} storage,
 * maintaining a stack-like structure of linked managed spans.
 *
 * @see io.opentracing.contrib.spanmanager.concurrent.SpanPropagatingExecutors
 * @see io.opentracing.contrib.spanmanager.concurrent.SpanPropagatingExecutorService
 */
public final class ActiveSpanManager {
    private static final Logger LOGGER = Logger.getLogger(ActiveSpanManager.class.getName());

    /**
     * The resolved {@link SpanManager} to delegate the implementation to.
     * <p>
     * This can be an {@link #register(SpanManager) explicitly registered delegate}
     * or an {@link #loadSingleSpiImplementation() automatically resolved SpanManager service},
     * (or <code>null</code> during initialization).
     */
    private static final AtomicReference<SpanManager> ACTIVE_SPAN_MANAGER = new AtomicReference<SpanManager>();

    private ActiveSpanManager() {
        throw new UnsupportedOperationException();
    }

    private static SpanManager lazySpanManager() {
        SpanManager spanManager = ACTIVE_SPAN_MANAGER.get();
        if (spanManager == null) {
            final SpanManager resolved = loadSingleSpiImplementation();
            while (spanManager == null && resolved != null) { // handle rare race condition
                ACTIVE_SPAN_MANAGER.compareAndSet(null, resolved);
                spanManager = ACTIVE_SPAN_MANAGER.get();
            }
            LOGGER.log(Level.INFO, "Using SpanManager implementation: {0}.", spanManager);
        }
        return spanManager;
    }

    /**
     * Explicitly configures a <code>SpanManager</code> to back the behaviour of the {@link ActiveSpanManager}.
     * <p>
     * The previous manager is returned so it can be:
     * <ul>
     * <li>{@link SpanManager#clear() cleared} as it no longer is the active manager, or</li>
     * <li>restored later, for example in a testing situation</li>
     * </ul>
     *
     * @param spanManager Manager for in-process span propagation.
     * @return The previously active SpanManager, or <code>null</code> if there was none.
     */
    public static SpanManager register(SpanManager spanManager) {
        SpanManager previous = ACTIVE_SPAN_MANAGER.getAndSet(spanManager);
        LOGGER.log(Level.INFO, "Registered ActiveSpanManager {0} (previously {1}).", new Object[]{spanManager, previous});
        return previous;
    }

    /**
     * Return the active {@link Span} in the current process.
     *
     * @return The currently active Span, or the <code>NoopSpan</code> if there is no active span.
     * @see SpanManager#currentSpan()
     */
    public static Span activeSpan() {
        Span activeSpan = lazySpanManager().currentSpan();
        return activeSpan != null ? activeSpan : NoopSpan.INSTANCE;
    }

    /**
     * Makes span the <em>active span</em> for the current process.
     *
     * @param span The span to become the new active span.
     * @return The managed object to release the new active span with.
     * @see SpanManager#manage(Span)
     * @see ManagedSpan#release()
     */
    public static ManagedSpan activate(Span span) {
        return lazySpanManager().manage(span);
    }

    /**
     * Unconditional cleanup of all active spans for the current process.
     *
     * @see SpanManager#clear()
     */
    public static void clear() {
        lazySpanManager().clear();
    }

    /**
     * Loads a single service implementation from {@link ServiceLoader}.
     *
     * @return The single service or a new <code>ThreadLocalSpanManager</code>.
     */
    private static SpanManager loadSingleSpiImplementation() {
        // Use the ServiceLoader to find the declared ActiveSpanManager implementation.
        Iterator<SpanManager> spiImplementations =
                ServiceLoader.load(SpanManager.class, SpanManager.class.getClassLoader()).iterator();
        if (spiImplementations.hasNext()) {
            SpanManager foundImplementation = spiImplementations.next();
            if (!spiImplementations.hasNext()) {
                return foundImplementation;
            }
            LOGGER.log(Level.WARNING, "More than one SpanManager service implementation found. " +
                    "Falling back to default ThreadLocal implementation.");
        }
        return new ThreadLocalSpanManager();
    }

}
