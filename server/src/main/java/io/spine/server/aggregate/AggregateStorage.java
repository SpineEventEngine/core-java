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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.OrderBy;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.server.ContextSpec;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.storage.EntityColumns;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.QueryParameters;
import io.spine.server.storage.AbstractStorage;
import io.spine.server.storage.MessageColumns;
import io.spine.server.storage.MessageQueries;
import io.spine.server.storage.MessageQuery;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.MessageWithColumns;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.string.Stringifiers;
import io.spine.system.server.MirrorId;
import io.spine.system.server.MirrorProjection;
import io.spine.system.server.MirrorRepository;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Streams.stream;
import static com.google.protobuf.util.Timestamps.checkValid;
import static io.spine.client.Filters.all;
import static io.spine.client.Filters.eq;
import static io.spine.server.aggregate.AggregateEventRecordColumn.aggregateId;
import static io.spine.server.aggregate.AggregateEventRecordColumn.created;
import static io.spine.server.aggregate.AggregateEventRecordColumn.version;
import static io.spine.system.server.MirrorProjection.TYPE_COLUMN_NAME;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;
import static java.lang.String.format;

/**
 * An event-sourced storage of aggregate part events and snapshots.
 *
 * @param <I>
 *         the type of IDs of aggregates managed by this storage
 */
@SPI
public class AggregateStorage<I> extends AbstractStorage<I, AggregateHistory> {

    private static final String TRUNCATE_ON_WRONG_SNAPSHOT_MESSAGE =
            "The specified snapshot index is incorrect";
    private static final int DEFAULT_HISTORY_DEPTH = 50;

    private final MessageStorage<AggregateEventRecordId, AggregateEventRecord> eventStorage;
    private final EntityRecordStorage<I> stateStorage;
    private boolean mirrorEnabled = false;

    /**
     * Executes certain kinds of aggregate reads using the
     * {@linkplain MirrorProjection mirror projections} of aggregates.
     *
     * <p>Used to optimize performance-heavy storage operations.
     *
     * <p>Is {@code null} either if not yet configured or if the corresponding aggregate type
     * is not mirrored.
     *
     * @see MirrorRepository
     */
//    private @Nullable Mirror<I> aggregateMirror;

    public AggregateStorage(ContextSpec spec,
                            Class<? extends Aggregate<I, ?, ?>> aggregateClass,
                            StorageFactory factory) {
        super(spec.isMultitenant());
        MessageColumns<AggregateEventRecord> eventColumns = new MessageColumns<>(
                AggregateEventRecord.class, AggregateEventRecordColumn.definitions()
        );
        eventStorage = factory.createMessageStorage(eventColumns, spec.isMultitenant());
        stateStorage = factory.createEntityRecordStorage(spec, aggregateClass);
    }

    protected AggregateStorage(AggregateStorage<I> delegate) {
        super(delegate.isMultitenant());
        this.eventStorage = delegate.eventStorage;
        this.stateStorage = delegate.stateStorage;
        this.mirrorEnabled = delegate.mirrorEnabled;
    }

    //    /**
//     * Configures an aggregate {@code Mirror} to optimize certain kinds of aggregate reads.
//     *
//     * @param mirrorRepository
//     *         the repository storing mirror {@linkplain MirrorProjection projections}
//     * @param stateType
//     *         the state type of a stored {@code Aggregate}
//     * @throws IllegalStateException
//     *         if the state type of a stored {@code Aggregate} is not mirrored by the
//     *         {@code MirrorRepository}
//     */
    void enableMirror() {
//        checkState(mirrorRepository.isMirroring(stateType),
//                   "An aggregate of type `%s` is not mirrored by a `MirrorRepository`.",
//                   stateType);
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
     * <p>Attempts to read aggregate IDs from the {@link MirrorRepository}, as they are already
     * stored there in a convenient form.
     *
     * <p>If an aggregate {@link MirrorRepository} is not configured to use for reads with this
     * repository, falls back to the default method of getting distinct aggregate IDs
     * from the event records.
     */
    @Override
    public Iterator<I> index() {
        if (mirrorEnabled) {
            return stateStorage.index();
        }
        return distinctAggregateIds();
    }

    public Optional<LifecycleFlags> readLifecycleFlags(I id) {
        return stateStorage.read(id)
                           .map(EntityRecord::getLifecycleFlags);
    }

    //TODO:2020-03-25:alex.tymchenko: get rid of this method.
    // Only allow to update the lifecycle flags for the entity.
    public void writeLifecycleFlags(I id, LifecycleFlags flags) {
        Optional<EntityRecord> read = stateStorage.read(id);
        EntityRecord record;
        if (read.isPresent()) {
            record = read.get();
            record = record.toBuilder()
                           .setLifecycleFlags(flags)
                           .vBuild();

        } else {
            record = EntityRecord.newBuilder()
                                 .setEntityId(Identifier.pack(id))
                                 .setLifecycleFlags(flags)
                                 .vBuild();
        }
        MessageWithColumns<I, EntityRecord> asMessage =
                MessageWithColumns.create(id, record, ImmutableMap.of());
        stateStorage.write(asMessage);
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
        ReadOperation<I> op = new ReadOperation<>(this, id, batchSize);
        return op.perform();
    }

    @Override
    public Optional<AggregateHistory> read(I id) {
        return read(id, DEFAULT_HISTORY_DEPTH);
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
            AggregateEventRecord record = toEventRecord(id, event);
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
        AggregateEventRecord record = toEventRecord(id, eventWithoutEnrichments);
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

        AggregateEventRecord record = toEventRecord(aggregateId, snapshot);
        writeEventRecord(aggregateId, record);
    }

    private void checkNotClosedAndArguments(I id, Object argument) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(argument);
    }

