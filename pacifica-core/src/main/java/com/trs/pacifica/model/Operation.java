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

package com.trs.pacifica.model;

import com.trs.pacifica.async.Callback;

import java.nio.ByteBuffer;

public class Operation {


    private ByteBuffer logData = LogEntry.EMPTY_DATA;

    private Callback onFinish = null;

    public ByteBuffer getLogData() {
        return logData;
    }

    public void setLogData(ByteBuffer logData) {
        this.logData = logData;
    }

    public Callback getOnFinish() {
        return onFinish;
    }

    public void setOnFinish(Callback onFinish) {
        this.onFinish = onFinish;
    }
}
