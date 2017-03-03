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

import io.opentracing.contrib.spanmanager.concurrent.SpanPropagatingExecutorService;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class DefaultSpanManagerMemoryTest {

    void recurseThreads(final ExecutorService executor) {
        executor.submit(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1);
                    recurseThreads(executor);
                } catch (InterruptedException ie) {
                    throw new IllegalStateException(ie.getMessage(), ie);
                }
            }
        });
    }

    /**
     * This example provided by @pauldraper shows how memory can leak using the default span manager.
     * <p>
     * If your business is recursively scheduling new threads, then stacking context from the parent thread may not be
     * a smart decision.
     * <p>
     * I (@sjoerdtalsma) let this test run for 10 minutes and the java process peaked to about 110MB of memory.
     * Stack wind-down at the end of the test was very noticable in this case though!
     */
    @Test
    @Ignore // This is a long-running unit test that shouldn't be enabled in the build by default.
    public void testMemoryLeak() throws InterruptedException {
        ExecutorService executor = new SpanPropagatingExecutorService(newSingleThreadExecutor(), DefaultSpanManager.getInstance());
        recurseThreads(executor);

        // Let it run for a minute.
        Thread.sleep(60 * 1000);
        executor.shutdownNow();
    }

}
