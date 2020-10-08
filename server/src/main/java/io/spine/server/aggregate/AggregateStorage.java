/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.base.EntityState;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.query.RecordQuery;
import io.spine.server.ContextSpec;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.AbstractStorage;
import io.spine.server.storage.StorageFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.aggregate.AggregateRepository.DEFAULT_SNAPSHOT_TRIGGER;
import static io.spine.server.storage.QueryConverter.convert;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A storage of aggregate events, snapshots and the most recent aggregate states.
 *
 * @param <I>
 *         the type of IDs of aggregates managed by this storage
 * @param <S>
 *         the type of states of aggregates managed by this storage
 */
@SPI
public class AggregateStorage<I, S extends EntityState<I>>
        extends AbstractStorage<I, AggregateHistory> {

    private static final String TRUNCATE_ON_WRONG_SNAPSHOT_MESSAGE =
            "The specified snapshot index `%d` must be non-negative.";

    private final AggregateEventStorage eventStorage;
    private final EntityRecordStorage<I, S> stateStorage;
    private boolean mirrorEnabled = false;
    private final Truncate truncation;
    private final HistoryBackward<I> historyBackward;

    public AggregateStorage(ContextSpec context,
                            Class<? extends Aggregate<I, S, ?>> aggregateClass,
                            StorageFactory factory) {
        super(context.isMultitenant());
        eventStorage = factory.createAggregateEventStorage(context.isMultitenant());
        stateStorage = factory.createEntityRecordStorage(context, aggregateClass);
        truncation = new Truncate(eventStorage);
        historyBackward = new HistoryBackward<>(eventStorage);
    }

    protected AggregateStorage(AggregateStorage<I, S> delegate) {
        super(delegate.isMultitenant());
        this.eventStorage = delegate.eventStorage;
        this.stateStorage = delegate.stateStorage;
        this.mirrorEnabled = delegate.mirrorEnabled;
        this.truncation = delegate.truncation;
        this.historyBackward = delegate.historyBackward;
    }

    void enableMirror() {
        mirrorEnabled = true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Opens the method for the package.
     */
    @Override
    protected void checkNotClosed() throws IllegalStateException {
        super.checkNotClosed();
    }

    /**
     * {@inheritDoc}
     *
     * <p>While it is possible to write individual event records only, in scope of the expected
     * usage scenarios, the IDs, lifecycle and versions of the {@code Aggregates} are
     * {@link #writeState(Aggregate) written} to this storage as well.
     *
     * <p>Therefore, this index contains only the identifiers of the {@code Aggregates} which
     * state was written to the storage.
     */
    @Override
    public Iterator<I> index() {
        return stateStorage.index();
    }

    /**
     * Forms and returns an {@link AggregateHistory} based on the
     * {@linkplain #historyBackward(Object, int)}  aggregate history}.
     *
     * @param id
     *         the identifier of the aggregate for which to return the history
     * @param batchSize
     *         the maximum number of the events to read
     * @return the record instance or {@code Optional.empty()} if the
     *         {@linkplain #historyBackward(Object, int) aggregate history} is empty
     * @throws IllegalStateException
     *         if the storage was closed before
     */
    @SuppressWarnings("CheckReturnValue") // calling builder method
    public Optional<AggregateHistory> read(I id, int batchSize) {
        ReadOperation<I, S> op = new ReadOperation<>(this, id, batchSize);
        return op.perform();
    }

    @Override
    public Optional<AggregateHistory> read(I id) {
        return read(id, DEFAULT_SNAPSHOT_TRIGGER);
    }

    /**
     * Writes events into the storage.
     *
     * <p><b>NOTE</b>: does not rewrite any events. Several events can be associated with one
     * aggregate ID.
     *
     * @param id
     *         the ID for the record
     * @param events
     *         non empty aggregate state record to store
     */
    @Override
    public void write(I id, AggregateHistory events) {
        checkNotClosedAndArguments(id, events);

        List<Event> eventList = events.getEventList();
        checkArgument(!eventList.isEmpty(), "Event list must not be empty.");

        for (Event event : eventList) {
            AggregateEventRecord record = AggregateRecords.newEventRecord(id, event);
            writeEventRecord(id, record);
        }
        if (events.hasSnapshot()) {
            writeSnapshot(id, events.getSnapshot());
        }
    }

    /**
     * Writes an event to the storage by an aggregate ID.
     *
     * <p>Before the storing, {@linkplain io.spine.core.Event#clearEnrichments() enrichments}
     * will be removed from the event.
     *
     * @param id
     *         the aggregate ID
     * @param event
     *         the event to write
     */
    void writeEvent(I id, Event event) {
        checkNotClosedAndArguments(id, event);

        Event eventWithoutEnrichments = event.clearEnrichments();
        AggregateEventRecord record = AggregateRecords.newEventRecord(id, eventWithoutEnrichments);
        writeEventRecord(id, record);
    }

    /**
     * Writes a {@code snapshot} by an {@code aggregateId} to the storage.
     *
     * @param aggregateId
     *         an ID of an aggregate of which the snapshot is made
     * @param snapshot
     *         the snapshot of the aggregate
     */
    void writeSnapshot(I aggregateId, Snapshot snapshot) {
        checkNotClosedAndArguments(aggregateId, snapshot);

        AggregateEventRecord record = AggregateRecords.newEventRecord(aggregateId, snapshot);
        writeEventRecord(aggregateId, record);
    }

    /**
     * Writes the passed record into the storage.
     *
     * @param id
     *         the aggregate ID
     * @param record
     *         the record to write
     */
    protected void writeEventRecord(I id, AggregateEventRecord record) {
        eventStorage.write(record.getId(), record);
    }

    /**
     * Reads the aggregate states from the storage according to the passed filters and returns
     * the results in the specified response format.
     *
     * <p>The results of this call are eventually consistent with the latest states of aggregates,
     * as instances are <em>not</em> restored from their events for querying.
     * Instead, this method works on top of the storage of the latest known aggregate states,
     * for better performance. In a distributed environment, the records in this storage may be
     * outdated, as new events may have been emitted by an aggregate instance on other server nodes.
     *
     * @param filters
     *         the filters to use when querying the aggregate states
     * @param format
     *         the format of the response
     * @return an iterator over the matching {@code EntityRecord}s
     */
    protected Iterator<EntityRecord> readStates(TargetFilters filters, ResponseFormat format) {
        ensureVisible();
        RecordQuery<I, EntityRecord> query = convert(stateStorage.recordSpec(), filters, format);
        return stateStorage.readAll(query);
    }

    /**
     * Reads the aggregate states from the storage and returns the results in the specified format.
     *
     * <p>This method performs no filtering. Other than that, it works in the same manner
     * as {@link #readStates(TargetFilters, ResponseFormat) readStates(filters, format)}.
     *
     * @param format
     *         the format of the response
     * @return an iterator over the records
     */
    protected Iterator<EntityRecord> readStates(ResponseFormat format) {
        ensureVisible();
        RecordQuery<I, EntityRecord> query = convert(stateStorage.recordSpec(), format);
        return stateStorage.readAll(query);
    }

    private void ensureVisible() {
        if (!mirrorEnabled) {
            throw newIllegalStateException(
                    "The aggregate state `%s` is NOT marked as visible for querying.",
                    stateClass());
        }
    }

    private Class<? extends EntityState<?>> stateClass() {
        return stateStorage.recordSpec()
                           .entityClass()
                           .stateClass();
    }

    protected void writeState(Aggregate<I, ?, ?> aggregate) {
        EntityRecord record = AggregateRecords.newStateRecord(aggregate, mirrorEnabled);
        EntityRecordWithColumns<I> result =
                EntityRecordWithColumns.create(aggregate, record);
        stateStorage.write(result);
    }

    protected void writeAll(Aggregate<I, ?, ?> aggregate,
                            ImmutableList<AggregateHistory> historySegments) {
        for (AggregateHistory history : historySegments) {
            write(aggregate.id(), history);
        }
        writeState(aggregate);
    }

    /**
     * Creates iterator of aggregate event history with the reverse traversal.
     *
     * <p>Records are sorted by timestamp descending (from newer to older).
     * The iterator is empty if there's no history for the aggregate with passed ID.
     *
     * @param id
     *         the identifier of the aggregate
     * @param batchSize
     *         the maximum number of the history records to read
     * @return new iterator instance
     */
    Iterator<AggregateEventRecord> historyBackward(I id, int batchSize) {
        return historyBackward(id, batchSize, null);
    }

    protected Iterator<AggregateEventRecord>
    historyBackward(I id, int batchSize, @Nullable Version startingFrom) {
        return historyBackward.read(id, batchSize, startingFrom);
    }

    /**
     * Truncates the storage, dropping all records which occur before the N-th snapshot for each
     * entity.
     *
     * <p>The snapshot index is counted from the latest to earliest, with {@code 0} representing
     * the latest snapshot.
     *
     * <p>The snapshot index higher than the overall snapshot count of the entity is allowed, the
     * entity records remain intact in this case.
     *
     * @throws IllegalArgumentException
     *         if the {@code snapshotIndex} is negative
     */
    @Internal
    public void truncateOlderThan(int snapshotIndex) {
        checkArgument(snapshotIndex >= 0, TRUNCATE_ON_WRONG_SNAPSHOT_MESSAGE);
        truncate(snapshotIndex);
    }

    /**
     * Truncates the storage, dropping all records older than {@code date} but not newer than the
     * Nth snapshot.
     *
     * <p>The snapshot index is counted from the latest to earliest, with {@code 0} representing
     * the latest snapshot for each entity.
     *
     * <p>The snapshot index higher than the overall snapshot count of the entity is allowed, the
     * records remain intact in this case.
     *
     * @throws IllegalArgumentException
     *         if the {@code snapshotIndex} is negative
     */
    @Internal
    public void truncateOlderThan(int snapshotIndex, Timestamp date) {
        checkNotNull(date);
        checkArgument(snapshotIndex >= 0, TRUNCATE_ON_WRONG_SNAPSHOT_MESSAGE, snapshotIndex);
        truncate(snapshotIndex, date);
    }

    /**
     * Drops all records which occur before the Nth snapshot for each entity.
     */
    protected void truncate(int snapshotIndex) {
        truncation.performWith(snapshotIndex, (r) -> true);
    }

    /**
     * Drops all records older than {@code date} but not newer than the Nth snapshot for each
     * entity.
     */
    protected void truncate(int snapshotIndex, Timestamp date) {
        truncation.performWith(snapshotIndex,
                               (r) -> Timestamps.compare(r.getTimestamp(), date) < 0);
    }

    private void checkNotClosedAndArguments(I id, Object argument) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(argument);
    }
}
