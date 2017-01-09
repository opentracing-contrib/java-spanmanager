package io.opentracing.contrib.spanmanager.concurrent;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.SpanManager;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class SpanPropagatingExecutorServiceTest {

    ExecutorService mockExecutorService;
    SpanManager mockSpanManager;
    Span mockSpan;

    SpanPropagatingExecutorService service;

    @Before
    public void setUp() {
        mockExecutorService = mock(ExecutorService.class);
        mockSpanManager = mock(SpanManager.class);
        mockSpan = mock(Span.class);
        when(mockSpanManager.currentSpan()).thenReturn(mockSpan);

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

        verify(mockSpanManager).currentSpan(); // current span must be propagated
        verify(mockExecutorService).execute(any(RunnableWithManagedSpan.class)); // into RunnableWithManagedSpan
        verifyNoMoreInteractions(runnable);
    }

    @Test
    public void testSubmitRunnable() {
        Runnable runnable = mock(Runnable.class);
        Future future = mock(Future.class);
        when(mockExecutorService.submit(any(Runnable.class))).thenReturn(future);

        assertThat(service.submit(runnable), is(sameInstance(future)));

        verify(mockSpanManager).currentSpan(); // current span must be propagated.
        verify(mockExecutorService).submit(any(RunnableWithManagedSpan.class)); // into RunnableWithManagedSpan
        verifyNoMoreInteractions(runnable, future);
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
