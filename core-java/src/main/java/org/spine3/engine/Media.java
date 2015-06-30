/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.engine;

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
public interface Media {

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
