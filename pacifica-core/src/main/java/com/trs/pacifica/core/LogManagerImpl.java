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

import com.trs.pacifica.*;
import com.trs.pacifica.async.Callback;
import com.trs.pacifica.async.Finished;
import com.trs.pacifica.async.thread.SingleThreadExecutor;
import com.trs.pacifica.error.LogEntryCorruptedException;
import com.trs.pacifica.error.PacificaException;
import com.trs.pacifica.error.PacificaErrorCode;
import com.trs.pacifica.log.codec.LogEntryCodecFactory;
import com.trs.pacifica.log.codec.LogEntryDecoder;
import com.trs.pacifica.log.codec.LogEntryEncoder;
import com.trs.pacifica.model.LogEntry;
import com.trs.pacifica.model.LogId;
import com.trs.pacifica.util.thread.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * impl of LogManager
 * TODO LogEntry Cache Support
 */
public class LogManagerImpl implements LogManager, LifeCycle<LogManagerImpl.Option> {

    static final Logger LOGGER = LoggerFactory.getLogger(LogManagerImpl.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final ReplicaImpl replica;
    private Option option;
    private SingleThreadExecutor executor;
    private LogStorage logStorage;
    private StateMachineCaller stateMachineCaller;
    private LogStorageFactory logStorageFactory;
    private LogEntryCodecFactory logEntryCodecFactory;


    /**
     * first log index
     */
    private volatile long firstLogIndex = 0L;

    /**
     * last log index
     */
    private volatile long lastLogIndex = 0L;

    /**
     * last log id on disk
     */
    private LogId lastLogIdOnDisk = new LogId(0, 0);

    /**
     * last log id on snapshot save
     */
    private LogId lastSnapshotLogId = new LogId(0, 0);


    public LogManagerImpl(ReplicaImpl replica) {
        this.replica = replica;
    }

    @Override
    public void init(LogManagerImpl.Option option) throws PacificaException {
        this.writeLock.lock();
        try {
            this.option = Objects.requireNonNull(option);
            this.executor = Objects.requireNonNull(option.getLogManagerExecutor());
            this.logStorageFactory = Objects.requireNonNull(option.getLogStorageFactory(), "log storage factory");
            this.logEntryCodecFactory = Objects.requireNonNull(option.getLogEntryCodecFactory(), "logEntryCodecFactory");
            this.stateMachineCaller = Objects.requireNonNull(option.getStateMachineCaller(), "stateMachineCaller");
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void startup() throws PacificaException {
        this.writeLock.lock();
        try {
            if (this.logStorage == null) {
                final LogEntryEncoder logEntryEncoder = Objects.requireNonNull(logEntryCodecFactory.getLogEntryEncoder());
                final LogEntryDecoder logEntryDecoder = Objects.requireNonNull(logEntryCodecFactory.getLogEntryDecoder());
                this.logStorage = Objects.requireNonNull(logStorageFactory.newLogStorage(option.getLogStoragePath(), logEntryEncoder, logEntryDecoder), "log storage");
            }
            this.logStorage.open();
            final LogId firstLogId = this.logStorage.getFirstLogId();
            if (firstLogId != null) {
                this.firstLogIndex = firstLogId.getIndex();
            }
            final LogId lastLogId = this.logStorage.getLastLogId();
            if (lastLogId != null) {
                this.lastLogIdOnDisk = lastLogId;
                this.lastLogIndex = lastLogId.getIndex();
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} is started. {}", this, this.replica.getReplicaId());
            }
        } finally {
            this.writeLock.unlock();
        }

    }

    @Override
    public void shutdown() throws PacificaException {
        this.writeLock.lock();
        try {
            this.logStorage.close();
            this.firstLogIndex = 0L;
            this.lastLogIndex = 0L;
            this.lastLogIdOnDisk = new LogId(0, 0);
            this.lastSnapshotLogId = new LogId(0, 0);
        } finally {
            this.writeLock.unlock();
        }
    }

    /**
     * @param logEntries
     * @param callback
     * @return
     */
    private boolean checkAndResolveConflict(final List<LogEntry> logEntries, Callback callback) {
        assert logEntries.isEmpty() == false;
        final LogEntry firstLogEntry = logEntries.get(0);
        final boolean primary = firstLogEntry.getLogId().getIndex() == 0;
        if (primary) {
            // fill LogEntry -> LogId.index
            logEntries.forEach(logEntry -> {
                logEntry.getLogId().setIndex(++this.lastLogIndex);
            });
        } else {
            //first
            final long firstLogIndex = firstLogEntry.getLogId().getIndex();
            if (firstLogIndex > this.lastLogIndex + 1) {
                //discontinuous
                final String errorMsg = String.format("there's gap between first_index=%d and last_log_index=%d", firstLogIndex, this.lastLogIndex);
                ThreadUtil.runCallback(callback, Finished.failure(new PacificaException(PacificaErrorCode.CONFLICT_LOG, errorMsg)));
                return false;
            }
            final LogEntry lastLogEntry = logEntries.get(logEntries.size() - 1);
            final long committedLogIndex = this.stateMachineCaller.getCommittedLogIndex();
            if (lastLogEntry.getLogId().getIndex() <= committedLogIndex) {
                //has been committed
                final String errorMsg = String.format("The received logEntries(last_log_index=%d) have all been committed(commit_point=%d), and we keep them unchanged",
                        lastLogEntry.getLogId().getIndex(), committedLogIndex);
                ThreadUtil.runCallback(callback, Finished.failure(new PacificaException(PacificaErrorCode.CONFLICT_LOG, errorMsg)));
                return false;
            }

            if (firstLogEntry.getLogId().getIndex() != this.lastLogIndex + 1) {
                // resolve conflict
                // 1、find conflict LogEntry
                int conflictingIndex = 0;
                for (; conflictingIndex < logEntries.size(); conflictingIndex++) {
                    final LogEntry curLogEntry = logEntries.get(conflictingIndex);
                    if (getLogTermAt(curLogEntry.getLogId().getIndex()) != curLogEntry.getLogId().getTerm()) {
                        break;
                    }
                }
                if (conflictingIndex != logEntries.size()) {
                    final LogEntry conflictingLogEntry = logEntries.get(conflictingIndex);
                    if (conflictingLogEntry.getLogId().getIndex() <= this.lastLogIndex) {
                        //has conflict log: truncate suffix
                        truncateSuffix(conflictingLogEntry.getLogId().getIndex() - 1, null);
                    }
                }
                if (conflictingIndex > 0) {
                    //remove duplication
                    logEntries.subList(0, conflictingIndex).clear();
                }
            }
            if (logEntries.isEmpty()) {
                final String errorMsg = String.format("The received logEntries([first_log_index=%d, last_log_index=%d]) have all been append(last_log_index=%d), and we keep them unchanged", firstLogIndex, lastLogEntry.getLogId().getIndex(), this.lastLogIndex);
                ThreadUtil.runCallback(callback, Finished.failure(new PacificaException(PacificaErrorCode.CONFLICT_LOG, errorMsg)));
                return false;
            }
            this.lastLogIndex = lastLogEntry.getLogId().getIndex();
        }
        return true;
    }

    @Override
    public void appendLogEntries(final List<LogEntry> logEntries, AppendLogEntriesCallback callback) {
        if (logEntries == null || logEntries.isEmpty()) {
            ThreadUtil.runCallback(callback, Finished.failure(new IllegalArgumentException("empty logEntries")));
            return;
        }
        this.writeLock.lock();
        try {
            // check resolve conflict
            if (!checkAndResolveConflict(logEntries, callback)) {
                return;
            }
            assert !logEntries.isEmpty();
            // fill LogEntry -> checksum
            final boolean enableLogEntryChecksum = this.option.getReplicaOption().isEnableLogEntryChecksum();
            if (enableLogEntryChecksum) {
                logEntries.forEach(logEntry -> {
                    if (logEntry.hasChecksum() == false) {
                        logEntry.setChecksum(logEntry.checksum());
                    }
                });
            }
            final LogEntry firstLogEntry = logEntries.get(0);
            callback.setFirstLogIndex(firstLogEntry.getLogId().getIndex());
            callback.setAppendCount(logEntries.size());
            // store log entries
            storeLogEntries(logEntries, callback);
        } finally {
            this.writeLock.unlock();
        }
    }


    @Override
    public LogEntry getLogEntryAt(final long logIndex) {
        if (logIndex < this.firstLogIndex || logIndex > this.lastLogIndex) {
            return null;
        }
        this.readLock.lock();
        try {
            final LogEntry logEntry = this.logStorage.getLogEntry(logIndex);
            if (logEntry != null && this.option.getReplicaOption().isEnableLogEntryChecksum() && logEntry.isCorrupted()) {
                throw new LogEntryCorruptedException(String.format("corrupted LogEntry(%s), actual_checksum=%d", logEntry, logEntry.checksum()));
            }
            return logEntry;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public long getLogTermAt(long logIndex) {
        if (logIndex <= 0) {
            return 0L;
        }
        this.readLock.lock();
        try {
            if (this.lastSnapshotLogId.getIndex() == logIndex) {
                return this.lastSnapshotLogId.getTerm();
            }
            if (logIndex < this.firstLogIndex || logIndex > this.lastLogIdOnDisk.getIndex()) {
                return 0L;
            }
            return getLogTermFromStorage(logIndex);
        } finally {
            this.readLock.unlock();
        }
    }

    private long getLogTermFromStorage(final long logIndex) {
        final LogId logId = this.logStorage.getLogIdAt(logIndex);
        if (logId != null) {
            return logId.getTerm();
        }
        return 0L;
    }

    @Override
    public LogId getFirstLogId() {

        return null;
    }

    @Override
    public LogId getLastLogId() {
        return null;
    }


    @Override
    public void onSnapshot(final long snapshotLogIndex, final long snapshotLogTerm) {
        this.writeLock.lock();
        try {
            if (snapshotLogIndex <= this.lastSnapshotLogId.getIndex()) {
                LOGGER.warn("{} on snapshot, snapshot_log_index={}, but last snapshot_log_index={}", this.replica.getReplicaId(), snapshotLogIndex, this.lastSnapshotLogId.getIndex());
                return;
            }
            final long localSnapshotLogTerm = getLogTermAt(snapshotLogIndex);
            this.lastSnapshotLogId = new LogId(snapshotLogIndex, snapshotLogTerm);
            if (localSnapshotLogTerm == 0) {
                //out of range LogEntry Queue
                truncatePrefix(snapshotLogIndex + 1, null);
            } else if (localSnapshotLogTerm == snapshotLogTerm) {
                // truncate prefix and reserved
                final long firstIndexKept = Math.max(0, snapshotLogIndex - this.option.getReplicaOption().getSnapshotLogIndexReserved());
                if (firstIndexKept > 0) {
                    truncatePrefix(firstIndexKept, null);
                }
            } else {
                // if in range log queue but conflicting op log at snapshotLogIndex, we need rest
                if (!unsafeReset(snapshotLogIndex + 1)) {
                    LOGGER.warn("{} Reset log manager failed, nextLogIndex={}.", this.replica.getReplicaId(), snapshotLogIndex + 1);
                }
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    private boolean unsafeReset(final long nextLogIndex) {
        // TODO
        this.firstLogIndex = nextLogIndex;
        this.lastLogIndex = nextLogIndex - 1;
        return this.logStorage.reset(nextLogIndex);
    }

    void storeLogEntries(final List<LogEntry> logEntries, final AppendLogEntriesCallback callback) {
        this.executor.execute(new StoreLogEntriesEvent(logEntries, callback));
    }

    /**
     * @param logEntries
     */
    private void doStoreLogEntries(final List<LogEntry> logEntries) {
        assert !logEntries.isEmpty();
        assert logEntries.get(0).getLogId().getIndex() == this.lastLogIdOnDisk.getIndex() + 1;

        this.writeLock.lock();
        try {
            final int count = this.logStorage.appendLogEntries(logEntries);
            this.lastLogIdOnDisk = logEntries.get(count - 1).getLogId().copy();
            if (count != logEntries.size()) {
                LOGGER.error("{} failed to append log entries, but real_append_count={}, expect_append_count={}",
                        this.replica.getReplicaId(), count, logEntries.size());
                reportError(new PacificaException(PacificaErrorCode.INTERNAL, String.format(
                        "Failed to append log entries, but real_append_count=%d, expect_append_count=%d", count, logEntries.size())));
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    private void reportError(PacificaException error) {

        this.stateMachineCaller.onError(error);

    }

    @Override
    public String toString() {
        final StringBuilder infoBuilder = new StringBuilder(this.getClass().getSimpleName() + "[");
        infoBuilder.append("firstLogIndex=").append(this.firstLogIndex).append(",");
        infoBuilder.append("lastLogIndex=").append(this.lastLogIndex).append(",");
        infoBuilder.append("lastLogIdOnDisk=").append(this.lastLogIdOnDisk).append(",");
        infoBuilder.append("lastSnapshotLogId=").append(this.lastSnapshotLogId).append(",");
        infoBuilder.append("]");
        return infoBuilder.toString();
    }

    void truncateSuffix(final long lastIndexKept, final Callback callback) {
        this.executor.execute(new TruncateSuffixEvent(lastIndexKept));
    }

    private void doTruncateSuffix(final long lastIndexKept) {
        this.writeLock.lock();
        try {
            if (lastIndexKept > this.lastLogIndex) {
                return;
            }
            if (lastIndexKept < this.stateMachineCaller.getCommittedLogIndex()) {
                return;
            }
            final LogId lastLogId = this.logStorage.truncateSuffix(lastIndexKept);
            if (lastLogId != null && lastLogId.getIndex() > this.lastLogIndex) {
                this.lastLogIdOnDisk = lastLogId.copy();
                this.lastLogIndex = this.lastLogIdOnDisk.getIndex();
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    void truncatePrefix(final long firstIndexKept, final Callback callback) {

        this.executor.execute(new TruncatePrefixEvent(firstIndexKept));
    }

    private void doTruncatePrefix(final long firstLogIndexKept) {
        this.writeLock.lock();
        try {
            if (firstLogIndexKept < this.firstLogIndex) {
                return;
            }
            if (firstLogIndexKept > this.lastLogIndex) {
                // out of range log queue
                this.lastLogIndex = firstLogIndexKept;
            }
            final LogId firstLogId = this.logStorage.truncatePrefix(firstLogIndexKept);
            if (firstLogId != null && firstLogId.getIndex() > this.firstLogIndex) {
                this.firstLogIndex = firstLogId.getIndex();
            }
        } finally {
            this.writeLock.unlock();
        }

    }

    class StoreLogEntriesEvent implements Runnable {

        private final List<LogEntry> logEntries;

        private final AppendLogEntriesCallback callback;

        StoreLogEntriesEvent(List<LogEntry> logEntries, AppendLogEntriesCallback callback) {
            this.logEntries = logEntries;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                doStoreLogEntries(logEntries);
                ThreadUtil.runCallback(callback, Finished._OK);
            } catch (Throwable e) {
                ThreadUtil.runCallback(callback, Finished.failure(e));
            }
        }
    }

    class TruncateSuffixEvent implements Runnable {

        private final long lastIndexKept;

        TruncateSuffixEvent(final long lastIndexKept) {
            this.lastIndexKept = lastIndexKept;
        }

        @Override
        public void run() {
            doTruncateSuffix(lastIndexKept);
        }
    }

    class TruncatePrefixEvent implements Runnable {
        private final long firstIndexKept;

        TruncatePrefixEvent(final long firstIndexKept) {
            this.firstIndexKept = firstIndexKept;
        }


        @Override
        public void run() {
            doTruncatePrefix(firstIndexKept);
        }
    }

    public static final class Option {

        ReplicaOption replicaOption;

        String logStoragePath;

        SingleThreadExecutor logManagerExecutor;

        LogEntryCodecFactory logEntryCodecFactory;
        LogStorageFactory logStorageFactory;

        StateMachineCaller stateMachineCaller;

        public SingleThreadExecutor getLogManagerExecutor() {
            return logManagerExecutor;
        }

        public void setLogManagerExecutor(SingleThreadExecutor logManagerExecutor) {
            this.logManagerExecutor = logManagerExecutor;
        }

        public LogStorageFactory getLogStorageFactory() {
            return logStorageFactory;
        }

        public void setLogStorageFactory(LogStorageFactory logStorageFactory) {
            this.logStorageFactory = logStorageFactory;
        }

        public String getLogStoragePath() {
            return logStoragePath;
        }

        public void setLogStoragePath(String logStoragePath) {
            this.logStoragePath = logStoragePath;
        }

        public ReplicaOption getReplicaOption() {
            return replicaOption;
        }

        public void setReplicaOption(ReplicaOption replicaOption) {
            this.replicaOption = replicaOption;
        }

        public StateMachineCaller getStateMachineCaller() {
            return stateMachineCaller;
        }

        public void setStateMachineCaller(StateMachineCaller stateMachineCaller) {
            this.stateMachineCaller = stateMachineCaller;
        }

        public LogEntryCodecFactory getLogEntryCodecFactory() {
            return logEntryCodecFactory;
        }

        public void setLogEntryCodecFactory(LogEntryCodecFactory logEntryCodecFactory) {
            this.logEntryCodecFactory = logEntryCodecFactory;
        }
    }


}
