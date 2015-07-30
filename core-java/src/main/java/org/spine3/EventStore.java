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
import org.spine3.storage.StorageWithTimelineAndVersion;

import java.util.List;

/**
 * Stores and loads the events.
 *
 * @author Mikhail Mikhaylov
 */
public class EventStore {

    private final StorageWithTimelineAndVersion<EventRecord> storage;

    public EventStore(StorageWithTimelineAndVersion<EventRecord> storage) {
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
     * Loads all events from given timestamp.
     *
     * @param from timestamp to load events from
     * @param <ID> aggregate id type
     * @return list of events
     */
    public <ID extends Message> List<EventRecord> getEvents(Timestamp from) {
        List<EventRecord> result = storage.read(from);
        return result;
    }

}
