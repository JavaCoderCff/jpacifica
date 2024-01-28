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

package com.trs.pacifica.core;

import com.trs.pacifica.ConfigurationClient;

import java.util.concurrent.TimeUnit;

public class ReplicaOption {

    static final int DEFAULT_GRACE_PERIOD_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(60);
    static final int MIN_GRACE_PERIOD_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(10);
    static final int MAX_GRACE_PERIOD_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(600);


    private ConfigurationClient configurationClient;


    /**
     * Grace period. If the time limit of the Secondary detection is exceeded,
     * the Primary is considered to be faulty, and the Primary change request is sent
     */
    private int gracePeriodTimeoutMs = DEFAULT_GRACE_PERIOD_TIMEOUT_MS; // default 60 s

    /**
     * lease period timeout ms= gracePeriodTimeoutMs * leasePeriodTimeoutRatio/100
     *
     */
    private int leasePeriodTimeoutRatio = 80;




}
