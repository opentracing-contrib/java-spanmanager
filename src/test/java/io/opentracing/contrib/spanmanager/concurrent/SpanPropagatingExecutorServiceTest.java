/**
 * Copyright 2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
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
import static org.junit.Assert.assertNull;
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

        assertNull("Current span in thread", runnable.span);
    }

    @Test
    public void testSubmitRunnableWithoutCurrentSpan() throws ExecutionException, InterruptedException {
        CurrentSpanRunnable runnable = new CurrentSpanRunnable();
        subject.submit(runnable).get(); // submit and block.
        assertNull("Current span in thread", runnable.span);
    }

    @Test
    public void testSubmitCallableWithoutCurrentSpan() throws ExecutionException, InterruptedException {
        Future<Span> threadSpan = subject.submit(new CurrentSpanCallable());
        assertNull("Current span in thread", threadSpan.get());
    }

    static class CurrentSpanRunnable implements Runnable {
        Span span = null;

        @Override
        public void run() {
            span = spanManager.current().getSpan();
        }
    }

    static class CurrentSpanCallable implements Callable<Span> {
        @Override
        public Span call() {
            return spanManager.current().getSpan();
        }
    }

}
