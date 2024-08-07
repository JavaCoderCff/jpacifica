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

package com.trs.pacifica.core.fsm;

import com.trs.pacifica.Replica;
import com.trs.pacifica.async.Callback;
import com.trs.pacifica.model.Operation;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * TODO optimize ByteBuffer, or BytesArray??
 */
public interface OperationIterator extends Iterator<ByteBuffer> {


    /**
     * Return the data whose content is the same as what was passed to {@link Replica#apply } in the primary replica.
     *
     * @return log data
     */
    public ByteBuffer getLogData();

    /**
     * get current index of op log
     *
     * @return log index
     */
    public long getLogIndex();

    /**
     * get current term of op log
     *
     * @return term
     */
    public long getLogTerm();


    /**
     * get current callback of {@link Operation#getOnFinish()}}, to see {@link Replica#apply(Operation)}
     *
     * @return Callback it is nullable
     */
    public Callback getCallback();


    /**
     * Break the current op in the iterator and roll back
     *
     * @param throwable catch error of StateMachine by user
     */
    public void breakAndRollback(Throwable throwable);
}
