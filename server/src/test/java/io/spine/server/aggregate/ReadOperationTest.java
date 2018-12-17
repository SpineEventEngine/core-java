/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.aggregate;

import io.spine.core.BoundedContextNames;
import io.spine.core.Event;
import io.spine.server.aggregate.given.ReadOperationTestEnv.TestAggregate;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.aggregate.given.ReadOperationTestEnv.events;
import static io.spine.server.aggregate.given.ReadOperationTestEnv.snapshot;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReadOperation should")
class ReadOperationTest {

    private static final String ID = "test-aggregate-ID";
    private static final StorageFactory storageFactory =
            InMemoryStorageFactory.newInstance(BoundedContextNames.assumingTests(), false);

    private AggregateStorage<String> storage;

    @BeforeEach
    void setUp() {
        storage = storageFactory.createAggregateStorage(TestAggregate.class);
    }

    @Test
    @DisplayName("read all events if there are a few")
    void readAll() {
        int eventCount = 10;
        fillEvents(eventCount);
        AggregateReadRequest<String> request = new AggregateReadRequest<>(ID, 100);
        ReadOperation<String> operation = new ReadOperation<>(storage, request);
        Optional<AggregateStateRecord> record = operation.perform();
        assertTrue(record.isPresent());
        AggregateStateRecord stateRecord = record.get();

        assertFalse(stateRecord.hasSnapshot());
        List<Event> events = stateRecord.getEventList();
        assertThat(events).hasSize(eventCount);
    }

    @Test
    @DisplayName("read snapshot if present")
    void readSnapshot() {
        fillEventsWithSnapshot(5);
        AggregateReadRequest<String> request = new AggregateReadRequest<>(ID, 100);
        ReadOperation<String> operation = new ReadOperation<>(storage, request);
        Optional<AggregateStateRecord> record = operation.perform();
        assertTrue(record.isPresent());
        AggregateStateRecord stateRecord = record.get();

        assertTrue(stateRecord.hasSnapshot());
    }

    @Test
    @DisplayName("strip all events which occurred before snapshot")
    void trimBeforeSnapshot() {
        fillEvents(3);
        fillEventsWithSnapshot(5);
        int expectedEventCount = 7;
        fillEvents(expectedEventCount);

        AggregateReadRequest<String> request = new AggregateReadRequest<>(ID, 100);
        ReadOperation<String> operation = new ReadOperation<>(storage, request);
        Optional<AggregateStateRecord> record = operation.perform();
        assertTrue(record.isPresent());
        AggregateStateRecord stateRecord = record.get();

        assertTrue(stateRecord.hasSnapshot());
        assertThat(stateRecord.getEventList()).hasSize(expectedEventCount);
    }

    private void fillEvents(int count) {
        List<Event> events = events(count);
        storage.write(ID, AggregateStateRecord
                .newBuilder()
                .addAllEvent(events)
                .build());
    }

    private void fillEventsWithSnapshot(int count) {
        List<Event> events = events(count);
        Snapshot snapshot = snapshot();
        storage.write(ID, AggregateStateRecord
                .newBuilder()
                .addAllEvent(events)
                .setSnapshot(snapshot)
                .build());
    }
}
