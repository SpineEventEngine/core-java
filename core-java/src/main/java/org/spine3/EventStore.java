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
package org.spine3;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.EventRecord;
import org.spine3.base.Snapshot;
import org.spine3.engine.Storage;

import java.util.List;

/**
 * Stores and loads the events and snapshots.
 *
 * @author Mikhail Mikhaylov
 */
public class EventStore {

    private static final Class ENTITY_CLASS = EventRecord.class;

    private final Storage storage;

    public EventStore(Storage storage) {
        this.storage = storage;
    }

    /**
     * Stores the event record.
     *
     * @param record event record to store
     */
    public void store(EventRecord record) {
        storage.store(record);
    }

    /**
     * Stores the aggregate root's snapshot.
     *
     * @param aggregateId the id of aggregate root
     * @param snapshot    the snapshot of aggregate root
     */
    public void storeSnapshot(Message aggregateId, Snapshot snapshot) {
        storage.storeSingleton(aggregateId, snapshot);
    }

    /**
     * Loads aggregateRoot's snapshot by given aggregateRootId.
     *
     * @param aggregateRootId the id of aggregate root
     * @return the snapshot of aggregate root
     */
    public Snapshot getLastSnapshot(Message aggregateRootId) {
        return storage.readSingleton(aggregateRootId);
    }

    /**
     * Loads all events by AggregateRoot Id.
     *
     * @param aggregateRootId the id of aggregateRoot
     * @return list of events
     */
    public List<EventRecord> getAllEvents(Message aggregateRootId) {
        List<EventRecord> result = storage.query(ENTITY_CLASS, aggregateRootId);
        return result;
    }

    /**
     * Loads all events by AggregateRoot Id from given timestamp.
     *
     * @param aggregateRootId the id of aggregateRoot
     * @param from            timestamp to load events from
     * @return list of events
     */
    public List<EventRecord> getEvents(Message aggregateRootId, Timestamp from) {
        List<EventRecord> result = storage.query(ENTITY_CLASS, aggregateRootId, from);
        return result;
    }

    /**
     * Loads all events from given timestamp.
     *
     * @param from timestamp to load events from
     * @return list of events
     */
    public List<EventRecord> getEvents(Timestamp from) {
        List<EventRecord> result = storage.query(ENTITY_CLASS, from);
        return result;
    }

    /**
     * Returns the event records for the given aggregate root
     * that has version greater than passed.
     *
     * @param sinceVersion the version of the aggregate root used as lower threshold for the result list
     * @return list of the event records
     */
    public List<EventRecord> getEvents(Message aggregateRootId, int sinceVersion) {
        List<EventRecord> result = storage.query(ENTITY_CLASS, aggregateRootId, sinceVersion);
        return result;
    }

}
