package io.opentracing.contrib.spanmanager;

import io.opentracing.Span;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the <em>currently active</em> {@link Span}.<br>
 * A {@link Span} becomes active in the current process after a call to {@link #manage(Span)}
 * and can be deactivated again by calling {@link ManagedSpan#release()}.
 */
public final class ActiveSpanManager implements SpanManager {
    private static final Logger LOGGER = Logger.getLogger(ActiveSpanManager.class.getName());

    /**
     * Singleton instance.
     * <p>
     * Since we cannot prevent people using {@linkplain #get() ActiveSpanManager.get()} as a constant,
     * this guarantees that references obtained before, during or after initialization
     * all behave as if obtained <em>after</em> initialization once properly initialized.<br>
     * As a minor additional benefit it makes it harder to circumvent the {@link SpanManager} API.
     */
    private static final ActiveSpanManager INSTANCE = new ActiveSpanManager();

    /**
     * The resolved {@link SpanManager} to delegate the implementation to.<br>
     * This can be either an {@link #set(SpanManager) explicitly set delegate}
     * or the automatically resolved service implementation.
     */
    private final AtomicReference<SpanManager> activeSpanManager = new AtomicReference<SpanManager>();

    private ActiveSpanManager() {
    }

    private SpanManager lazySpanManager() {
        SpanManager instance = this.activeSpanManager.get();
        if (instance == null) {
            final SpanManager singleton = loadSingleSpiImplementation();
            while (instance == null && singleton != null) { // handle rare race condition
                this.activeSpanManager.compareAndSet(null, singleton);
                instance = this.activeSpanManager.get();
            }
            LOGGER.log(Level.INFO, "Using ActiveSpanManager implementation: {0}.", instance);
        }
        return instance;
    }

    /**
     * Returns the {@linkplain SpanManager} instance.
     * Upon first use of any method, this manager lazily determines which actual {@link SpanManager} implementation
     * to use:
     * <ol type="a">
     * <li>If an explicitly configured manager was provided via the {@link #set(SpanManager)} method,
     * that will always take precedence over automatically resolved instances.</li>
     * <li>An implementation can automatically be provided using the Java {@link ServiceLoader} through the
     * <code>META-INF/services/io.opentracing.contrib.spanmanager.SpanManager</code> service definition file.<br>
     * The {@link ActiveSpanManager} will not attempt to choose between implementations;
     * if more than one is found by the {@linkplain ServiceLoader service loader},
     * a warning is logged and tracing is disabled by falling back to the default implementation:</li>
     * <li>If no tracer implementation is available, a default {@link ThreadLocal} based implementation is used.</li>
     * </ol>
     *
     * @return The active SpanManager.
     */
    public static SpanManager get() {
        return INSTANCE;
    }

    /**
     * Programmatically sets a <code>SpanManager</code> implementation as <em>the</em> singleton instance.
     * <p>
     * The previous manager is returned so it can be:
     * <ul>
     * <li>{@link #clear() cleared} as it no longer is the active manager, or</li>
     * <li>restored later, for example in a testing situation</li>
     * </ul>
     *
     * @param spanManager The overridden implementation to use for in-process span management.
     * @return The previous <code>SpanManager</code>, or <code>null</code> if there was none.
     */
    public static SpanManager set(SpanManager spanManager) {
        if (spanManager instanceof ActiveSpanManager) {
            LOGGER.log(Level.FINE, "Attempted to set the ActiveSpanManager as delegate of itself.");
            return INSTANCE.activeSpanManager.get(); // no-op, return 'previous' manager.
        }
        return INSTANCE.activeSpanManager.getAndSet(spanManager);
    }

    /**
     * Return the currently active {@link Span}.
     *
     * @return The currently active Span, or the <code>NoopSpan</code> if there is no active span.
     * @see SpanManager#manage(Span)
     */
    @Override
    public Span currentSpan() {
        return lazySpanManager().currentSpan();
    }

    /**
     * Makes span the <em>currently active span</em> within the running process.
     *
     * @param span The span to become the active span.
     * @return The managed object to release the currently active span with.
     * @see SpanManager#currentSpan()
     * @see ManagedSpan#release()
     */
    @Override
    public ManagedSpan manage(Span span) {
        return lazySpanManager().manage(span);
    }

    /**
     * Releases all managed spans including any parents.
     * <p>
     * This allows boundary filters to release all spans before relinquishing control over their Thread,
     * which may end up repurposed by a threadpool.
     *
     * @see ManagedSpan#release()
     */
    @Override
    public void clear() {
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
