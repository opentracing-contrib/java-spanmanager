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
package io.opentracing.contrib.spanmanager;

import io.opentracing.NoopSpan;
import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.SpanManager.ManagedSpan;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class DefaultSpanManagerTest {

    SpanManager manager;

    @Before
    public void prepareManager() {
        manager = DefaultSpanManager.getInstance();
        manager.clear();
    }

    @After
    public void resetManager() {
        manager.clear();
    }

    @Test
    public void testBasicStackBehaviour() {
        Span span1 = mock(Span.class);
        Span span2 = mock(Span.class);
        Span span3 = mock(Span.class);

        assertThat("empty stack", manager.current().getSpan(), is(nullValue()));

        ManagedSpan managed1 = manager.activate(span1);
        assertThat("pushed span1", manager.current().getSpan(), is(sameInstance(span1)));

        ManagedSpan managed2 = manager.activate(span2);
        assertThat("pushed span2", manager.current().getSpan(), is(sameInstance(span2)));

        ManagedSpan managed3 = manager.activate(span3);
        assertThat("pushed span3", manager.current().getSpan(), is(sameInstance(span3)));

        managed3.deactivate();
        assertThat("popped span3", manager.current().getSpan(), is(sameInstance(span2)));

        managed2.deactivate();
        assertThat("popped span2", manager.current().getSpan(), is(sameInstance(span1)));

        managed1.deactivate();
        assertThat("popped span1", manager.current().getSpan(), is(nullValue()));
    }

    @Test
    public void testMultipleReleases() {
        Span span1 = mock(Span.class);
        Span span2 = mock(Span.class);

        assertThat("empty stack", manager.current().getSpan(), is(nullValue()));

        ManagedSpan managed1 = manager.activate(span1);
        assertThat("pushed span1", manager.current().getSpan(), is(sameInstance(span1)));

        ManagedSpan managed2 = manager.activate(span2);
        assertThat("pushed span2", manager.current().getSpan(), is(sameInstance(span2)));

        managed2.deactivate();
        managed2.deactivate();
        assertThat("popped span2", manager.current().getSpan(), is(sameInstance(span1)));

        managed1.deactivate();
        managed2.deactivate();
        managed1.deactivate();
        managed2.deactivate();
        assertThat("popped span1", manager.current().getSpan(), is(nullValue()));
    }


    @Test
    public void testTemporaryNoSpan() {
        Span span1 = mock(Span.class);
        Span span2 = null;
        Span span3 = mock(Span.class);

        assertThat("empty stack", manager.current().getSpan(), is(nullValue()));

        ManagedSpan managed1 = manager.activate(span1);
        assertThat("pushed span1", manager.current().getSpan(), is(sameInstance(span1)));

        ManagedSpan managed2 = manager.activate(span2);
        assertThat("pushed span2", manager.current().getSpan(), is(nullValue()));

        ManagedSpan managed3 = manager.activate(span3);
        assertThat("pushed span3", manager.current().getSpan(), is(sameInstance(span3)));

        managed3.deactivate();
        assertThat("popped span3", manager.current().getSpan(), is(nullValue()));

        managed2.deactivate();
        assertThat("popped span2", manager.current().getSpan(), is(sameInstance(span1)));

        managed1.deactivate();
        assertThat("popped span1", manager.current().getSpan(), is(nullValue()));
    }

    @Test
    public void testOutOfOrderRelease() {
        Span span1 = mock(Span.class);
        Span span2 = mock(Span.class);
        Span span3 = mock(Span.class);

        assertThat("empty stack", manager.current().getSpan(), is(nullValue()));

        ManagedSpan managed1 = manager.activate(span1);
        assertThat("pushed span1", manager.current().getSpan(), is(sameInstance(span1)));

        ManagedSpan managed2 = manager.activate(span2);
        assertThat("pushed span2", manager.current().getSpan(), is(sameInstance(span2)));

        ManagedSpan managed3 = manager.activate(span3);
        assertThat("pushed span3", manager.current().getSpan(), is(sameInstance(span3)));

        // Pop2: Span1 -> Span2(X) -> Span3  :  currentSpan stays Span3
        managed2.deactivate();
        assertThat("released span2", manager.current().getSpan(), is(sameInstance(span3)));

        managed3.deactivate();
        assertThat("skipped span2 (already-released)", manager.current().getSpan(), is(sameInstance(span1)));

        managed1.deactivate();
        assertThat("popped span1", manager.current().getSpan(), is(nullValue()));
    }

    /**
     * <strong>Note:</strong> This is not a normal use-case!<br>
     * The {@link ManagedSpan} is intended to be created and used in the scope of a try-with-resources block
     * (so within the scope of a single thread).
     * <p>
     * This test is merely here to guarantee predictable behaviour when it happens.
     */
    @Test
    public void testReleaseFromOtherThreads() throws InterruptedException {
        Span span1 = mock(Span.class);
        Span span2 = mock(Span.class);
        Span span3 = mock(Span.class);

        assertThat("empty stack", manager.current().getSpan(), is(nullValue()));

        ManagedSpan managed1 = manager.activate(span1);
        assertThat("pushed span1", manager.current().getSpan(), is(sameInstance(span1)));

        final ManagedSpan managed2 = manager.activate(span2);
        assertThat("pushed span2", manager.current().getSpan(), is(sameInstance(span2)));

        ManagedSpan managed3 = manager.activate(span3);
        assertThat("pushed span3", manager.current().getSpan(), is(sameInstance(span3)));

        // Schedule 10 threads to release managed2
        Thread[] releasers = new Thread[10];
        for (int i = 0; i < releasers.length; i++) {
            releasers[i] = new Thread() {
                @Override
                public void run() {
                    managed2.deactivate();
                }
            };
        }

        // Schedule managed2.release() 10x
        for (int i = 0; i < releasers.length; i++) releasers[i].start();

        managed3.deactivate();

        // Wait for managed2.releases
        for (int i = 0; i < releasers.length; i++) releasers[i].join();

        assertThat("popped span2+3", manager.current().getSpan(), is(sameInstance(span1)));

        managed1.deactivate();
        assertThat("popped span1", manager.current().getSpan(), is(nullValue()));
    }

    @Test
    public void testExplicitRelease() {
        Span span1 = mock(Span.class);

        assertThat("empty stack", manager.current().getSpan(), is(nullValue()));

        manager.activate(span1);
        assertThat("pushed span1", manager.current().getSpan(), is(sameInstance(span1)));

        manager.current().deactivate();
        assertThat("popped span1", manager.current().getSpan(), is(nullValue()));

        // Try releasing - should not have any effect
        manager.current().deactivate();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLegacyCurrentSpan() {
        assertThat("empty stack", manager.currentSpan(), is(instanceOf(NoopSpan.class)));

        Span span1 = mock(Span.class);
        manager.activate(span1);
        assertThat("pushed span1", manager.currentSpan(), is(sameInstance(span1)));
    }

}
