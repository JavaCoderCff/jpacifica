/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trs.pacifica.rpc.client;

import com.trs.pacifica.async.DirectExecutor;

import java.util.concurrent.Executor;

public class ExecutorInvokeCallback extends FilterInvokeCallback {
    static final Executor DEFAULT_EXECUTOR = new DirectExecutor();

    private final Executor executor;

    ExecutorInvokeCallback(InvokeCallback delegate, Executor executor) {
        super(delegate);
        this.executor = executor == null ? DEFAULT_EXECUTOR : executor;
    }

    ExecutorInvokeCallback(InvokeCallback delegate) {
        super(delegate);
        this.executor = delegate.executor() == null ? DEFAULT_EXECUTOR : delegate.executor();
    }


    @Override
    public void complete(Object result, Throwable err) {
        this.executor.execute(() -> {
            ExecutorInvokeCallback.super.complete(result, err);
        });
    }

    @Override
    public Executor executor() {
        return executor;
    }


    public static ExecutorInvokeCallback wrap(InvokeCallback invokeCallback) {
        return new ExecutorInvokeCallback(invokeCallback);
    }


    public static ExecutorInvokeCallback wrap(InvokeCallback invokeCallback, Executor executor) {
        return new ExecutorInvokeCallback(invokeCallback, executor);
    }

}