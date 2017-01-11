# opentracing-java-spanmanager

Warning: This library is still a work in progress!

This library provides a way to manage spans and propagate them to other threads.

## SpanManager

Interface that defines _current_ span management:
 1. `manage(span)` makes the given span the _current_ managed span.  
    Returns a `ManagedSpan` containing a `release()` method
    to later 'unmanage' the span with.
 2. `currentSpan()` returns the _current_ managed span,
    or the `NoopSpan` if no span is managed.
 3. `clear()` provides unconditional cleanup of _all managed spans_ for the current process.

## DefaultSpanManager

A _default_ SpanManager maintaining a `Stack`-like `ThreadLocal` storage of _linked managed spans_.

Releasing a _linked managed span_ uses the following algorithm:
 1. If the released span is not the _current_ span, the current span is left alone.
 2. Otherwise, the first parent that is <em>not yet released</em> is set as the new current span.
 3. If no current parents remain, the current span is cleared.
 4. Consecutive `release()` calls for already-released spans will be ignored.

## Concurrency

### SpanPropagatingExecutorService

This `ExecutorService` _propagates the current span_ 
from the caller into each call that is executed.  
The current span of the caller is obtained from the configured `SpanManager`.

_Please Note:_ The active span is merely _propagated_ (as-is).  
It is explicitly **not** finished when the calls end,
nor will new spans be automatically related to the propagated span.

### SpanPropagatingExecutors

Contains factory-methods similar to standard java `Executors`:  
 - `SpanPropagatingExecutors.newFixedThreadPool(int, SpanManager)`
 - `SpanPropagatingExecutors.newSingleThreadExecutor(SpanManager)`
 - `SpanPropagatingExecutors.newCachedThreadPool(SpanManager)`
 - Variants of the above with additional `ThreadFactory` argument.

## ManagedSpanTracer

This convenience `Tracer` automates managing the _current span_:
 1. It wraps another `Tracer`.
 2. `Spans` created with this tracer are:
    - automatically _managed_ when started, and
    - automatically _released_ when finished.

## Examples

### Manually propagating any Span into a background thread

To propagate a `Span` into a new `Thread`, the _currentSpan_ from the caller must be
remembered by the `Runnable`:

```java
    class ExampleRunnable implements Runnable {
        private final SpanManager spanManager;
        private final Span currentSpanFromCallingThread;
        
        ExampleRunnable(SpanManager spanManager) {
            this(spanManager, NoopSpan.INSTANCE);
        }
        
        private ExampleRunnable(SpanManager spanManager, Span currentSpanFromCallingThread) {
            this.spanManager = spanManager;
            this.currentSpanFromCallingThread = currentSpanFromCallingThread;
        }
        
        ExampleRunnable withCurrentSpan() {
            return new ExampleRunnable(spanManager, spanManager.currentSpan());
        }
        
        @Override
        public void run() {
            try (ManagedSpan parent = spanManager.manage(currentSpanFromCallingThread)) { // propagate currentSpan()

                // Any background code that requires tracing
                // and may use spanManager.currentSpan().
                
            } // parent.release() restores spanManager.currentSpan() to NoopSpan in new thread.
        }
    }
```

Then the application can propagate this _currentSpan_ into background threads:

```java
    class App {
        public static void main(String... args) throws InterruptedException {
            Config config = ...;
            Tracer tracer = config.getTracer();
            SpanManager spanManager = config.getSpanManager();
            ExampleRunnable runnable = new ExampleRunnable(spanManager);

            try (Span appSpan = tracer.buildSpan("main").start();           // start appSpan
                    ManagedSpan managed = spanManager.manage(appSpan)) {    // set currentSpan() to appSpan
            
                Thread example = new Thread(runnable.withCurrentSpan());
                example.start();
                example.join();
                
            } // managed.release() + appSpan.finish()
            
            System.exit(0);
        }
    }

```

### Threadpool that propagates SpanManager.currentSpan() into threads

```java
    class TracedCall implements Callable<String> {
        SpanManager spanManager = ... // inject or DefaultSpanManager.getInstance();
        
        @Override
        public String call() {
            Span currentSpan = spanManager.currentSpan();
            try (Span newSpan = tracer.buildSpan("someCall").asChildOf(currentSpan.context()).start()) {
                return "New span: " + newSpan + ", parent: " + parent;
            }
        }
    }

    class Caller {
        SpanManager spanManager = ... // inject or DefaultSpanManager.getInstance(); 
        ExecutorService propagatingThreadpool = new SpanPropagatingExecutorService(anyThreadpool(), spanManager);

        void run() {
            // ...code that sets the current Span somewhere:
            try (ManagedSpan current = spanManager.manage(someSpan)) {
                
                // scheduling the traced call:
                Future<String> result = propagatingThreadpool.submit(new TracedCall());
                
            }
        }
    }

```

### Propagating threadpool with 'ManagedSpan' Tracer

When starting a new span and making it the _currentSpan_, the manual example above used:
```java
    try (Span span = tracer.buildSpan("main").start();           // start span
            ManagedSpan managed = spanManager.manage(span)) {    // set currentSpan() to span
        // ...traced block of code...
    }
```

The `ManagedSpanTracer` automatically makes any started span the active span
and also releases it when the span is finished:

```java

    class Caller {
        SpanManager spanManager = ... // inject or DefaultSpanManager.getInstance();
        Tracer tracer = new ManagedSpanTracer(anyTracer(), spanManager);
        ExecutorService propagatingThreadpool = new SpanPropagatingExecutorService(anyThreadpool(), spanManager);

        void run() {
            try (Span parent = tracer.buildSpan("parentOperation").start()) { // parent == spanManager.currentSpan()
            
                // Scheduling the traced call:
                Future<String> result = propagatingThreadpool.submit(new TracedCall());
                
            } // parent.finish() + ((ManagedSpan) parent).release()   // Performed by ManagedSpanTracer
        }
    }

```

