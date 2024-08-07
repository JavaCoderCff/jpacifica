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

import com.trs.pacifica.rpc.service.ReplicaServiceManager;
import com.trs.pacifica.error.PacificaException;
import com.trs.pacifica.model.ReplicaId;
import com.trs.pacifica.proto.RpcRequest;
import com.trs.pacifica.rpc.service.ReplicaService;
import com.trs.pacifica.rpc.RpcRequestFinished;
import com.trs.pacifica.util.RpcUtil;

public class GetFileRequestHandler extends InternalRpcRequestHandler<RpcRequest.GetFileRequest, RpcRequest.GetFileResponse> {


    public GetFileRequestHandler(ReplicaServiceManager replicaManager) {
        super(replicaManager, RpcRequest.GetFileResponse.getDefaultInstance());
    }


    @Override
    public String interest() {
        return RpcRequest.GetFileRequest.class.getName();
    }

    @Override
    protected RpcRequest.GetFileResponse asyncHandleRequest(ReplicaService replicaService, RpcRequest.GetFileRequest request, RpcRequestFinished<RpcRequest.GetFileResponse> rpcRequestFinished) throws PacificaException {
        return replicaService.handleGetFileRequest(request, rpcRequestFinished);
    }

    @Override
    protected ReplicaId parseReplicaId(RpcRequest.GetFileRequest request) {
        return RpcUtil.toReplicaId(request.getTargetId());
    }
}