    private static <I> AggregateEventRecord toEventRecord(I aggregateId, Event event) {
        checkArgument(event.hasContext(), "Event context must be set.");
        checkArgument(event.hasMessage(), "Event message must be set.");

        String eventIdStr = Identifier.toString(event.getId());
        checkNotEmptyOrBlank(eventIdStr, "Event ID cannot be empty or blank.");

        EventContext context = event.context();
        Timestamp timestamp = checkValid(context.getTimestamp());
        Any packedId = Identifier.pack(aggregateId);

        AggregateEventRecordId recordId = eventRecordId(eventIdStr);
        return AggregateEventRecord.newBuilder()
                                   .setId(recordId)
                                   .setAggregateId(packedId)
                                   .setTimestamp(timestamp)
                                   .setEvent(event)
                                   .build();
    }

    private static <I> AggregateEventRecord toEventRecord(I aggregateId, Snapshot snapshot) {
        Timestamp value = checkValid(snapshot.getTimestamp());
        String stringId = Stringifiers.toString(aggregateId);
        String snapshotTimeStamp = Timestamps.toString(snapshot.getTimestamp());
        String snapshotId = format("%s_%s_%s", "snapshot", stringId, snapshotTimeStamp);
        AggregateEventRecordId recordId = eventRecordId(snapshotId);
        return AggregateEventRecord.newBuilder()
                                   .setId(recordId)
                                   .setAggregateId(Identifier.pack(aggregateId))
                                   .setTimestamp(value)
                                   .setSnapshot(snapshot)
                                   .build();
    }

    private static AggregateEventRecordId eventRecordId(String snapshotId) {
        return AggregateEventRecordId.newBuilder()
                                     .setValue(snapshotId)
                                     .vBuild();
    }

    // Storage implementation API.

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
    //TODO:2020-04-01:alex.tymchenko: review the request considering the original `AggregateEventRecord` comparator.
    protected Iterator<AggregateEventRecord> historyBackward(I id, int batchSize) {
        MessageQuery<AggregateEventRecordId> query = historyBackwardQuery(id);
        ResponseFormat responseFormat = historyBackwardResponseFormat(batchSize);
        Iterator<AggregateEventRecord> iterator = eventStorage.readAll(query, responseFormat);
        return iterator;
    }

    Iterator<AggregateEventRecord> historyBackward(I id, int batchSize, Version startingFrom) {
        MessageQuery<AggregateEventRecordId> query = historyBackwardQuery(id);
        query = query.append(QueryParameters.lt(version.column(), startingFrom.getNumber()));
        ResponseFormat responseFormat = historyBackwardResponseFormat(batchSize);
        Iterator<AggregateEventRecord> iterator = eventStorage.readAll(query, responseFormat);
        return iterator;
    }

    private MessageQuery<AggregateEventRecordId> historyBackwardQuery(I id) {
        QueryParameters params = QueryParameters.eq(aggregateId.column(), Identifier.pack(id));
        return MessageQueries.of(params);
    }

    private static ResponseFormat historyBackwardResponseFormat(@Nullable Integer batchSize) {
        ResponseFormat.Builder builder =
                ResponseFormat.newBuilder()
                              .addOrderBy(higherVersionFirst())
                              .addOrderBy(newestFirst());
        if (batchSize != null) {
            builder.setLimit(batchSize);
        }
        return builder.vBuild();
    }

    private static OrderBy newestFirst() {
        return OrderBy.newBuilder()
                      .setDirection(OrderBy.Direction.DESCENDING)
                      .setColumn(created.column()
                                        .name()
                                        .value())
                      .vBuild();
    }

    private static OrderBy higherVersionFirst() {
        return OrderBy.newBuilder()
                      .setDirection(OrderBy.Direction.DESCENDING)
                      .setColumn(version.column()
                                        .name()
                                        .value())
                      .vBuild();
    }

    /**
     * Truncates the storage, dropping all records which occur before the Nth snapshot for each
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
        checkArgument(snapshotIndex >= 0, TRUNCATE_ON_WRONG_SNAPSHOT_MESSAGE);
        truncate(snapshotIndex, date);
    }

    /**
     * Drops all records which occur before the Nth snapshot for each entity.
     */
    protected void truncate(int snapshotIndex) {
        truncate(snapshotIndex, (r) -> true);
    }

