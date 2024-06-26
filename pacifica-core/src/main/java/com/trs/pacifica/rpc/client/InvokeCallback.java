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

import com.trs.pacifica.util.thread.ThreadUtil;

import java.util.concurrent.Executor;

public interface InvokeCallback {

    /**
     * callback on rpc client receive response
     * @param result
     * @param err
     */
    void complete(final Object result, final Throwable err);

    default Executor executor() {
        return ThreadUtil._CALLBACK_EXECUTOR;
    }
}
