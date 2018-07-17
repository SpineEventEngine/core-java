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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Time;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.RejectionContext;
import io.spine.core.Version;
import io.spine.server.aggregate.given.StorageRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.storage.AbstractStorageTest;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.testdata.Sample;
import io.spine.testing.Tests;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Lists.transform;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.Versions.increment;
import static io.spine.core.Versions.zero;
import static io.spine.server.aggregate.given.StorageRecords.sequenceFor;
import static io.spine.testing.core.given.GivenEnrichment.withOneAttribute;
import static io.spine.testing.server.TestEventFactory.newInstance;
import static io.spine.time.Durations2.seconds;
import static io.spine.validate.Validate.isDefault;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names */,
        "unused" /* JUnit nested classes considered unused in abstract test class */})
public abstract class AggregateStorageTest
        extends AbstractStorageTest<ProjectId,
                                    AggregateStateRecord,
                                    AggregateReadRequest<ProjectId>,
                                    AggregateStorage<ProjectId>> {

    private static final Function<AggregateEventRecord, Event> TO_EVENT =
            record -> record != null ? record.getEvent() : null;

    private final ProjectId id = Sample.messageOfType(ProjectId.class);
    private final TestEventFactory eventFactory = newInstance(AggregateStorageTest.class);
    private AggregateStorage<ProjectId> storage;

    private static Snapshot newSnapshot(Timestamp time) {
        return Snapshot.newBuilder()
                       .setState(Any.getDefaultInstance())
                       .setTimestamp(time)
                       .build();
    }

    private static EventId newEventId() {
        return EventId.newBuilder()
                      .setValue(newUuid())
                      .build();
    }

    @Override
    @BeforeEach
    public void setUpAbstractStorageTest() {
        super.setUpAbstractStorageTest();
        storage = getStorage();
    }

    @Override
    protected AggregateStateRecord newStorageRecord() {
        List<AggregateEventRecord> records = sequenceFor(id);
        List<Event> expectedEvents = records.stream()
                                            .map(TO_EVENT)
                                            .collect(toList());
        AggregateStateRecord record = AggregateStateRecord
                .newBuilder()
                .addAllEvent(expectedEvents)
                .build();
        return record;
    }

    @Override
    protected ProjectId newId() {
        return Sample.messageOfType(ProjectId.class);
    }

    @Override
    protected AggregateReadRequest<ProjectId> newReadRequest(ProjectId id) {
        return new AggregateReadRequest<>(id, MAX_VALUE);
    }

    @Override
    protected Class<? extends TestAggregate> getTestEntityClass() {
        return TestAggregate.class;
    }

    /**
     * Creates the storage for the specified ID and aggregate class.
     *
     * <p>The created storage should be closed manually.
     *
     * @param idClass        the class of aggregate ID
     * @param aggregateClass the aggregate class
     * @param <I>            the type of aggregate IDs
     * @return a new storage instance
     */
    protected abstract <I> AggregateStorage<I>
    newStorage(Class<? extends I> idClass, Class<? extends Aggregate<I, ?, ?>> aggregateClass);

    @Nested
    @DisplayName("being empty, return")
    class BeingEmptyReturn {

        @Test
        @DisplayName("iterator over empty collection on reading history")
        void emptyHistory() {
            Iterator<AggregateEventRecord> iterator = historyBackward();

            assertFalse(iterator.hasNext());
        }

        @Test
        @DisplayName("absent AggregateStateRecord on reading record")
        void absentRecord() {
            AggregateReadRequest<ProjectId> readRequest = newReadRequest(id);
            Optional<AggregateStateRecord> record = storage.read(readRequest);

            assertFalse(record.isPresent());
        }
    }

    @Nested
    @DisplayName("not accept null")
    class NotAcceptNull {

        @Test
        @DisplayName("request ID when reading history")
        void idForReadHistory() {
            assertThrows(NullPointerException.class,
                         () -> storage.historyBackward(Tests.nullRef()));
        }

        @Test
        @DisplayName("event for writing")
        void event() {
            assertThrows(NullPointerException.class,
                         () -> storage.writeEvent(id, Tests.nullRef()));
        }

        @Test
        @DisplayName("event ID for writing")
        void eventId() {
            assertThrows(NullPointerException.class,
                         () -> storage.writeEvent(Tests.nullRef(), Event.getDefaultInstance()));
        }

        @Test
        @DisplayName("snapshot for writing")
        void snapshot() {
            assertThrows(NullPointerException.class,
                         () -> storage.writeSnapshot(id, Tests.nullRef()));
        }

        @Test
        @DisplayName("snapshot ID for writing")
        void snapshotId() {
            assertThrows(NullPointerException.class,
                         () -> storage.writeSnapshot(Tests.nullRef(),
                                                     Snapshot.getDefaultInstance()));
        }
    }

    @Nested
    @DisplayName("write and read one event by ID of type")
    class WriteAndReadEvent {

        @Test
        @DisplayName("Message")
        void byMessageId() {
            writeAndReadEventTest(id, storage);
        }

        @Test
        @DisplayName("String")
        void byStringId() {
            AggregateStorage<String> storage = newStorage(String.class,
                                                          TestAggregateWithIdString.class);
            String id = newUuid();
            writeAndReadEventTest(id, storage);
        }

        @Test
        @DisplayName("Long")
        void byLongId() {
            AggregateStorage<Long> storage = newStorage(Long.class,
                                                        TestAggregateWithIdLong.class);
            long id = 10L;
            writeAndReadEventTest(id, storage);
        }

        @Test
        @DisplayName("Integer")
        void byIntegerId() {
            AggregateStorage<Integer> storage = newStorage(Integer.class,
                                                           TestAggregateWithIdInteger.class);
            int id = 10;
            writeAndReadEventTest(id, storage);
        }
    }

    @Test
    @DisplayName("write and read one record")
    void writeAndReadRecord() {
        AggregateEventRecord expected = StorageRecord.create(getCurrentTime());

        storage.writeRecord(id, expected);

        Iterator<AggregateEventRecord> iterator = historyBackward();
        assertTrue(iterator.hasNext());
        AggregateEventRecord actual = iterator.next();
        assertEquals(expected, actual);
        assertFalse(iterator.hasNext());
    }

    // Ignore this test because several records can be stored by an aggregate ID.
    @SuppressWarnings({
            "NoopMethodInAbstractClass",
            "RefusedBequest",
            "MethodDoesntCallSuperMethod"
    })
    @Override
    @Test
    @DisplayName("re-write record if writing by the same ID")
    protected void rewriteRecord() {
    }

    @Nested
    @DisplayName("read history of aggregate with status")
    class ReadHistory {

        @Test
        @DisplayName("`archived`")
        void ofArchived() {
            LifecycleFlags archivedRecordFlags = LifecycleFlags.newBuilder()
                                                               .setArchived(true)
                                                               .build();
            storage.writeLifecycleFlags(id, archivedRecordFlags);
            writeAndReadEventTest(id, storage);
        }

        @Test
        @DisplayName("`deleted`")
        void ofDeleted() {
            LifecycleFlags deletedRecordFlags = LifecycleFlags.newBuilder()
                                                              .setDeleted(true)
                                                              .build();
            storage.writeLifecycleFlags(id, deletedRecordFlags);
            writeAndReadEventTest(id, storage);
        }
    }

    @Nested
    @DisplayName("index aggregate with status")
    class IndexAggregate {

        @Test
        @DisplayName("`archived`")
        void archived() {
            LifecycleFlags archivedRecordFlags = LifecycleFlags.newBuilder()
                                                               .setArchived(true)
                                                               .build();
            storage.writeRecord(id, StorageRecord.create(getCurrentTime()));
            storage.writeLifecycleFlags(id, archivedRecordFlags);
            assertTrue(storage.index()
                              .hasNext());
        }

        @Test
        @DisplayName("`deleted`")
        void deleted() {
            LifecycleFlags deletedRecordFlags = LifecycleFlags.newBuilder()
                                                              .setDeleted(true)
                                                              .build();
            storage.writeRecord(id, StorageRecord.create(getCurrentTime()));
            storage.writeLifecycleFlags(id, deletedRecordFlags);
            assertTrue(storage.index()
                              .hasNext());
        }
    }

    @Nested
    @DisplayName("read records with status")
    class ReadRecords {

        @Test
        @DisplayName("`archived`")
        void archived() {
            readRecordsWithLifecycle(LifecycleFlags.newBuilder()
                                                   .setArchived(true)
                                                   .build());
        }

        @Test
        @DisplayName("`deleted`")
        void deleted() {
            readRecordsWithLifecycle(LifecycleFlags.newBuilder()
                                                   .setDeleted(true)
                                                   .build());
        }

        @Test
        @DisplayName("`archived` and `deleted`")
        void archivedAndDeleted() {
            readRecordsWithLifecycle(LifecycleFlags.newBuilder()
                                                   .setArchived(true)
                                                   .setDeleted(true)
                                                   .build());
        }

        private void readRecordsWithLifecycle(LifecycleFlags flags) {
            AggregateStateRecord record = newStorageRecord();
            storage.write(id, record);
            storage.writeLifecycleFlags(id, flags);
            Optional<AggregateStateRecord> read = storage.read(newReadRequest(id));
            assertTrue(read.isPresent());
            AggregateStateRecord readRecord = read.get();
            assertEquals(record, readRecord);
        }
    }

    @Nested
    @DisplayName("write records and return them")
    class WriteRecordsAndReturn {

        @Test
        @DisplayName("sorted by timestamp descending")
        void sortedByTimestamp() {
            List<AggregateEventRecord> records = sequenceFor(id);

            writeAll(id, records);

            Iterator<AggregateEventRecord> iterator = historyBackward();
            List<AggregateEventRecord> actual = newArrayList(iterator);
            reverse(records); // expected records should be in a reverse order
            assertEquals(records, actual);
        }

        @Test
        @DisplayName("sorted by version descending")
        void sortedByVersion() {
            int eventsNumber = 5;
            List<AggregateEventRecord> records = newLinkedList();
            Timestamp timestamp = getCurrentTime();
            Version currentVersion = zero();
            for (int i = 0; i < eventsNumber; i++) {
                Project state = Project.getDefaultInstance();
                Event event = eventFactory.createEvent(state, currentVersion, timestamp);
                AggregateEventRecord record = StorageRecord.create(timestamp, event);
                records.add(record);
                currentVersion = increment(currentVersion);
            }
            writeAll(id, records);

            Iterator<AggregateEventRecord> iterator = historyBackward();
            List<AggregateEventRecord> actual = newArrayList(iterator);
            reverse(records); // expected records should be in a reverse order
            assertEquals(records, actual);
        }
    }

    @Test
    @DisplayName("sort by version rather than by timestamp")
    void sortByVersionFirstly() {
        Project state = Project.getDefaultInstance();
        Version minVersion = zero();
        Version maxVersion = increment(minVersion);
        Timestamp minTimestamp = Timestamps.MIN_VALUE;
        Timestamp maxTimestamp = Timestamps.MAX_VALUE;

        // The first event is an event, which is the oldest, i.e. with the minimal version.
        Event expectedFirst = eventFactory.createEvent(state, minVersion, maxTimestamp);
        Event expectedSecond = eventFactory.createEvent(state, maxVersion, minTimestamp);

        storage.writeEvent(id, expectedSecond);
        storage.writeEvent(id, expectedFirst);

        List<Event> events = storage.read(newReadRequest(id))
                                    .get()
                                    .getEventList();
        assertTrue(events.indexOf(expectedFirst) < events.indexOf(expectedSecond));
    }

    @Test
    @DisplayName("write and read snapshot")
    void writeAndReadSnapshot() {
        Snapshot expected = newSnapshot(getCurrentTime());

        storage.writeSnapshot(id, expected);

        Iterator<AggregateEventRecord> iterator = historyBackward();
        assertTrue(iterator.hasNext());
        AggregateEventRecord actual = iterator.next();
        assertEquals(expected, actual.getSnapshot());
        assertFalse(iterator.hasNext());
    }

    @Nested
    @DisplayName("write records and load history")
    class WriteRecordsAndLoadHistory {

        @Test
        @DisplayName("if there are no snapshots available")
        void withNoSnapshots() {
            testWriteRecordsAndLoadHistory(getCurrentTime());
        }

        @Test
        @DisplayName("till last snapshot available")
        void tillLastSnapshot() {
            Duration delta = seconds(10);
            Timestamp time1 = getCurrentTime();
            Timestamp time2 = add(time1, delta);
            Timestamp time3 = add(time2, delta);

            storage.writeRecord(id, StorageRecord.create(time1));
            storage.writeSnapshot(id, newSnapshot(time2));

            testWriteRecordsAndLoadHistory(time3);
        }
    }

    @Nested
    @DisplayName("properly read event count")
    class ReadEventCount {

        @Test
        @DisplayName("set to default zero value")
        void zeroByDefault() {
            assertEquals(0, storage.readEventCountAfterLastSnapshot(id));
        }

        @Test
        @DisplayName("set to specified value")
        void setToSpecifiedValue() {
            int expectedCount = 32;
            storage.writeEventCountAfterLastSnapshot(id, expectedCount);

            int actualCount = storage.readEventCountAfterLastSnapshot(id);

            assertEquals(expectedCount, actualCount);
        }

        @Test
        @DisplayName("updated to specified value")
        void updated() {
            int primaryValue = 16;
            storage.writeEventCountAfterLastSnapshot(id, primaryValue);
            int expectedValue = 32;
            storage.writeEventCountAfterLastSnapshot(id, expectedValue);

            int actualCount = storage.readEventCountAfterLastSnapshot(id);

            assertEquals(expectedValue, actualCount);
        }
    }

    @Test
    @DisplayName("continue reading history if snapshot was not found in first batch")
    void continueReadHistoryIfSnapshotNotFound() {
        Version currentVersion = zero();
        Snapshot snapshot = Snapshot.newBuilder()
                                    .setVersion(currentVersion)
                                    .build();
        storage.writeSnapshot(id, snapshot);

        int eventCountAfterSnapshot = 10;
        for (int i = 0; i < eventCountAfterSnapshot; i++) {
            currentVersion = increment(currentVersion);
            Project state = Project.getDefaultInstance();
            Event event = eventFactory.createEvent(state, currentVersion);
            storage.writeEvent(id, event);
        }

        int batchSize = 1;
        AggregateReadRequest<ProjectId> request = new AggregateReadRequest<>(id, batchSize);
        Optional<AggregateStateRecord> optionalStateRecord = storage.read(request);

        assertTrue(optionalStateRecord.isPresent());
        AggregateStateRecord stateRecord = optionalStateRecord.get();
        assertEquals(snapshot, stateRecord.getSnapshot());
        assertEquals(eventCountAfterSnapshot, stateRecord.getEventCount());
    }

    @Nested
    @DisplayName("not store enrichment")
    class NotStoreEnrichment {

        @Test
        @DisplayName("for EventContext")
        void forEventContext() {
            EventContext enrichedContext = EventContext.newBuilder()
                                                       .setEnrichment(withOneAttribute())
                                                       .build();
            Event event = Event.newBuilder()
                               .setId(newEventId())
                               .setContext(enrichedContext)
                               .setMessage(Any.getDefaultInstance())
                               .build();
            storage.writeEvent(id, event);
            EventContext loadedContext = storage.read(newReadRequest(id))
                                                .get()
                                                .getEvent(0)
                                                .getContext();
            assertTrue(isDefault(loadedContext.getEnrichment()));
        }

        @Test
        @DisplayName("for origin of RejectionContext type")
        void forRejectionContextOrigin() {
            RejectionContext origin = RejectionContext.newBuilder()
                                                      .setEnrichment(withOneAttribute())
                                                      .build();
            EventContext context = EventContext.newBuilder()
                                               .setRejectionContext(origin)
                                               .build();
            Event event = Event.newBuilder()
                               .setId(newEventId())
                               .setContext(context)
                               .setMessage(Any.getDefaultInstance())
                               .build();
            storage.writeEvent(id, event);
            RejectionContext loadedOrigin = storage.read(newReadRequest(id))
                                                   .get()
                                                   .getEvent(0)
                                                   .getContext()
                                                   .getRejectionContext();
            assertTrue(isDefault(loadedOrigin.getEnrichment()));
        }

        @Test
        @DisplayName("for origin of EventContext type")
        void forEventContextOrigin() {
            EventContext origin = EventContext.newBuilder()
                                              .setEnrichment(withOneAttribute())
                                              .build();
            EventContext context = EventContext.newBuilder()
                                               .setEventContext(origin)
                                               .build();
            Event event = Event.newBuilder()
                               .setId(newEventId())
                               .setContext(context)
                               .setMessage(Any.getDefaultInstance())
                               .build();
            storage.writeEvent(id, event);
            EventContext loadedOrigin = storage.read(newReadRequest(id))
                                               .get()
                                               .getEvent(0)
                                               .getContext()
                                               .getEventContext();
            assertTrue(isDefault(loadedOrigin.getEnrichment()));
        }
    }

    @Nested
    @DisplayName("being closed, not allow")
    class BeingClosedThrow {

        @Test
        @DisplayName("writing event count")
        void onWritingEventCount() {
            close(storage);

            assertThrows(IllegalStateException.class,
                         () -> storage.writeEventCountAfterLastSnapshot(id, 5));
        }

        @Test
        @DisplayName("reading event count")
        void onReadingEventCount() {
            close(storage);

            assertThrows(IllegalStateException.class,
                         () -> storage.readEventCountAfterLastSnapshot(id));
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as we write right before we get.
    private <I> void writeAndReadEventTest(I id, AggregateStorage<I> storage) {
        Event expectedEvent = eventFactory.createEvent(Time.getCurrentTime());

        storage.writeEvent(id, expectedEvent);

        AggregateReadRequest<I> readRequest = new AggregateReadRequest<>(id, MAX_VALUE);
        AggregateStateRecord events = storage.read(readRequest)
                                             .get();
        assertEquals(1, events.getEventCount());
        Event actualEvent = events.getEvent(0);
        assertEquals(expectedEvent, actualEvent);

        close(storage);
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
    // OK as we write right before we get.
    protected void testWriteRecordsAndLoadHistory(Timestamp firstRecordTime) {
        List<AggregateEventRecord> records = sequenceFor(id, firstRecordTime);

        writeAll(id, records);

        AggregateStateRecord events = storage.read(newReadRequest(id))
                                             .get();
        List<Event> expectedEvents = transform(records, TO_EVENT);
        List<Event> actualEvents = events.getEventList();
        assertEquals(expectedEvents, actualEvents);
    }

    protected void writeAll(ProjectId id, Iterable<AggregateEventRecord> records) {
        for (AggregateEventRecord record : records) {
            storage.writeRecord(id, record);
        }
    }

    private Iterator<AggregateEventRecord> historyBackward() {
        AggregateReadRequest<ProjectId> readRequest = newReadRequest(id);
        return storage.historyBackward(readRequest);
    }

    public static class TestAggregate extends Aggregate<ProjectId, Project, ProjectVBuilder> {
        protected TestAggregate(ProjectId id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdString
            extends Aggregate<String, Project, ProjectVBuilder> {
        private TestAggregateWithIdString(String id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdInteger
            extends Aggregate<Integer, Project, ProjectVBuilder> {
        private TestAggregateWithIdInteger(Integer id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdLong
            extends Aggregate<Long, Project, ProjectVBuilder> {
        private TestAggregateWithIdLong(Long id) {
            super(id);
        }
    }
}
