# java-activespan

This library provides a way to manage an _active span_ within the current process
and propagate it to other threads.

## ActiveSpanManager

The core of this library, this class provides
 1. The `activeSpan()` method to return the _active span_ in the current process.   
    If there is no active span, a `NoopSpan` is returned instead.
 2. The _active span_ can be set through the `activate()` method,
    returning a `SpanDeactivator`.
 3. Method `clear()` to deactivate _all active spans_ in a process, including any active parents.
    Not intended for 'regular' use, this method is available for boundary filters, 
    making sure all spans are cleared before the current process is repurposed by a threadpool. 

## SpanDeactivator

Returned by `ActiveSpanManager.activate()` this object can `deactivate()` a 
previously activated `Span` again.
It implements `Closeable` for easy use in try-with-resources blocks.

## Concurrency

### SpanAwareExecutorService

This `ExecutorService` wraps an existing threadpool and propagates the _active span_
of the caller into the tasks it executes.  
No `Spans` are started or finished automatically by this executor.  
Convenience variants of the java `Executors` factory methods are provided by the `SpanAwareExecutors` class:
 - `SpanAwareExecutors.newFixedThreadPool(int)`
 - `SpanAwareExecutors.newSingleThreadExecutor()`
 - `SpanAwareExecutors.newCachedThreadPool()`
 - Variants of the above with additional `ThreadFactory` argument.

### ActiveSpanTracer

This `Tracer` makes managing the _active span_ easier.
 1. It is a _wrapper_ that forwards all calls to another `Tracer` implementation.
 2. `Span` instances created with this tracer are automatically 
    _activated_ when started and _deactivated_ when finished.
 3. The `SpanBuilder` of this tracer will short-circuit to the `NoopSpanBuilder`.
    This means any `NoopSpan` instances created do not automatically become the active span.

## Custom span managers

It is possible to provide a custom implementation of `ActiveSpanManager`.
This may be useful if you already have a method in place to propagate contextual information
from one thread to another. Creating a custom manager is one way to piggyback the _active span_ on
your existing propagation mechanism. Another way would be to call _activate_ and _deactivate_
at the appropriate moments in your propagation mechanism.  

To enable a custom manager:
 1. Programmatically, call the `ActiveSpanManager.set()` with the new implementation.
 2. Using the standard Java `ServiceLoader`, bundle a 
    `META-INF/services/io.opentracing.contrib.activespan.ActiveSpanManager` service file
    containing the classname of the implementation.

## Examples

_TODO create examples_
