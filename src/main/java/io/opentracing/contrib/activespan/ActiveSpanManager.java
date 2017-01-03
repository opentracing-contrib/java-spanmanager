package io.opentracing.contrib.activespan;

import io.opentracing.Span;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the <em>active</em> {@link Span}.<br>
 * A {@link Span} becomes active in the current process after a call to {@link #activate(Span)}
 * and can be deactivated again by calling {@link SpanDeactivator#deactivate()}.
 */
public abstract class ActiveSpanManager {
    private static final Logger LOGGER = Logger.getLogger(ActiveSpanManager.class.getName());
    private static final AtomicReference<ActiveSpanManager> INSTANCE = new AtomicReference<ActiveSpanManager>();

    /**
     * Returns the {@linkplain ActiveSpanManager} implementation.
     * <p>
     * The default implementation will use a {@link ThreadLocal ThreadLocal storage} to maintain the active {@link Span}
     * and its parents.
     * <p>
     * Custom implementations can be provided by:
     * <ol>
     * <li>calling {@link #set(ActiveSpanManager)} programmatically, or</li>
     * <li>defining a <code>META-INF/services/io.opentracing.contrib.activespan.ActiveSpanManager</code> service file
     * containing the classname of the implementation</li>
     * </ol>
     *
     * @return The ActiveSpanManager implementation.
     */
    public static ActiveSpanManager get() {
        ActiveSpanManager activeSpanManager = INSTANCE.get();
        if (activeSpanManager == null) {
            ActiveSpanManager implementation = loadSingleSpiImplementation();
            while (activeSpanManager == null && implementation != null) { // handle rare race condition
                INSTANCE.compareAndSet(null, implementation);
                activeSpanManager = INSTANCE.get();
            }
            LOGGER.log(Level.FINE, "ActiveSpanManager implementation: {0}.", activeSpanManager);
        }
        return activeSpanManager;
    }

    /**
     * Programmatically sets an <code>ActiveSpanManager</code> implementation as <em>the</em> singleton instance.
     * <p>
     * Any previous manager is returned so it can be restored if necessary, for example in a testing situation.
     *
     * @param instance The overridden implementation to use for in-process span management.
     * @return Any previous <code>ActiveSpanManager</code> that was initialized before, or <code>null</code>.
     */
    public static ActiveSpanManager set(ActiveSpanManager instance) {
        return INSTANCE.getAndSet(instance);
    }

    /**
     * Return the active {@link Span}.
     *
     * @return The active Span, or the <code>NoopSpan</code> if there is no active span.
     * @see #activeSpan()
     */
    public abstract Span activeSpan();

    /**
     * Makes span the <em>active span</em> within the running process.
     *
     * @param span The span to become the active span.
     * @return The deactivator to undo the activation.
     * @see #activeSpan()
     */
    public abstract SpanDeactivator activate(Span span);

    /**
     * Deactivates all active spans in the current process which includes any active parents.
     * <p>
     * This method allows boundary filters to deactivate all active spans
     * before returning control over their Thread, which may end up back in some threadpool.<br>
     *
     * @see SpanDeactivator#deactivate()
     */
    public abstract void clear();

//    /**
//     * Wraps the {@link Callable} to execute with the {@link ActiveSpanManager#activeSpan() active span}
//     * from the scheduling thread.
//     *
//     * @param callable The callable to wrap.
//     * @param <V>      The return type of the wrapped call.
//     * @return The wrapped call executing with the active span of the scheduling process.
//     */
//    public static <V> CallableWithActiveSpan<V> withActiveSpan(Callable<V> callable) {
//        return CallableWithActiveSpan.of(callable);
//    }
//
//    /**
//     * Wraps the {@link Runnable} to execute with the {@link ActiveSpanManager#activeSpan() active span}
//     * from the scheduling thread.
//     *
//     * @param runnable The runnable to wrap.
//     * @return The wrapped runnable executing with the active span of the scheduling process.
//     */
//    public static RunnableWithActiveSpan withActiveSpan(Runnable runnable) {
//        return RunnableWithActiveSpan.of(runnable);
//    }

    private static ActiveSpanManager loadSingleSpiImplementation() {
        // Use the ServiceLoader to find the declared ActiveSpanManager implementation.
        Iterator<ActiveSpanManager> spiImplementations =
                ServiceLoader.load(ActiveSpanManager.class, ActiveSpanManager.class.getClassLoader()).iterator();
        if (spiImplementations.hasNext()) {
            ActiveSpanManager foundImplementation = spiImplementations.next();
            if (!spiImplementations.hasNext()) {
                return foundImplementation;
            }
            LOGGER.log(Level.WARNING, "More than one ActiveSpanManager service implementation found. " +
                    "Falling back to default ThreadLocal implementation.");
        }
        return new ThreadLocalSpanManager();
    }
}
