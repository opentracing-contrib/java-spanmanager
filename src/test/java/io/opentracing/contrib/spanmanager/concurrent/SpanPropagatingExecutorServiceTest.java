package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.NoopSpan;
import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.contrib.spanmanager.SpanManager;
import io.opentracing.contrib.spanmanager.SpanManager.ManagedSpan;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class SpanPropagatingExecutorServiceTest {

    static final ExecutorService threadpool = Executors.newCachedThreadPool();
    static final SpanManager spanManager = DefaultSpanManager.getInstance();

    SpanPropagatingExecutorService subject;

    @Before
    public void setUp() {
        subject = new SpanPropagatingExecutorService(threadpool, spanManager);
        spanManager.clear();
    }

    @After
    public void tearDown() {
        spanManager.clear();
    }

    @AfterClass
    public static void shutdownThreadpool() {
        assertThat(threadpool.shutdownNow(), equalTo(Collections.<Runnable>emptyList()));
    }

    @Test
    public void testExecuteRunnable() throws ExecutionException, InterruptedException {
        CurrentSpanRunnable runnable = new CurrentSpanRunnable();
        ManagedSpan callerManagedSpan = spanManager.manage(mock(Span.class));
        try {

            // Execute runnable and wait for completion.
            FutureTask<Void> future = new FutureTask<Void>(runnable, null);
            subject.execute(future);
            future.get();

            assertThat("Current span in thread", runnable.span, is(sameInstance(callerManagedSpan.getSpan())));
        } finally {
            callerManagedSpan.release();
        }
    }

    @Test
    public void testSubmitRunnable() throws ExecutionException, InterruptedException {
        CurrentSpanRunnable runnable = new CurrentSpanRunnable();
        ManagedSpan callerManagedSpan = spanManager.manage(mock(Span.class));
        try {

            subject.submit(runnable).get(); // submit and block.
            assertThat("Current span in thread", runnable.span, is(sameInstance(callerManagedSpan.getSpan())));

        } finally {
            callerManagedSpan.release();
        }
    }

    @Test
    public void testSubmitCallable() throws ExecutionException, InterruptedException {
        Callable<Span> callable = new CurrentSpanCallable();
        ManagedSpan callerManagedSpan = spanManager.manage(mock(Span.class));
        try {

            Future<Span> threadSpan = subject.submit(callable);
            assertThat("Current span in thread", threadSpan.get(), is(sameInstance(callerManagedSpan.getSpan())));

        } finally {
            callerManagedSpan.release();
        }
    }

    @Test
    public void testInvokeAll() throws ExecutionException, InterruptedException {
        Collection<Callable<Span>> callables = Arrays.<Callable<Span>>asList(
                new CurrentSpanCallable(), new CurrentSpanCallable(), new CurrentSpanCallable());
        ManagedSpan callerManagedSpan = spanManager.manage(mock(Span.class));
        try {

            List<Future<Span>> futures = subject.invokeAll(callables);
            assertThat("Futures", futures, hasSize(equalTo(callables.size())));
            for (Future<Span> threadSpan : futures) {
                assertThat("Current span in thread", threadSpan.get(), is(sameInstance(callerManagedSpan.getSpan())));
            }

        } finally {
            callerManagedSpan.release();
        }
    }


    @Test
    public void testExecuteRunnableWithoutCurrentSpan() throws ExecutionException, InterruptedException {
        CurrentSpanRunnable runnable = new CurrentSpanRunnable();

        // Execute runnable and wait for completion.
        FutureTask<Void> future = new FutureTask<Void>(runnable, null);
        subject.execute(future);
        future.get();

        assertThat("Current span in thread", runnable.span, allOf(notNullValue(), instanceOf(NoopSpan.class)));
    }

    @Test
    public void testSubmitRunnableWithoutCurrentSpan() throws ExecutionException, InterruptedException {
        CurrentSpanRunnable runnable = new CurrentSpanRunnable();
        subject.submit(runnable).get(); // submit and block.
        assertThat("Current span in thread", runnable.span, allOf(notNullValue(), instanceOf(NoopSpan.class)));
    }

    @Test
    public void testSubmitCallableWithoutCurrentSpan() throws ExecutionException, InterruptedException {
        Future<Span> threadSpan = subject.submit(new CurrentSpanCallable());
        assertThat("Current span in thread", threadSpan.get(), allOf(notNullValue(), instanceOf(NoopSpan.class)));
    }

    static class CurrentSpanRunnable implements Runnable {
        Span span = null;

        @Override
        public void run() {
            span = spanManager.currentSpan();
        }
    }

    static class CurrentSpanCallable implements Callable<Span> {
        @Override
        public Span call() {
            return spanManager.currentSpan();
        }
    }

}
