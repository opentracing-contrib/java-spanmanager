package io.opentracing.contrib.spanmanager;

import io.opentracing.NoopSpan;
import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.SpanManager.ManagedSpan;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class DefaultSpanManagerTest {

    SpanManager manager;

    @Before
    public void resetManager() {
        manager = DefaultSpanManager.getInstance();
        manager.clear();
    }

    @Test
    public void testBasicStackBehaviour() {
        Span span1 = mock(Span.class);
        Span span2 = mock(Span.class);
        Span span3 = mock(Span.class);

        assertThat("empty stack", manager.currentSpan(), is(instanceOf(NoopSpan.class)));

        ManagedSpan managed1 = manager.manage(span1);
        assertThat("pushed span1", manager.currentSpan(), is(sameInstance(span1)));

        ManagedSpan managed2 = manager.manage(span2);
        assertThat("pushed span2", manager.currentSpan(), is(sameInstance(span2)));

        ManagedSpan managed3 = manager.manage(span3);
        assertThat("pushed span3", manager.currentSpan(), is(sameInstance(span3)));

        managed3.release();
        assertThat("popped span3", manager.currentSpan(), is(sameInstance(span2)));

        managed2.release();
        assertThat("popped span2", manager.currentSpan(), is(sameInstance(span1)));

        managed1.release();
        assertThat("popped span1", manager.currentSpan(), is(instanceOf(NoopSpan.class)));
    }

    @Test
    public void testMultipleReleases() {
        Span span1 = mock(Span.class);
        Span span2 = mock(Span.class);

        assertThat("empty stack", manager.currentSpan(), is(instanceOf(NoopSpan.class)));

        ManagedSpan managed1 = manager.manage(span1);
        assertThat("pushed span1", manager.currentSpan(), is(sameInstance(span1)));

        ManagedSpan managed2 = manager.manage(span2);
        assertThat("pushed span2", manager.currentSpan(), is(sameInstance(span2)));

        managed2.release();
        managed2.release();
        assertThat("popped span2", manager.currentSpan(), is(sameInstance(span1)));

        managed1.release();
        managed2.release();
        managed1.release();
        managed2.release();
        assertThat("popped span1", manager.currentSpan(), is(instanceOf(NoopSpan.class)));
    }


    @Test
    public void testTemporaryNoSpan() {
        Span span1 = mock(Span.class);
        Span span2 = null;
        Span span3 = mock(Span.class);

        assertThat("empty stack", manager.currentSpan(), is(instanceOf(NoopSpan.class)));

        ManagedSpan managed1 = manager.manage(span1);
        assertThat("pushed span1", manager.currentSpan(), is(sameInstance(span1)));

        ManagedSpan managed2 = manager.manage(span2);
        assertThat("pushed span2", manager.currentSpan(), is(instanceOf(NoopSpan.class)));

        ManagedSpan managed3 = manager.manage(span3);
        assertThat("pushed span3", manager.currentSpan(), is(sameInstance(span3)));

        managed3.release();
        assertThat("popped span3", manager.currentSpan(), is(instanceOf(NoopSpan.class)));

        managed2.release();
        assertThat("popped span2", manager.currentSpan(), is(sameInstance(span1)));

        managed1.release();
        assertThat("popped span1", manager.currentSpan(), is(instanceOf(NoopSpan.class)));
    }

    @Test
    public void testOutOfOrderRelease() {
        Span span1 = mock(Span.class);
        Span span2 = mock(Span.class);
        Span span3 = mock(Span.class);

        assertThat("empty stack", manager.currentSpan(), is(instanceOf(NoopSpan.class)));

        ManagedSpan managed1 = manager.manage(span1);
        assertThat("pushed span1", manager.currentSpan(), is(sameInstance(span1)));

        ManagedSpan managed2 = manager.manage(span2);
        assertThat("pushed span2", manager.currentSpan(), is(sameInstance(span2)));

        ManagedSpan managed3 = manager.manage(span3);
        assertThat("pushed span3", manager.currentSpan(), is(sameInstance(span3)));

        // Pop2: Span1 -> Span2(X) -> Span3  :  current span stays Span3
        managed2.release();
        assertThat("released span2", manager.currentSpan(), is(sameInstance(span3)));

        managed3.release();
        assertThat("skipped span2 (already-released)", manager.currentSpan(), is(sameInstance(span1)));

        managed1.release();
        assertThat("popped span1", manager.currentSpan(), is(instanceOf(NoopSpan.class)));
    }

}
