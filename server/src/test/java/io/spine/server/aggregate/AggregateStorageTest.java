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
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.storage.AbstractStorageShould;
import io.spine.test.Tests;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static io.spine.core.given.GivenEnrichment.withOneAttribute;
import static io.spine.server.aggregate.given.StorageRecords.sequenceFor;
import static io.spine.server.command.TestEventFactory.newInstance;
import static io.spine.time.Durations2.seconds;
import static io.spine.validate.Validate.isDefault;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Litus
 */
@DisplayName("AggregateStorage should")
public abstract class AggregateStorageTest
        extends AbstractStorageShould<ProjectId,
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

    // Ignore this test because several records can be stored by an aggregate ID.
    @Override
    @SuppressWarnings({
            "NoopMethodInAbstractClass",
            "RefusedBequest",
            "MethodDoesntCallSuperMethod"
    })
    public void rewrite_record_if_write_by_the_same_id() {
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

    @Test
    @DisplayName("return iterator over empty collection when reading history from empty storage")
    void readEmptyHistory() {
        Iterator<AggregateEventRecord> iterator = historyBackward();

        assertFalse(iterator.hasNext());
    }

    @Test
    @DisplayName("return absent AggregateStateRecord when reading history from empty storage")
    void readRecordWithEmptyHistory() {
        AggregateReadRequest<ProjectId> readRequest = newReadRequest(id);
        Optional<AggregateStateRecord> record = storage.read(readRequest);

        assertFalse(record.isPresent());
    }

    @Test
    @DisplayName("throw exception when trying to read history by null ID")
    void notReadByNullId() {
        assertThrows(NullPointerException.class,
                     () -> storage.historyBackward(Tests.nullRef()));
    }

    @Test
    @DisplayName("throw exception when trying to write null event")
    void notWriteNullEvent() {
        assertThrows(NullPointerException.class,
                     () -> storage.writeEvent(id, Tests.nullRef()));
    }

    @Test
    @DisplayName("throw exception when trying to write event by null ID")
    void notWriteEventByNullId() {
        assertThrows(NullPointerException.class,
                     () -> storage.writeEvent(Tests.nullRef(), Event.getDefaultInstance()));
    }

    @Test
    @DisplayName("throw exception when trying to write null snapshot")
    void notWriteNullSnapshot() {
        assertThrows(NullPointerException.class,
                     () -> storage.write(id, Tests.nullRef()));
    }

    @Test
    @DisplayName("throw exception when trying to write snapshot by null ID")
    void notWriteSnapshotByNullId() {
        assertThrows(NullPointerException.class,
                     () -> storage.writeSnapshot(Tests.nullRef(), Snapshot.getDefaultInstance()));
    }

    @Test
    @DisplayName("write and read one event by Message ID")
    void writeAndReadEventByMessageId() {
        writeAndReadEventTest(id, storage);
    }

    @Test
    @DisplayName("write and read one event by String ID")
    void writeAndReadEventByStringId() {
        AggregateStorage<String> storage = newStorage(String.class,
                                                      TestAggregateWithIdString.class);
        String id = newUuid();
        writeAndReadEventTest(id, storage);
    }

    @Test
    @DisplayName("write and read one event by Long ID")
    void writeAndReadEventByLongId() {
        AggregateStorage<Long> storage = newStorage(Long.class,
                                                    TestAggregateWithIdLong.class);
        long id = 10L;
        writeAndReadEventTest(id, storage);
    }

    @Test
    @DisplayName("write and read one event by Integer ID")
    void writeAndReadEventByIntegerId() {
        AggregateStorage<Integer> storage = newStorage(Integer.class,
                                                       TestAggregateWithIdInteger.class);
        int id = 10;
        writeAndReadEventTest(id, storage);
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

    @Test
    @DisplayName("read history of archived aggregate")
    void readHistoryOfArchivedAggregate() {
        LifecycleFlags archivedRecordFlags = LifecycleFlags.newBuilder()
                                                           .setArchived(true)
                                                           .build();
        storage.writeLifecycleFlags(id, archivedRecordFlags);
        writeAndReadEventTest(id, storage);
    }

    @Test
    @DisplayName("read history of deleted aggregate")
    void readHistoryOfDeletedAggregate() {
        LifecycleFlags deletedRecordFlags = LifecycleFlags.newBuilder()
                                                          .setDeleted(true)
                                                          .build();
        storage.writeLifecycleFlags(id, deletedRecordFlags);
        writeAndReadEventTest(id, storage);
    }

    @Test
    @DisplayName("index archived aggregate")
    void indexArchivedAggregate() {
        LifecycleFlags archivedRecordFlags = LifecycleFlags.newBuilder()
                                                           .setArchived(true)
                                                           .build();
        storage.writeRecord(id, StorageRecord.create(getCurrentTime()));
        storage.writeLifecycleFlags(id, archivedRecordFlags);
        assertTrue(storage.index()
                          .hasNext());
    }

    @Test
    @DisplayName("index deleted aggregate")
    void indexDeletedAggregate() {
        LifecycleFlags deletedRecordFlags = LifecycleFlags.newBuilder()
                                                          .setDeleted(true)
                                                          .build();
        storage.writeRecord(id, StorageRecord.create(getCurrentTime()));
        storage.writeLifecycleFlags(id, deletedRecordFlags);
        assertTrue(storage.index()
                          .hasNext());
    }

    @Test
    @DisplayName("write records and return them sorted by timestamp descending")
    void writeRecordsAndReturnSortedByTimestamp() {
        List<AggregateEventRecord> records = sequenceFor(id);

        writeAll(id, records);

        Iterator<AggregateEventRecord> iterator = historyBackward();
        List<AggregateEventRecord> actual = newArrayList(iterator);
        reverse(records); // expected records should be in a reverse order
        assertEquals(records, actual);
    }

    @Test
    @DisplayName("write records and return them sorted by version descending")
    void writeRecordsAndReturnSortedByVersion() {
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

    @Test
    @DisplayName("write records and load history if there are no snapshots available")
    void writeRecordsAndLoadHistoryIfNoSnapshots() {
        testWriteRecordsAndLoadHistory(getCurrentTime());
    }

    @Test
    @DisplayName("write records and load history till last snapshot available")
    void writeRecordsAndLoadHistoryTillLastSnapshot() {
        Duration delta = seconds(10);
        Timestamp time1 = getCurrentTime();
        Timestamp time2 = add(time1, delta);
        Timestamp time3 = add(time2, delta);

        storage.writeRecord(id, StorageRecord.create(time1));
        storage.writeSnapshot(id, newSnapshot(time2));

        testWriteRecordsAndLoadHistory(time3);
    }

    @Test
    @DisplayName("return zero event count after last snapshot by default")
    void returnZeroEventCountByDefault() {
        assertEquals(0, storage.readEventCountAfterLastSnapshot(id));
    }

    @Test
    @DisplayName("write and read event count after last snapshot")
    void writeAndReadEventCount() {
        int expectedCount = 32;
        storage.writeEventCountAfterLastSnapshot(id, expectedCount);

        int actualCount = storage.readEventCountAfterLastSnapshot(id);

        assertEquals(expectedCount, actualCount);
    }

    @Test
    @DisplayName("re-write event count after last snapshot")
    void rewriteEventCount() {
        int primaryValue = 16;
        storage.writeEventCountAfterLastSnapshot(id, primaryValue);
        int expectedValue = 32;
        storage.writeEventCountAfterLastSnapshot(id, expectedValue);

        int actualCount = storage.readEventCountAfterLastSnapshot(id);

        assertEquals(expectedValue, actualCount);
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

    @Test
    @DisplayName("not store enrichment for EventContext")
    void notStoreEnrichmentForEventContext() {
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
    @DisplayName("not store enrichment for origin of RejectionContext type")
    void notStoreEnrichmentForRejectionContextOrigin() {
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
    @DisplayName("not store enrichment for origin of EventContext type")
    void notStoreEnrichmentForEventContextOrigin() {
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

    @Test
    @DisplayName("read archived records")
    void readArchivedRecords() {
        readRecordsWithLifecycle(LifecycleFlags.newBuilder()
                                               .setArchived(true)
                                               .build());
    }

    @Test
    @DisplayName("read deleted records")
    void readDeletedRecords() {
        readRecordsWithLifecycle(LifecycleFlags.newBuilder()
                                               .setDeleted(true)
                                               .build());
    }

    @Test
    @DisplayName("read archived and deleted records")
    void readArchivedAndDeletedRecords() {
        readRecordsWithLifecycle(LifecycleFlags.newBuilder()
                                               .setArchived(true)
                                               .setDeleted(true)
                                               .build());
    }

    @Test
    @DisplayName("throw exception when trying to write event count to closed storage")
    void notWriteEventCountToClosedStorage() {
        close(storage);

        assertThrows(IllegalStateException.class,
                     () -> storage.writeEventCountAfterLastSnapshot(id, 5));
    }

    @Test
    @DisplayName("throw exception when trying to read event count from closed storage")
    void notReadEventCountFromClosedStorage() {
        close(storage);

        assertThrows(IllegalStateException.class,
                     () -> storage.readEventCountAfterLastSnapshot(id));
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

    private void readRecordsWithLifecycle(LifecycleFlags flags) {
        AggregateStateRecord record = newStorageRecord();
        storage.write(id, record);
        storage.writeLifecycleFlags(id, flags);
        Optional<AggregateStateRecord> read = storage.read(newReadRequest(id));
        assertTrue(read.isPresent());
        AggregateStateRecord readRecord = read.get();
        assertEquals(record, readRecord);
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
