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

package com.trs.pacifica.rpc.internal;

import com.google.protobuf.Message;
import com.trs.pacifica.rpc.service.ReplicaServiceManager;
import com.trs.pacifica.error.PacificaErrorCode;
import com.trs.pacifica.error.PacificaException;
import com.trs.pacifica.model.ReplicaId;
import com.trs.pacifica.rpc.service.ReplicaService;
import com.trs.pacifica.rpc.RpcRequestHandler;
import com.trs.pacifica.rpc.RpcRequestFinished;

public abstract class InternalRpcRequestHandler<Req extends Message, Rep extends Message> extends RpcRequestHandler<Req, Rep> {

    private final ReplicaServiceManager replicaManager;

    protected InternalRpcRequestHandler(ReplicaServiceManager replicaManager, Rep defaultMessage) {
        super(defaultMessage);
        this.replicaManager = replicaManager;
    }

    @Override
    protected Rep asyncHandleRequest(Req request, RpcRequestFinished<Rep> rpcRequestFinished) throws PacificaException {
        final ReplicaId replicaId = parseReplicaId(request);
        if (replicaId == null) {
            throw new PacificaException(PacificaErrorCode.INTERNAL, "can not parse replica id");
        }
        final ReplicaService replicaService = replicaManager.getReplicaService(replicaId);
        if (replicaService == null) {
            throw new PacificaException(PacificaErrorCode.INTERNAL, String.format("can not found replica service, replica_id=%s", replicaId));
        }
        return asyncHandleRequest(replicaService, request, rpcRequestFinished);
    }

    protected abstract Rep asyncHandleRequest(ReplicaService replicaService, Req request, RpcRequestFinished<Rep> rpcRequestFinished) throws PacificaException;


    protected abstract ReplicaId parseReplicaId(Req request);

}
