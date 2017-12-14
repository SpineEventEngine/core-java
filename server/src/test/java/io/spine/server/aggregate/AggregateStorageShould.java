/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.RejectionContext;
import io.spine.core.Version;
import io.spine.server.aggregate.given.Given.StorageRecord;
import io.spine.server.command.TestEventFactory;
import io.spine.server.storage.AbstractStorageShould;
import io.spine.test.Tests;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.testdata.Sample;
import io.spine.time.Time;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Lists.transform;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.Identifier.newUuid;
import static io.spine.core.Versions.increment;
import static io.spine.core.Versions.zero;
import static io.spine.core.given.GivenEnrichment.withOneAttribute;
import static io.spine.server.aggregate.given.Given.StorageRecords.sequenceFor;
import static io.spine.server.command.TestEventFactory.newInstance;
import static io.spine.time.Durations2.seconds;
import static io.spine.time.Time.getCurrentTime;
import static io.spine.validate.Validate.isDefault;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.reverse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
public abstract class AggregateStorageShould
        extends AbstractStorageShould<ProjectId,
                                      AggregateStateRecord,
                                      AggregateReadRequest<ProjectId>,
                                      AggregateStorage<ProjectId>> {

    private final ProjectId id = Sample.messageOfType(ProjectId.class);
    private final TestEventFactory eventFactory = newInstance(AggregateStorageShould.class);

    private AggregateStorage<ProjectId> storage;

    @Override
    public void setUpAbstractStorageTest() {
        super.setUpAbstractStorageTest();
        storage = getStorage();
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
    protected abstract <I> AggregateStorage<I> newStorage(Class<? extends I> idClass,
                                                          Class<? extends Aggregate<I, ?, ?>> aggregateClass);

    @Override
    protected AggregateStateRecord newStorageRecord() {
        final List<AggregateEventRecord> records = sequenceFor(id);
        final List<Event> expectedEvents = transform(records, TO_EVENT);
        final AggregateStateRecord aggregateStateRecord =
                AggregateStateRecord.newBuilder()
                                    .addAllEvent(expectedEvents)
                                    .build();
        return aggregateStateRecord;
    }

    @Override
    protected ProjectId newId() {
        return Sample.messageOfType(ProjectId.class);
    }

    @Override
    protected AggregateReadRequest<ProjectId> newReadRequest(ProjectId id) {
        return new AggregateReadRequest<>(id, MAX_VALUE);
    }

    @Test
    public void return_iterator_over_empty_collection_if_read_history_from_empty_storage() {
        final Iterator<AggregateEventRecord> iterator = historyBackward();

        assertFalse(iterator.hasNext());
    }

    @Test
    public void return_absent_AggregateStateRecord_if_read_history_from_empty_storage() {
        final AggregateReadRequest<ProjectId> readRequest = newReadRequest(id);
        final Optional<AggregateStateRecord> aggregateStateRecord = storage.read(readRequest);

        assertFalse(aggregateStateRecord.isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_read_history_by_null_id() {
        storage.historyBackward(Tests.<AggregateReadRequest<ProjectId>>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_event() {
        storage.writeEvent(id, Tests.<Event>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_event_by_null_id() {
        storage.writeEvent(Tests.<ProjectId>nullRef(), Event.getDefaultInstance());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null_snapshot() {
        storage.write(id, Tests.<AggregateStateRecord>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_snapshot_by_null_id() {
        storage.writeSnapshot(Tests.<ProjectId>nullRef(), Snapshot.getDefaultInstance());
    }

    @Test
    public void write_read_and_one_event_by_Message_id() {
        writeAndReadEventTest(id, storage);
    }

    @Test
    public void write_and_read_event_by_String_id() {
        final AggregateStorage<String> storage = newStorage(String.class,
                                                            TestAggregateWithIdString.class);
        final String id = newUuid();
        writeAndReadEventTest(id, storage);
    }

    @Test
    public void write_and_read_event_by_Long_id() {
        final AggregateStorage<Long> storage = newStorage(Long.class,
                                                          TestAggregateWithIdLong.class);
        final long id = 10L;
        writeAndReadEventTest(id, storage);
    }

    @Test
    public void write_and_read_event_by_Integer_id() {
        final AggregateStorage<Integer> storage = newStorage(Integer.class,
                                                             TestAggregateWithIdInteger.class);
        final int id = 10;
        writeAndReadEventTest(id, storage);
    }

    @Test
    public void write_and_read_one_record() {
        final AggregateEventRecord expected = StorageRecord.create(getCurrentTime());

        storage.writeRecord(id, expected);

        final Iterator<AggregateEventRecord> iterator = historyBackward();
        assertTrue(iterator.hasNext());
        final AggregateEventRecord actual = iterator.next();
        assertEquals(expected, actual);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_records_and_return_sorted_by_timestamp_descending() {
        final List<AggregateEventRecord> records = sequenceFor(id);

        writeAll(id, records);

        final Iterator<AggregateEventRecord> iterator = historyBackward();
        final List<AggregateEventRecord> actual = newArrayList(iterator);
        reverse(records); // expected records should be in a reverse order
        assertEquals(records, actual);
    }

    @Test
    public void write_records_and_return_sorted_by_version_descending() {
        final int eventsNumber = 5;
        final List<AggregateEventRecord> records = newLinkedList();
        final Timestamp timestamp = getCurrentTime();
        Version currentVersion = zero();
        for (int i = 0; i < eventsNumber; i++) {
            final Project state = Project.getDefaultInstance();
            final Event event = eventFactory.createEvent(state, currentVersion, timestamp);
            final AggregateEventRecord record = StorageRecord.create(timestamp, event);
            records.add(record);
            currentVersion = increment(currentVersion);
        }
        writeAll(id, records);

        final Iterator<AggregateEventRecord> iterator = historyBackward();
        final List<AggregateEventRecord> actual = newArrayList(iterator);
        reverse(records); // expected records should be in a reverse order
        assertEquals(records, actual);
    }

    @Test
    public void sort_by_version_rather_then_by_timestamp() {
        final Project state = Project.getDefaultInstance();
        final Version minVersion = zero();
        final Version maxVersion = increment(minVersion);
        final Timestamp minTimestamp = Timestamps.MIN_VALUE;
        final Timestamp maxTimestamp = Timestamps.MAX_VALUE;

        // The first event is an event, which is the oldest, i.e. with the minimal version.
        final Event expectedFirst = eventFactory.createEvent(state, minVersion, maxTimestamp);
        final Event expectedSecond = eventFactory.createEvent(state, maxVersion, minTimestamp);

        storage.writeEvent(id, expectedSecond);
        storage.writeEvent(id, expectedFirst);

        final List<Event> events = storage.read(newReadRequest(id))
                                          .get()
                                          .getEventList();
        assertTrue(events.indexOf(expectedFirst) < events.indexOf(expectedSecond));
    }

    @Test
    public void write_and_read_snapshot() {
        final Snapshot expected = newSnapshot(getCurrentTime());

        storage.writeSnapshot(id, expected);

        final Iterator<AggregateEventRecord> iterator = historyBackward();
        assertTrue(iterator.hasNext());
        final AggregateEventRecord actual = iterator.next();
        assertEquals(expected, actual.getSnapshot());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void write_records_and_load_history_if_no_snapshots() {
        testWriteRecordsAndLoadHistory(getCurrentTime());
    }

    @Test
    public void write_records_and_load_history_till_last_snapshot() {
        final Duration delta = seconds(10);
        final Timestamp time1 = getCurrentTime();
        final Timestamp time2 = add(time1, delta);
        final Timestamp time3 = add(time2, delta);

        storage.writeRecord(id, StorageRecord.create(time1));
        storage.writeSnapshot(id, newSnapshot(time2));

        testWriteRecordsAndLoadHistory(time3);
    }

    @Test
    public void return_zero_event_count_after_last_snapshot_by_default() {
        assertEquals(0, storage.readEventCountAfterLastSnapshot(id));
    }

    @Test
    public void write_and_read_event_count_after_last_snapshot() {
        final int expectedCount = 32;
        storage.writeEventCountAfterLastSnapshot(id, expectedCount);

        final int actualCount = storage.readEventCountAfterLastSnapshot(id);

        assertEquals(expectedCount, actualCount);
    }

    @Test
    public void rewrite_event_count_after_last_snapshot() {
        final int primaryValue = 16;
        storage.writeEventCountAfterLastSnapshot(id, primaryValue);
        final int expectedValue = 32;
        storage.writeEventCountAfterLastSnapshot(id, expectedValue);

        final int actualCount = storage.readEventCountAfterLastSnapshot(id);

        assertEquals(expectedValue, actualCount);
    }

    @Test
    public void continue_history_reading_if_snapshot_was_not_found_in_first_batch() {
        Version currentVersion = zero();
        final Snapshot snapshot = Snapshot.newBuilder()
                                          .setVersion(currentVersion)
                                          .build();
        storage.writeSnapshot(id, snapshot);

        final int eventCountAfterSnapshot = 10;
        for (int i = 0; i < eventCountAfterSnapshot; i++) {
            currentVersion = increment(currentVersion);
            final Project state = Project.getDefaultInstance();
            final Event event = eventFactory.createEvent(state, currentVersion);
            storage.writeEvent(id, event);
        }

        final int batchSize = 1;
        final AggregateReadRequest<ProjectId> request = new AggregateReadRequest<>(id, batchSize);
        final Optional<AggregateStateRecord> optionalStateRecord = storage.read(request);

        assertTrue(optionalStateRecord.isPresent());
        final AggregateStateRecord stateRecord = optionalStateRecord.get();
        assertEquals(snapshot, stateRecord.getSnapshot());
        assertEquals(eventCountAfterSnapshot, stateRecord.getEventCount());
    }

    @Test
    public void not_store_enrichment_for_EventContext() {
        final EventContext enrichedContext = EventContext.newBuilder()
                                                         .setEnrichment(withOneAttribute())
                                                         .build();
        final Event event = Event.newBuilder()
                                 .setId(newEventId())
                                 .setContext(enrichedContext)
                                 .setMessage(Any.getDefaultInstance())
                                 .build();
        storage.writeEvent(id, event);
        final EventContext loadedContext = storage.read(newReadRequest(id))
                                                  .get()
                                                  .getEvent(0)
                                                  .getContext();
        assertTrue(isDefault(loadedContext.getEnrichment()));
    }

    @Test
    public void not_store_enrichment_for_origin_of_RejectionContext_type() {
        final RejectionContext origin = RejectionContext.newBuilder()
                                                        .setEnrichment(withOneAttribute())
                                                        .build();
        final EventContext context = EventContext.newBuilder()
                                                 .setRejectionContext(origin)
                                                 .build();
        final Event event = Event.newBuilder()
                                 .setId(newEventId())
                                 .setContext(context)
                                 .setMessage(Any.getDefaultInstance())
                                 .build();
        storage.writeEvent(id, event);
        final RejectionContext loadedOrigin = storage.read(newReadRequest(id))
                                                     .get()
                                                     .getEvent(0)
                                                     .getContext()
                                                     .getRejectionContext();
        assertTrue(isDefault(loadedOrigin.getEnrichment()));
    }

    @Test
    public void not_store_enrichment_for_origin_of_EventContext_type() {
        final EventContext origin = EventContext.newBuilder()
                                                .setEnrichment(withOneAttribute())
                                                .build();
        final EventContext context = EventContext.newBuilder()
                                                 .setEventContext(origin)
                                                 .build();
        final Event event = Event.newBuilder()
                                 .setId(newEventId())
                                 .setContext(context)
                                 .setMessage(Any.getDefaultInstance())
                                 .build();
        storage.writeEvent(id, event);
        final EventContext loadedOrigin = storage.read(newReadRequest(id))
                                                 .get()
                                                 .getEvent(0)
                                                 .getContext()
                                                 .getEventContext();
        assertTrue(isDefault(loadedOrigin.getEnrichment()));
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_write_event_count_to_closed_storage() {
        close(storage);

        storage.writeEventCountAfterLastSnapshot(id, 5);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_read_event_count_from_closed_storage() {
        close(storage);

        storage.readEventCountAfterLastSnapshot(id);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as we write right before we get.
    private <I> void writeAndReadEventTest(I id, AggregateStorage<I> storage) {
        final Event expectedEvent = eventFactory.createEvent(Time.getCurrentTime());

        storage.writeEvent(id, expectedEvent);

        final AggregateReadRequest<I> readRequest = new AggregateReadRequest<>(id, MAX_VALUE);
        final AggregateStateRecord events = storage.read(readRequest)
                                                   .get();
        assertEquals(1, events.getEventCount());
        final Event actualEvent = events.getEvent(0);
        assertEquals(expectedEvent, actualEvent);

        close(storage);
    }

    // Ignore this test because several records can be stored by an aggregate ID.
    @Override
    @SuppressWarnings({"NoopMethodInAbstractClass", "RefusedBequest", "MethodDoesntCallSuperMethod"})
    public void rewrite_record_if_write_by_the_same_id() {
    }

    @SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
    // OK as we write right before we get.
    protected void testWriteRecordsAndLoadHistory(Timestamp firstRecordTime) {
        final List<AggregateEventRecord> records = sequenceFor(id, firstRecordTime);

        writeAll(id, records);

        final AggregateStateRecord events = storage.read(newReadRequest(id))
                                                   .get();
        final List<Event> expectedEvents = transform(records, TO_EVENT);
        final List<Event> actualEvents = events.getEventList();
        assertEquals(expectedEvents, actualEvents);
    }

    protected void writeAll(ProjectId id, Iterable<AggregateEventRecord> records) {
        for (AggregateEventRecord record : records) {
            storage.writeRecord(id, record);
        }
    }

    private Iterator<AggregateEventRecord> historyBackward() {
        final AggregateReadRequest<ProjectId> readRequest = newReadRequest(id);
        return storage.historyBackward(readRequest);
    }

    protected static final Function<AggregateEventRecord, Event> TO_EVENT =
            new Function<AggregateEventRecord, Event>() {
                @Nullable // return null because an exception won't be propagated in this case
                @Override
                public Event apply(@Nullable AggregateEventRecord input) {
                    return (input == null) ? null : input.getEvent();
                }
            };

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
