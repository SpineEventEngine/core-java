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
package org.spine3.server;

import com.google.protobuf.Timestamp;
import org.spine3.base.EventRecord;
import org.spine3.server.storage.EventStorage;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * A store of all events in a bounded context.
 *
 * @author Mikhail Mikhaylov
 */
public class EventStore implements Closeable {

    private final EventStorage storage;

    /**
     * Creates a new instance running on the passed storage.
     *
     * @param storage the underlying storage for the store.
     */
    public EventStore(EventStorage storage) {
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
     * Returns an iterator through all the events in the history sorted by timestamp.
     *
     * @return iterator instance
     */
    public Iterator<EventRecord> allEvents() {
        return storage.allEvents();
    }

    /**
     * Returns an iterator over all event records since the passed time.
     *
     * @param timestamp a point in time from which the iterator would go
     * @return iterator instance
     */
    public Iterator<EventRecord> eventsSince(Timestamp timestamp) {
        return storage.since(timestamp);
    }

    /**
     * Closes the underlying storage.
     *
     * @throws IOException if the attempt to close the storage throws an exception
     */
    @Override
    public void close() throws IOException {
        storage.close();
    }
}
