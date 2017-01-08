package io.opentracing.contrib.spanmanager;

import io.opentracing.Span;
import io.opentracing.Tracer;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Forwards all methods to another {@linkplain SpanManager} that can be configured in one of two ways:
 * <ol>
 * <li>Explicitly, calling {@link #register(SpanManager)} with a configured span manager, or:</li>
 * <li>Automatically using the Java {@link ServiceLoader} SPI mechanism to load a {@linkplain SpanManager}
 * from the classpath.</li>
 * </ol>
 * <p>
 * When the SpanManager is needed it is lazily looked up using the following rules:
 * <ol type="a">
 * <li>The last-{@link #register(SpanManager) registered} SpanManager always takes precedence.</li>
 * <li>If no SpanManager was registered, one is looked up from the {@link ServiceLoader}.<br>
 * The {@linkplain GlobalSpanManager} will not attempt to choose between implementations:</li>
 * <li>If no single SpanManager service is found, the {@link DefaultSpanManager} will be used.</li>
 * </ol>
 *
 * @see io.opentracing.contrib.spanmanager.concurrent.SpanPropagatingExecutors
 * @see io.opentracing.contrib.spanmanager.concurrent.SpanPropagatingExecutorService
 */
public final class GlobalSpanManager implements SpanManager {
    private static final Logger LOGGER = Logger.getLogger(GlobalSpanManager.class.getName());

    /**
     * Singleton instance.
     * <p>
     * Since we cannot prevent people using {@linkplain #get() GlobalSpanManager.get()} as a constant,
     * this guarantees that references obtained before, during or after initialization
     * all behave as if obtained <em>after</em> initialization once properly initialized.<br>
     * As a minor additional benefit it makes it harder to circumvent the {@link Tracer} API.
     */
    private static final GlobalSpanManager INSTANCE = new GlobalSpanManager();

    /**
     * The resolved {@link SpanManager} to delegate the implementation to.
     * <p>
     * This can be an {@link #register(SpanManager) explicitly registered} or
     * the {@link #loadSingleSpiImplementation() automatically resolved} SpanManager
     * (or <code>null</code> during initialization).
     */
    private final AtomicReference<SpanManager> globalSpanManager = new AtomicReference<SpanManager>();

    private GlobalSpanManager() {
    }

    /**
     * Returns the constant {@linkplain GlobalSpanManager}.
     * <p>
     * All methods are forwarded to the currently configured SpanManager.<br>
     * Until a tracer is {@link #register(SpanManager) explicitly configured},
     * one is looked up from the {@link ServiceLoader},
     * falling back to the {@link DefaultSpanManager}.<br>
     * A span manager can be re-configured at any time.
     * For example, the span manager used to manage a new span
     * may be different than the one backing the {@link SpanManager.ManagedSpan ManagedSpan}
     * from the previous span.
     *
     * @return The global tracer constant.
     * @see #register(SpanManager)
     */
    public static SpanManager get() {
        return INSTANCE;
    }

    /**
     * Lazily resolves the global SpanManager.
     * <p>
     * This indirection guarantees a valid SpanManager is available before, during and after
     * application initialization.<br>
     * It also means the global SpanManager can be {@link #register(SpanManager) re-configured} at any time.
     *
     * @return The global SpanManager (non-<code>null</code>).
     */
    private static SpanManager lazySpanManager() {
        SpanManager spanManager = INSTANCE.globalSpanManager.get();
        if (spanManager == null) {
            final SpanManager resolved = loadSingleSpiImplementation();
            while (spanManager == null && resolved != null) { // handle rare race condition
                INSTANCE.globalSpanManager.compareAndSet(null, resolved);
                spanManager = INSTANCE.globalSpanManager.get();
            }
            LOGGER.log(Level.INFO, "Using GlobalSpanManager: {0}.", spanManager);
        }
        return spanManager;
    }

    /**
     * Explicitly configures a <code>SpanManager</code> to back the behaviour of the {@link GlobalSpanManager} methods.
     * <p>
     * The previous SpanManager is returned so it can be:
     * <ul>
     * <li>{@link SpanManager#clear() cleared} as it no longer is the global manager, or</li>
     * <li>restored later, for example in a testing situation.</li>
     * </ul>
     *
     * @param spanManager New SpanManager for span propagation.
     * @return The previously global SpanManager, or <code>null</code> if there was none.
     */
    public static SpanManager register(SpanManager spanManager) {
        SpanManager previous = INSTANCE.globalSpanManager.getAndSet(spanManager);
        LOGGER.log(Level.INFO, "Registered GlobalSpanManager {0} (previously {1}).", new Object[]{spanManager, previous});
        return previous;
    }

    @Override
    public Span currentSpan() {
        return lazySpanManager().currentSpan();
    }

    @Override
    public ManagedSpan manage(Span span) {
        return lazySpanManager().manage(span);
    }

    @Override
    public void clear() {
        lazySpanManager().clear();
    }

    /**
     * Loads a single service implementation from {@link ServiceLoader}.
     *
     * @return The single service or the <code>DefaultSpanManager</code>.
     * @see DefaultSpanManager
     */
    private static SpanManager loadSingleSpiImplementation() {
        // Use the ServiceLoader to find the declared SpanManager implementation.
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
        return DefaultSpanManager.getInstance();
    }

}
