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

package com.trs.pacifica.sender;

import com.trs.pacifica.async.Callback;
import com.trs.pacifica.model.ReplicaId;

import java.util.concurrent.TimeUnit;

/**
 * Only called by Primary.
 *
 */
public interface SenderGroup {

    default public void addSenderTo(ReplicaId replicaId) {
        addSenderTo(replicaId, SenderType.Secondary, false);
    }

    default public void addSenderTo(ReplicaId replicaId, SenderType senderType) {
        addSenderTo(replicaId, senderType, false);
    }



    /**
     * add Sender
     * @param replicaId
     * @param senderType  to see {@link SenderType}
     * @param checkConnection  check connect
     */
    public void addSenderTo(ReplicaId replicaId, SenderType senderType, boolean checkConnection);


    /**
     * Whether the specified Replica is alive
     * @param replicaId
     * @return true if the Replica is alive
     */
    public boolean isAlive(ReplicaId replicaId);


    default public boolean waitCaughtUp(final ReplicaId replicaId, final Sender.OnCaughtUp onCaughtUp, long timeout, TimeUnit unit) {
        return waitCaughtUp(replicaId, onCaughtUp, unit.toMillis(timeout));
    }

    public boolean waitCaughtUp(final ReplicaId replicaId, final Sender.OnCaughtUp onCaughtUp, final long timeoutMs);


    /**
     *
     * @param logIndex
     * @return
     */
    public boolean continueAppendLogEntry(final long logIndex);


    public Sender removeSender(ReplicaId replicaId);



}