    /**
     * Drops all records older than {@code date} but not newer than the Nth snapshot for each
     * entity.
     */
    protected void truncate(int snapshotIndex, Timestamp date) {
        truncate(snapshotIndex, (r) -> Timestamps.compare(r.getTimestamp(), date) < 0);
    }

    protected void truncate(int snapshotIndex, Predicate<AggregateEventRecord> predicate) {
        ResponseFormat orderChronologically = historyBackwardResponseFormat(null);
        Iterator<AggregateEventRecord> eventRecords = eventStorage.readAll(orderChronologically);
        Map<Any, Integer> snapshotHitsByAggregateId = newHashMap();
        while (eventRecords.hasNext()) {
            AggregateEventRecord eventRecord = eventRecords.next();
            Any packedId = eventRecord.getAggregateId();
            int snapshotsHit = snapshotHitsByAggregateId.get(packedId) != null
                               ? snapshotHitsByAggregateId.get(packedId)
                               : 0;
            if (snapshotsHit > snapshotIndex && predicate.test(eventRecord)) {
                eventStorage.delete(eventRecord.getId());
            }
            if (eventRecord.hasSnapshot()) {
                snapshotHitsByAggregateId.put(packedId, snapshotsHit + 1);
            }
        }
    }

    /**
     * Obtains distinct aggregate IDs from the stored event records.
     */
    protected Iterator<I> distinctAggregateIds() {
        Iterator<AggregateEventRecord> iterator =
                eventStorage.readAll(ResponseFormat.getDefaultInstance());
        ImmutableSet<I> distictIds = stream(iterator).map(this::aggregateIdOf)
                                                     .collect(toImmutableSet());
        return distictIds.iterator();
    }

    @SuppressWarnings("unchecked")
    private I aggregateIdOf(AggregateEventRecord record) {
        return (I) Identifier.unpack(record.getAggregateId());
    }

    Iterator<EntityRecord> readStates(TargetFilters filters, ResponseFormat format) {
        MessageQuery<I> query = MessageQueries.from(filters, stateStorage.columns());
        return stateStorage.readAll(query, format);
    }

    void writeState(Aggregate<I, ?, ?> aggregate) {
        //TODO:2020-03-21:alex.tymchenko: this behavior seems not to be tested.
//        if (!mirrorEnabled) {
//            return;
//        }
        EntityRecord record = toStateRecord(aggregate);
        EntityColumns columns = EntityColumns.of(aggregate.modelClass());
        EntityRecordWithColumns<I> result =
                EntityRecordWithColumns.create(aggregate, columns, record);
        stateStorage.write(result);
    }

    private EntityRecord toStateRecord(Aggregate<I, ?, ?> aggregate) {
        LifecycleFlags flags = aggregate.lifecycleFlags();
        I id = aggregate.id();
        Version version = aggregate.version();

        EntityRecord.Builder builder =
                EntityRecord.newBuilder()
                            .setEntityId(Identifier.pack(id))
                            .setLifecycleFlags(flags)
                            .setVersion(version);
        if (mirrorEnabled) {
            EntityState state = aggregate.state();
            builder.setState(AnyPacker.pack(state));
        }
        return builder.vBuild();
    }

    /**
     * Executes certain kinds of aggregate reads through the {@link MirrorRepository}.
     *
     * <p>Used to optimize performance-heavy operations on the storage.
     *
     * @param <I>
     *         the type of IDs of aggregates managed by this storage
     */
    private static class Mirror<I> {

        private final MirrorRepository mirrorRepository;
        private final TypeUrl stateType;
        private final boolean multitenant;

        private Mirror(MirrorRepository repository, TypeUrl stateType, boolean multitenant) {
            this.mirrorRepository = repository;
            this.stateType = stateType;
            this.multitenant = multitenant;
        }

        /**
         * Performs a storage {@code index} operation.
         *
         * @return distinct aggregate IDs
         */
        @SuppressWarnings("unchecked") // Ensured logically.
        private Iterator<I> index() {
            Iterator<I> result = evaluateForCurrentTenant(() -> {
                CompositeFilter allOfType = all(eq(TYPE_COLUMN_NAME, stateType.value()));
                TargetFilters filters = TargetFilters
                        .newBuilder()
                        .addFilter(allOfType)
                        .vBuild();
                Iterator<MirrorProjection> found =
                        mirrorRepository.find(filters, ResponseFormat.getDefaultInstance());
                Iterator<I> iterator = stream(found)
                        .map(MirrorProjection::id)
                        .map(MirrorId::getValue)
                        .map(id -> (I) AnyPacker.unpack(id))
                        .iterator();
                return iterator;
            });
            return result;
        }

        private <T> T evaluateForCurrentTenant(Supplier<T> operation) {
            TenantAwareRunner runner = TenantAwareRunner.withCurrentTenant(multitenant);
            T result = runner.evaluate(operation);
            return result;
        }
    }
}
