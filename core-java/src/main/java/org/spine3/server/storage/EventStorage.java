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

package org.spine3.server.storage;

import com.google.protobuf.Timestamp;
import org.spine3.base.EventRecord;

import java.io.Closeable;
import java.util.Iterator;

import static org.spine3.util.Events.toEventStoreRecord;

/**
 * A storage used by {@link org.spine3.server.EventStore} for keeping event data.
 *
 * @author Alexander Yevsyukov
 */
public abstract class EventStorage implements Closeable {

    @SuppressWarnings("TypeMayBeWeakened")
    public void store(EventRecord record) {
        final EventStoreRecord storeRecord = toEventStoreRecord(record);
        write(storeRecord);
    }

    /**
     * Returns iterator through all the event records in the storage sorted by timestamp.
     *
     * @return iterator instance
     */
    public abstract Iterator<EventRecord> allEvents();

    /**
     * Returns iterator through event records since the passed moment of time.
     *
     * @param timestamp a moment from which return event records
     * @return iterator of event records since the passed time
     */
    public abstract Iterator<EventRecord> since(Timestamp timestamp);

    /**
     * Writes record into the storage.
     *
     * @param record the record to write
     * @throws java.lang.NullPointerException if record is null
     */
    protected abstract void write(EventStoreRecord record);

}
