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
package com.trs.pacifica;

import com.trs.pacifica.async.Callback;
import com.trs.pacifica.error.LogEntryCorruptedException;
import com.trs.pacifica.error.PacificaException;
import com.trs.pacifica.model.LogEntry;
import com.trs.pacifica.model.LogId;
import com.trs.pacifica.snapshot.SnapshotMeta;

import java.util.List;

public interface LogManager {


    /**
     * @param logEntries
     * @param callback
     */
    public void appendLogEntries(List<LogEntry> logEntries, AppendLogEntriesCallback callback);

    default public void appendLogEntry(final LogEntry logEntry, final AppendLogEntriesCallback callback) {
        appendLogEntries(List.of(logEntry), callback);
    }

    /**
     * get LogEntry at log index
     *
     * @param logIndex
     * @return
     * @throws LogEntryCorruptedException if the LogEntry corrupted
     */
    public LogEntry getLogEntryAt(final long logIndex) throws PacificaException;

    /**
     * @param logIndex
     * @return term of LogId at logIndex.
     * @return 0 if logIndex <= 0 or not found LogEntry.
     */
    public long getLogTermAt(final long logIndex);

    /**
     * get LogId at commit point
     *
     * @return
     */
    public LogId getCommittedPoint();

    /**
     * get first log id
     *
     * @return LogId(0, 0) if nothing
     */
    public LogId getFirstLogId();

    /**
     * get last log id
     *
     * @return LogId(0, 0) if nothing
     */
    public LogId getLastLogId();


    /**
     * if expectedLastLogIndex <= lastLogIndexOnDisk will run newLogCallback and return -1L
     *
     * @param expectedLastLogIndex
     * @param newLogWaiter
     * @return waiterId for remove the waiter.
     * @throws NullPointerException if newLogCallback is null
     */
    public long waitNewLog(final long expectedLastLogIndex, final NewLogWaiter newLogWaiter);


    /**
     * called when snapshot be saved or load
     * @param snapshotLogIndex log index at snapshot
     * @param snapshotLogTerm  log term at snapshot
     */
    public void onSnapshot(final long snapshotLogIndex, final long snapshotLogTerm);


    public void onCommitted(final long committedLogIndex, final long committedLogTerm);

    /**
     * @param waiterId
     * @return true if success
     */
    public boolean removeWaiter(final long waiterId);

    public static abstract class NewLogWaiter implements Callback {

        private long newLogIndex = 0L;

        public long getNewLogIndex() {
            return newLogIndex;
        }

        public void setNewLogIndex(long newLogIndex) {
            this.newLogIndex = newLogIndex;
        }
    }


    public static abstract class AppendLogEntriesCallback implements Callback {

        private long firstLogIndex;

        private int appendCount;

        public long getFirstLogIndex() {
            return firstLogIndex;
        }

        public void setFirstLogIndex(long firstLogIndex) {
            this.firstLogIndex = firstLogIndex;
        }

        public int getAppendCount() {
            return appendCount;
        }

        public void setAppendCount(int appendCount) {
            this.appendCount = appendCount;
        }
    }

}

