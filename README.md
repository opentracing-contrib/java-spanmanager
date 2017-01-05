# java-activespan

This library provides a way to manage spans and propagate them to other threads.

## ActiveSpanManager

Static utility to access the _currently active span_.
 1. The `activeSpan()` method returns the _active span_ in the current process.   
    If there is no active span, a `NoopSpan` is returned instead.
 2. This _active span_ can be set through the `activate(Span)` method,
    returning a `ManagedSpan` for later deactivation.
 3. `clear()` provides unconditional cleanup of _all active spans_ for the current process.
 4. A custom SpanManager can be registered by calling `register(SpanManager)`.
    _see section 'Custom span managers'_

## Concurrency

### SpanPropagatingExecutorService

This `ExecutorService` propagates the _active span_ from the caller into each call that is executed.  
Please Note: The active span is merely propagated as-is.
It is explicitly **not** finished by the calls.

### SpanPropagatingExecutors

Provides factory-methods similar to standard java `Executors`:  
 - `SpanPropagatingExecutors.newFixedThreadPool(int)`
 - `SpanPropagatingExecutors.newSingleThreadExecutor()`
 - `SpanPropagatingExecutors.newCachedThreadPool()`
 - Variants of the above with additional `ThreadFactory` argument.

### ManagedSpanTracer

This convenience `Tracer` automates managing the _active span_:
 1. It wraps another `Tracer`.
 2. `Spans` created with this tracer are:
    - automatically _activated_ when started, and
    - automatically _released_ when finished.

## Custom span managers

It is possible to provide a custom implementation for `ActiveSpanManager`.  
This may be useful if you already have a way to propagate contextual information
from one thread to another. Creating a custom manager is one way to piggyback the _active span_ on
your existing propagation mechanism.  

The _active_ SpanManager is resolved as follows:
 1. The last-registered span manager always takes precedence.
 2. If no manager was registered, one is looked up from the `ServiceLoader`.  
    The ActiveSpanManager will not attempt to choose between implementations:
 3. If no single implementation is found, the default SpanManager will be used.


## Examples

_TODO create examples_

