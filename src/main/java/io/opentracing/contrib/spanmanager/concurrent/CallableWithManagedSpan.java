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

import java.util.concurrent.Callable;

/**
 * {@link Callable} wrapper that will execute with a {@link SpanManager#manage(Span) managed span}
 * specified from the scheduling thread.
 *
 * @see SpanManager
 */
final class CallableWithManagedSpan<T> implements Callable<T> {

    private final Callable<T> delegate;
    private final SpanManager spanManager;
    private final Span spanToManage;

    CallableWithManagedSpan(Callable<T> callable, SpanManager spanManager, Span spanToManage) {
        if (callable == null) throw new NullPointerException("Callable is <null>.");
        if (spanManager == null) throw new NullPointerException("Span manager is <null>.");
        this.delegate = callable;
        this.spanManager = spanManager;
        this.spanToManage = spanToManage;
    }

    /**
     * Performs the delegate call with the specified managed span.
     *
     * @return The result from the original call.
     * @throws Exception if the original call threw an exception.
     */
    public T call() throws Exception {
        SpanManager.ManagedSpan managedSpan = spanManager.manage(spanToManage);
        try {
            return delegate.call();
        } finally {
            managedSpan.release();
        }
    }

}
