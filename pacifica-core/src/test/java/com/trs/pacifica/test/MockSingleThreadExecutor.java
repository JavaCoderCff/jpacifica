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

package com.trs.pacifica.test;

import com.trs.pacifica.async.thread.SingleThreadExecutor;
import com.trs.pacifica.util.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MockSingleThreadExecutor implements SingleThreadExecutor {

    private Executor executor = Executors.newFixedThreadPool (1, new NamedThreadFactory("jpacifica-test-single-thread"));

    @Override
    public boolean shutdownGracefully() {
        return true;
    }

    @Override
    public boolean shutdownGracefully(long timeout, TimeUnit unit) {
        return true;
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }
}
