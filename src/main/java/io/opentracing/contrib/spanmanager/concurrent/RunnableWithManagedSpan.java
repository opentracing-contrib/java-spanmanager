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

/**
 * {@link Runnable} wrapper that will execute with a {@link SpanManager#activate(Span) managed active span}
 * specified from the scheduling thread.
 *
 * @see SpanManager
 */
final class RunnableWithManagedSpan implements Runnable {

    private final Runnable delegate;
    private final SpanManager spanManager;
    private final Span spanToManage;

    RunnableWithManagedSpan(Runnable runnable, SpanManager spanManager, Span spanToManage) {
        if (runnable == null) throw new NullPointerException("Runnable is <null>.");
        if (spanManager == null) throw new NullPointerException("Span manager is <null>.");
        this.delegate = runnable;
        this.spanManager = spanManager;
        this.spanToManage = spanToManage;
    }

    /**
     * Performs the runnable action with the specified managed span.
     */
    @Override
    public void run() {
        SpanManager.ManagedSpan managedSpan = spanManager.activate(spanToManage);
        try {
            delegate.run();
        } finally {
            managedSpan.deactivate();
        }
    }

}
