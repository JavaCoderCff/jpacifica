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

package com.trs.pacifica.rpc.node;

import com.trs.pacifica.spi.SPI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SPI
public class DefaultEndpointManager implements EndpointManager {

    private final Map<String, Endpoint> nodeIdToEndpointMap = new ConcurrentHashMap<>();

    @Override
    public Endpoint getEndpoint(String nodeId) {
        return nodeIdToEndpointMap.get(nodeId);
    }

    @Override
    public void registerEndpoint(String nodeId, Endpoint endpoint) {
        this.nodeIdToEndpointMap.put(nodeId, endpoint);
    }


}
