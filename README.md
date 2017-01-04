# java-activespan

This library provides a way to manage a _current span_ and propagate it to other threads.

## SpanManager

The core of this library, this interface defines
 1. The `currentSpan()` method to return the _currently managed span_.
 2. The `manage(Span)` method to make the specified `Span` the currently managed span.
    A `ManagedSpan` instance is returned to release this _currently managed span_.
 3. The `clear()` method to releases all managed spans including any parents.

## ManagedSpan

Obtained from the `SpanManager`, this object can be used to 
release the _currently managed span_ again.
 1. The `getSpan()` method returns the managed `Span` object.
 2. The `release()` method 'unmanages' the span.  
    Implementors are encouraged to restore the managed span as it was before,
    providing stack-like behaviour.  
    It must be possible to repeatedly call `release()` without side effects.
 3. `ManagedSpan` extends `Closeable` where `close()` is an alias for `release()`
    so it can reliably be used in `try-with-resources` blocks.  
    **Please note:** _Closing a ManagedSpan does **not** automatically finish the actual span._  
    (with the `ManagedSpanTracer` being an exception to this rule)

## ActiveSpanManager

Provides the `ActiveSpanManager.get()` method that returns the singleton _active span manager_.  
Upon first use of this manager, it lazily determines which `SpanManager` implementation to use:
 1. If an explicitly configured `SpanManager` was provided via the `ActiveSpanManager.set()` method,
    that will always take precedence over automatically resolved manager instances.
 2. A `SpanManager` implementation can be automatically provided using the java `ServiceLoader` through the
    `META-INF/services/io.opentracing.contrib.spanmanager.SpanManager` service definition file.
    The ActiveSpanManager class will not attempt to choose between implementations;
    if more than one is found, a warning is logged and the default implementation is used:
 3. If no `SpanManager` is found, a default implementation is used,
    providing `ThreadLocal` storage to manage the active span.

## Concurrency

### SpanAwareExecutorService

This `ExecutorService` wraps an existing threadpool and propagates the currently active span
of the caller into the tasks it executes.  
Please note: This ExecutorService does not automatically start or finish spans,
but merely propagates the _currently active_ span into the jobs being executed.

### SpanAwareExecutors

Factory-methods similar to standard java `Executors`:  
 - `SpanAwareExecutors.newFixedThreadPool(int)`
 - `SpanAwareExecutors.newSingleThreadExecutor()`
 - `SpanAwareExecutors.newCachedThreadPool()`
 - Variants of the above with additional `ThreadFactory` argument.

### ManagedSpanTracer

This convenience `Tracer` automates managing the _active span_:
 1. It is a wrapper that forwards all calls to another `Tracer` implementation.
 2. `Span` instances created with this tracer are managed when started
    (automatically becoming the currently active span)
    and released when finished.

## Examples

_TODO create examples_
