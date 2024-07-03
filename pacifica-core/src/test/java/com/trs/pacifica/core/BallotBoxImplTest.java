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

import com.trs.pacifica.LogManager;
import com.trs.pacifica.Replica;
import com.trs.pacifica.StateMachineCaller;
import com.trs.pacifica.model.ReplicaGroup;
import com.trs.pacifica.model.ReplicaId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class BallotBoxImplTest {

    private BallotBoxImpl ballotBox;

    private StateMachineCaller stateMachineCaller;

    @BeforeEach
    public void setup() {
        this.stateMachineCaller = Mockito.mock(StateMachineCaller.class);
        Mockito.doReturn(1003L).when(this.stateMachineCaller).getLastCommittedLogIndex();
        Replica replica = Mockito.mock(Replica.class);
        Mockito.doReturn(new ReplicaId("group", "node")).when(replica).getReplicaId();
        this.ballotBox = new BallotBoxImpl(replica);

        BallotBoxImpl.Option option = new BallotBoxImpl.Option();
        option.setFsmCaller(stateMachineCaller);
        ballotBox.init(option);
        ballotBox.startup();
    }


    @AfterEach
    public void shutdown() {
        this.ballotBox.shutdown();
    }

    @Test
    public void testInitiateBallotAndBallotBy() {
        Assertions.assertEquals(1004, this.ballotBox.getPendingLogIndex());
        ReplicaId primary = new ReplicaId("group", "primary");
        ReplicaId secondary1 = new ReplicaId("group", "secondary1");
        ReplicaId secondary2 = new ReplicaId("group", "secondary2");
        ReplicaId secondary3 = new ReplicaId("group", "secondary3");
        List<ReplicaId> secondaries = new ArrayList<>();
        secondaries.add(secondary1);
        secondaries.add(secondary2);
        secondaries.add(secondary3);
        ReplicaGroup replicaGroup = Mockito.mock(ReplicaGroup.class);
        Mockito.doReturn(primary).when(replicaGroup).getPrimary();
        Mockito.doReturn(secondaries).when(replicaGroup).listSecondary();
        Mockito.doReturn(1L).when(replicaGroup).getVersion();
        Mockito.doReturn(1L).when(replicaGroup).getPrimaryTerm();
        Mockito.doReturn("group").when(replicaGroup).getGroupName();

        Assertions.assertTrue(this.ballotBox.initiateBallot(replicaGroup));
        List<BallotBoxImpl.Ballot> ballots = this.ballotBox.getBallotQueue();
        Assertions.assertEquals(1, ballots.size());
        BallotBoxImpl.Ballot ballot = ballots.get(0);
        Assertions.assertEquals(4, ballot.getQuorum());

        Assertions.assertTrue(this.ballotBox.ballotBy(primary, 1004, 1004));
        Assertions.assertEquals(3, ballot.getQuorum());
        Assertions.assertEquals(1004, this.ballotBox.getPendingLogIndex());
        Assertions.assertTrue(this.ballotBox.ballotBy(secondary1, 1004, 1004));
        Assertions.assertEquals(2, ballot.getQuorum());
        Assertions.assertEquals(1004, this.ballotBox.getPendingLogIndex());
        Assertions.assertTrue(this.ballotBox.ballotBy(secondary2, 1004, 1004));
        Assertions.assertEquals(1, ballot.getQuorum());
        Assertions.assertEquals(1004, this.ballotBox.getPendingLogIndex());
        Assertions.assertTrue(this.ballotBox.ballotBy(secondary3, 1004, 1004));
        Assertions.assertEquals(0, ballot.getQuorum());
        Assertions.assertEquals(1005, this.ballotBox.getPendingLogIndex());
        Assertions.assertEquals(1004, this.ballotBox.getLastCommittedLogIndex());

        Mockito.verify(this.stateMachineCaller).commitAt(1004);

    }

}
