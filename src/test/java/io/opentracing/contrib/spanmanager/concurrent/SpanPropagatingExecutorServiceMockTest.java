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

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.SpanManager;
import io.opentracing.contrib.spanmanager.SpanManager.ManagedSpan;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class SpanPropagatingExecutorServiceMockTest {

    ExecutorService mockExecutorService;
    SpanManager mockSpanManager;
    Span mockSpan;
    ManagedSpan mockManagedSpan;

    SpanPropagatingExecutorService service;

    @Before
    public void setUp() {
        mockExecutorService = mock(ExecutorService.class);
        mockSpanManager = mock(SpanManager.class);
        mockSpan = mock(Span.class);
        mockManagedSpan = mock(ManagedSpan.class);
        when(mockManagedSpan.getSpan()).thenReturn(mockSpan);
        when(mockSpanManager.current()).thenReturn(mockManagedSpan);

        service = new SpanPropagatingExecutorService(mockExecutorService, mockSpanManager);
    }

    @After
    public void verifyMocks() {
        verifyNoMoreInteractions(mockExecutorService, mockSpanManager, mockSpan);
    }

    @Test
    public void testExecuteRunnable() {
        Runnable runnable = mock(Runnable.class);

        service.execute(runnable);

        verify(mockSpanManager).current(); // current span must be propagated
        verify(mockExecutorService).execute(any(RunnableWithManagedSpan.class)); // into RunnableWithManagedSpan
    }

    @Test
    public void testSubmitRunnable() {
        Runnable runnable = mock(Runnable.class);
        Future future = mock(Future.class);
        when(mockExecutorService.submit(any(Runnable.class))).thenReturn(future);

        assertThat(service.submit(runnable), is(sameInstance(future)));

        verify(mockSpanManager).current(); // current span must be propagated
        verify(mockExecutorService).submit(any(RunnableWithManagedSpan.class)); // into RunnableWithManagedSpan
    }

    @Test
    public void testSubmitCallable() {
        Callable callable = mock(Callable.class);
        Future future = mock(Future.class);
        when(mockExecutorService.submit(any(Callable.class))).thenReturn(future);

        assertThat(service.submit(callable), is(sameInstance(future)));

        verify(mockSpanManager).current(); // current span must be propagated
        verify(mockExecutorService).submit(any(CallableWithManagedSpan.class)); // into CallableWithManagedSpan
    }

    @Test
    public void testInvokeAll() throws InterruptedException {
        Collection<Callable<Object>> callables = Arrays.<Callable<Object>>asList(mock(Callable.class), mock(Callable.class));
        List<Future<Object>> futures = mock(List.class);
        when(mockExecutorService.invokeAll((Collection<Callable<Object>>) anyCollection())).thenReturn(futures);

        assertThat(service.invokeAll(callables), is(sameInstance(futures)));

        verify(mockSpanManager, times(1)).current(); // current span must be obtained once
        verify(mockExecutorService).invokeAll(anyCollection());
    }

    @Test
    public void testConstructorWithoutDelegate() {
        try {
            new SpanPropagatingExecutorService(null, mock(SpanManager.class));
            fail("Exception with message expected.");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage(), is(Matchers.notNullValue()));
        }
    }

    @Test
    public void testConstructorWithoutSpanManager() {
        try {
            new SpanPropagatingExecutorService(mock(ExecutorService.class), null);
            fail("Exception with message expected.");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage(), is(Matchers.notNullValue()));
        }
    }

}
