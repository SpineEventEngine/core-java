/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.spine3.sample.store.filesystem;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.base.Snapshot;

import java.util.List;

/**
 * Defines the low level data interface of the storage
 * that is used to read/write events, commands, snapshots, etc.
 *
 * @author Mikhail Melnik
 */
public interface Storage {

    /**
     * Returns the event records for the given aggregate root.
     *
     * @param aggregateId the id of the aggregate
     * @return list of the event records
     */
    List<EventRecord> readEvents(Message aggregateId);

    /**
     * Returns the command requests for the given aggregate root.
     *
     * @param aggregateId the id of the aggregate
     * @return list of the command requests
     */
    List<CommandRequest> readCommands(Message aggregateId);

    /**
     * Returns the event records for the given aggregate root
     * that has version greater than passed.
     *
     * @param sinceVersion the version of the aggregate root used as lower threshold for the result list
     * @return list of the event records
     */
    List<EventRecord> readEvents(int sinceVersion);

    /**
     * Returns all event records with timestamp greater than passed one.
     *
     * @param from the timestamp used as the lower threshold for the result list
     * @return list of the event records
     */
    List<EventRecord> readEvents(Timestamp from);

    /**
     * Returns all commands requests with timestamp greater than passed one.
     *
     * @param from the timestamp used as the lower threshold for the result list
     * @return list of the command requests
     */
    List<CommandRequest> readCommands(Timestamp from);

    /**
     * Returns all event records with timestamp greater than passed one.
     *
     * @param from the timestamp used as the lower threshold for the result list
     * @param to   the timestamp used as the upper threshold for the result list
     * @return list of the event records
     */
    List<EventRecord> readEvents(Timestamp from, Timestamp to);

    /**
     * Returns all commands requests with timestamp greater than passed one.
     *
     * @param from the timestamp used as the lower threshold for the result list
     * @param to   the timestamp used as the upper threshold for the result list
     * @return list of the command requests
     */
    List<CommandRequest> readCommands(Timestamp from, Timestamp to);

    /**
     * Returns list of the all stored event records.
     *
     * @return list of the event records
     */
    List<EventRecord> readAllEvents();

    /**
     * Returns list of the all stored command requests.
     *
     * @return list of the command requests
     */
    List<CommandRequest> readAllCommands();

    /**
     * Returns the last snapshot for the given aggregate root.
     *
     * @return the {@link Snapshot} object
     */
    Snapshot readLastSnapshot(Message aggregateId);

    /**
     * Writes the passed event record to storage.
     *
     * @param eventRecord the event record to store
     */
    void writeEvent(EventRecord eventRecord);

    /**
     * Writes the passed command request to storage.
     *
     * @param commandRequest the command request to store
     */
    void writeCommand(CommandRequest commandRequest);

    /**
     * Writes the snapshot of the given aggregate root to storage.
     *
     * @param aggregateId the aggregate root id
     * @param snapshot    the snapshot to store
     */
    void writeSnapshot(Message aggregateId, Snapshot snapshot);

}
