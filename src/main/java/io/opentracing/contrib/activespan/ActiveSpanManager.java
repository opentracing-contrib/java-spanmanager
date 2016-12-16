package io.opentracing.contrib.activespan;

import io.opentracing.NoopSpan;
import io.opentracing.Span;
import io.opentracing.contrib.activespan.concurrent.SpanAwareCallable;
import io.opentracing.contrib.activespan.concurrent.SpanAwareRunnable;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the <em>active</em> {@link Span}.<br>
 * A {@link Span} becomes active in the current process after a call to {@link #activate(Span)}
 * and can be deactivated again by calling {@link #deactivate(SpanDeactivator)}.
 * <p>
 * The default implementation will use a {@link ThreadLocal ThreadLocal storage} to maintain the active {@link Span}
 * and its parents.
 * <p>
 * Custom implementations can be provided by:
 * <ol>
 * <li>calling {@link #setActiveSpanManager(ActiveSpanManager)} programmatically, or</li>
 * <li>defining a <code>META-INF/services/io.opentracing.contrib.activespan.ActiveSpanManager</code> service file
 * containing the classname of the implementation</li>
 * </ol>
 *
 * @author Sjoerd Talsma
 * @navassoc - activeSpan - io.opentracing.Span
 */
public abstract class ActiveSpanManager {
    private static final Logger LOGGER = Logger.getLogger(ActiveSpanManager.class.getName());
    private static final AtomicReference<ActiveSpanManager> INSTANCE = new AtomicReference<ActiveSpanManager>();

    /**
     * Managed object to deactivate an activated {@link Span} with.
     */
    public interface SpanDeactivator {
    }

    /**
     * Programmatically sets an <code>ActiveSpanManager</code> implementation as <em>the</em> singleton instance.
     * <p>
     * Any previous manager is returned so it can be restored if necessary, for example in a testing situation.
     *
     * @param instance The overridden implementation to use for in-process span management.
     * @return Any previous <code>ActiveSpanManager</code> that was initialized before, or <code>null</code>.
     */
    public static ActiveSpanManager setActiveSpanManager(ActiveSpanManager instance) {
        return INSTANCE.getAndSet(instance);
    }

    /**
     * Return the active {@link Span}.
     *
     * @return The active Span, or the <code>NoopSpan</code> if there is no active span.
     */
    public static Span activeSpan() {
        try {
            Span activeSpan = getInstance().getActiveSpan();
            if (activeSpan != null) return activeSpan;
        } catch (Exception activeSpanException) {
            LOGGER.log(Level.WARNING, "Could not obtain active span.", activeSpanException);
        }
        return NoopSpan.INSTANCE;
    }

    /**
     * Makes span the <em>active span</em> within the running process.
     * <p>
     * Any exception thrown by the {@link #setActiveSpan(Span) implementation} is logged and will return
     * no {@link SpanDeactivator} (<code>null</code>) because tracing code must not break application functionality.
     *
     * @param span The span to become the active span.
     * @return The object that will restore any currently <em>active</em> deactivated, or <code>null</code>.
     * @see #activeSpan()
     * @see #deactivate(SpanDeactivator)
     */
    public static SpanDeactivator activate(Span span) {
        try {
            if (span == null) span = NoopSpan.INSTANCE;
            return getInstance().setActiveSpan(span);
        } catch (Exception activationException) {
            LOGGER.log(Level.WARNING, "Could not activate {0}.", new Object[]{span, activationException});
            return null;
        }
    }

    /**
     * Invokes the given {@link SpanDeactivator} which should normally* reactivate the parent of the <em>active span</em>
     * within the running process.
     * <p>
     * Any exception thrown by the implementation is logged and swallowed because tracing code must not break
     * application functionality.
     * <p>
     * <em>*) should normally</em> because the default stack unwinding algorithm is a little more intricate
     * to deal with out-of-order deactivation.
     *
     * @param deactivator The deactivator that was received upon span activation.
     * @see #activate(Span)
     */
    public static void deactivate(SpanDeactivator deactivator) {
        if (deactivator != null) try {
            getInstance().deactivateSpan(deactivator);
        } catch (Exception deactivationException) {
            LOGGER.log(Level.WARNING, "Could not deactivate {0}.", new Object[]{deactivator, deactivationException});
        }
    }

    /**
     * Deactivates any active span including any active parents.
     * <p>
     * This method allows boundary filters to deactivate all active spans
     * before returning control over their Thread, which may end up back in some threadpool.
     *
     * @return <code>true</code> if any spans were deactivated, otherwise <code>false</code>.
     */
    public static boolean deactivateAll() {
        try {
            return getInstance().deactivateAllSpans();
        } catch (Exception clearException) {
            LOGGER.log(Level.WARNING, "Could not clear active spans.", clearException);
            return false;
        }
    }

    /**
     * Wraps the {@link Callable} to execute with the {@link ActiveSpanManager#activeSpan() active span}
     * from the scheduling thread.
     *
     * @param callable The callable to wrap.
     * @param <V>      The return type of the wrapped call.
     * @return The wrapped call executing with the active span of the scheduling process.
     */
    public static <V> SpanAwareCallable<V> spanAware(Callable<V> callable) {
        return SpanAwareCallable.of(callable);
    }

    /**
     * Wraps the {@link Runnable} to execute with the {@link ActiveSpanManager#activeSpan() active span}
     * from the scheduling thread.
     *
     * @param runnable The runnable to wrap.
     * @return The wrapped runnable executing with the active span of the scheduling process.
     */
    public static SpanAwareRunnable spanAware(Runnable runnable) {
        return SpanAwareRunnable.of(runnable);
    }

    /**
     * Implementation of the {@link #activeSpan()} method.
     */
    protected abstract Span getActiveSpan();

    /**
     * Implementation of the {@link #activate(Span)} method.
     */
    protected abstract SpanDeactivator setActiveSpan(Span span);

    /**
     * Implementation of the {@link #deactivate(SpanDeactivator)} method.
     */
    protected abstract void deactivateSpan(SpanDeactivator deactivator);

    /**
     * Implementation of the {@link #deactivateAll()} method.
     */
    protected abstract boolean deactivateAllSpans();

    /**
     * Loads a single service implementation from {@link ServiceLoader} or returns the
     * {@link #setActiveSpanManager(ActiveSpanManager) explicitly configured} manager instance.
     *
     * @return The single implementation or the ThreadLocalSpanManager.
     */
    private static ActiveSpanManager getInstance() {
        ActiveSpanManager instance = INSTANCE.get();
        if (instance == null) {
            ActiveSpanManager singleton = null;

            // Use the service instance from the ServiceLoader if there is exactly one implementation.
            for (Iterator<ActiveSpanManager> implementations = ServiceLoader
                    .load(ActiveSpanManager.class, ActiveSpanManager.class.getClassLoader())
                    .iterator(); singleton == null && implementations.hasNext(); ) {
                ActiveSpanManager implementation = implementations.next();
                if (implementation != null) {
                    LOGGER.log(Level.FINEST, "Service loaded: {0}.", implementation);
                    if (implementations.hasNext()) { // Don't load the next implementation but fall-back to default.
                        LOGGER.log(Level.WARNING, "More than one ActiveSpanManager service implementation found. " +
                                "Falling back to default implementation.");
                        break;
                    } else {
                        singleton = implementation;
                    }
                }
            }

            if (singleton == null) {
                LOGGER.log(Level.FINEST, "No ActiveSpanManager service implementation found. " +
                        "Falling back to default implementation.");
                singleton = new ThreadLocalSpanManager();
            }

            while (instance == null) { // deal with race condition (should rarely repeat)
                INSTANCE.compareAndSet(null, singleton);
                instance = INSTANCE.get();
            }
            LOGGER.log(Level.FINE, "Singleton ActiveSpanManager implementation: {0}.", instance);
        }
        return instance;
    }

}
