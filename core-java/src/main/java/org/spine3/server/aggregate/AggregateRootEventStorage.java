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
package org.spine3.server.aggregate;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.EventRecord;
import org.spine3.server.MessageJournal;

import java.util.List;

//TODO:2015-09-16:alexander.yevsyukov: Have generic parameter for aggregate root ID type.
/**
 * Stores and loads the events for a class of aggregate roots.
 *
 * @author Mikhail Mikhaylov
 */
public class AggregateRootEventStorage {

    private final MessageJournal<EventRecord> storage;

    public AggregateRootEventStorage(MessageJournal<EventRecord> storage) {
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
     * Loads all events by AggregateRoot Id.
     *
     * @param aggregateRootId the id of aggregateRoot
     * @return list of events
     */
    public List<EventRecord> loadAll(Message aggregateRootId) {
        List<EventRecord> result = storage.load(aggregateRootId);
        return result;
    }

    /**
     * Loads all events by AggregateRoot Id from given timestamp.
     *
     * @param aggregateRootId the id of aggregateRoot
     * @param from            timestamp to load events from
     * @return list of events
     */
    public List<EventRecord> loadSince(Message aggregateRootId, Timestamp from) {
        List<EventRecord> result = storage.loadSince(aggregateRootId, from);
        return result;
    }

}
