package io.opentracing.contrib.spanmanager;

import io.opentracing.NoopSpan;
import io.opentracing.Span;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static utility to access the <em>active</em> {@link Span}.
 * <p>
 * The {@link SpanManager} responsible for managing the <em>active</em> span is lazily resolved:
 * <ol type="a">
 * <li>The last-{@link #register(SpanManager) registered} SpanManager always takes precedence.</li>
 * <li>If no SpanManager was registered, one is looked up from the {@link ServiceLoader}.<br>
 * The ActiveSpanManager will not attempt to choose between implementations:</li>
 * <li>If no single implementation is found, the default SpanManager will be used.</li>
 * </ol>
 * <p>
 * The {@linkplain ThreadLocalSpanManager default SpanManager} uses {@link ThreadLocal} storage,
 * maintaining a stack-like structure of linked {@link ManagedSpan managed spans}.
 *
 * @see io.opentracing.contrib.spanmanager.concurrent.SpanPropagatingExecutors
 * @see io.opentracing.contrib.spanmanager.concurrent.SpanPropagatingExecutorService
 */
public final class ActiveSpanManager {
    private static final Logger LOGGER = Logger.getLogger(ActiveSpanManager.class.getName());

    /**
     * The resolved {@link SpanManager} to delegate the implementation to.
     * <p>
     * This can be an {@link #register(SpanManager) explicitly registered} or
     * the {@link #loadSingleSpiImplementation() automatically resolved} SpanManager
     * (or <code>null</code> during initialization).
     */
    private static final AtomicReference<SpanManager> ACTIVE_SPANMANAGER = new AtomicReference<SpanManager>();

    private ActiveSpanManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * Lazily resolves the active SpanManager.
     * <p>
     * The active SpanManager can be {@link #register(SpanManager) re-configured} at any time.
     *
     * @return The active span manager (non-<code>null</code>).
     */
    private static SpanManager activeSpanManager() {
        SpanManager activeSpanManager = ACTIVE_SPANMANAGER.get();
        if (activeSpanManager == null) {
            final SpanManager resolved = loadSingleSpiImplementation();
            while (activeSpanManager == null && resolved != null) { // handle rare race condition
                ACTIVE_SPANMANAGER.compareAndSet(null, resolved);
                activeSpanManager = ACTIVE_SPANMANAGER.get();
            }
            LOGGER.log(Level.INFO, "Using ActiveSpanManager: {0}.", activeSpanManager);
        }
        return activeSpanManager;
    }

    /**
     * Explicitly configures a <code>SpanManager</code> to back the behaviour of the {@link ActiveSpanManager} methods.
     * <p>
     * The previous manager is returned so it can be:
     * <ul>
     * <li>{@link SpanManager#clear() cleared} as it no longer is the active manager, or</li>
     * <li>restored later, for example in a testing situation.</li>
     * </ul>
     *
     * @param spanManager Manager for in-process span propagation.
     * @return The previously active SpanManager, or <code>null</code> if there was none.
     */
    public static SpanManager register(SpanManager spanManager) {
        SpanManager previous = ACTIVE_SPANMANAGER.getAndSet(spanManager);
        LOGGER.log(Level.INFO, "Registered ActiveSpanManager {0} (previously {1}).", new Object[]{spanManager, previous});
        return previous;
    }

    /**
     * The {@link SpanManager#currentSpan()} from the active SpanManager.
     *
     * @return The active Span, or the <code>NoopSpan</code> if there is no active span.
     * @see SpanManager#currentSpan()
     */
    public static Span activeSpan() {
        Span activeSpan = activeSpanManager().currentSpan();
        return activeSpan != null ? activeSpan : NoopSpan.INSTANCE;
    }

    /**
     * {@link SpanManager#manage(Span) Makes span the current span} of the active SpanManager.
     *
     * @param span The span to become the new active span.
     * @return The ManagedSpan to release the new active span with.
     * @see SpanManager#manage(Span)
     * @see ManagedSpan#release()
     */
    public static ManagedSpan activate(Span span) {
        return activeSpanManager().manage(span);
    }

    /**
     * Unconditional {@link SpanManager#clear() cleanup} of all active spans from the active SpanManager.
     *
     * @see SpanManager#clear()
     */
    public static void clear() {
        activeSpanManager().clear();
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
            LOGGER.log(Level.WARNING, "More than one SpanManager service found. " +
                    "Falling back to default ThreadLocal implementation.");
        }
        return new ThreadLocalSpanManager();
    }

}
