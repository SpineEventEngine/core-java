/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.annotation.SPI;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.protobuf.AnyPacker;
import io.spine.server.storage.AbstractStorage;
import io.spine.server.storage.StorageWithLifecycleFlags;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.system.server.MirrorId;
import io.spine.system.server.MirrorProjection;
import io.spine.system.server.MirrorRepository;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Streams.stream;
import static com.google.protobuf.util.Timestamps.checkValid;
import static io.spine.client.Filters.all;
import static io.spine.client.Filters.eq;
import static io.spine.system.server.MirrorProjection.TYPE_COLUMN_NAME;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;

/**
 * An event-sourced storage of aggregate part events and snapshots.
 *
 * @param <I>
 *         the type of IDs of aggregates managed by this storage
 */
@SPI
public abstract class AggregateStorage<I>
        extends AbstractStorage<I, AggregateHistory, AggregateReadRequest<I>>
        implements StorageWithLifecycleFlags<I, AggregateHistory, AggregateReadRequest<I>> {

    private static final String TRUNCATE_ON_WRONG_SNAPSHOT_MESSAGE =
            "The specified snapshot index is incorrect";

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
    private @Nullable Mirror<I> aggregateMirror;

    protected AggregateStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * Configures an aggregate {@code Mirror} to optimize certain kinds of aggregate reads.
     *
     * @param mirrorRepository
     *         the repository storing mirror {@linkplain MirrorProjection projections}
     * @param stateType
     *         the state type of a stored {@code Aggregate}
     * @throws IllegalStateException
     *         if the state type of a stored {@code Aggregate} is not mirrored by the
     *         {@code MirrorRepository}
     */
    void configureMirror(MirrorRepository mirrorRepository, TypeUrl stateType) {
        checkState(mirrorRepository.isMirroring(stateType),
                   "An aggregate of type `%s` is not mirrored by a `MirrorRepository`.",
                   stateType);
        aggregateMirror = new Mirror<>(mirrorRepository, stateType, isMultitenant());
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
        if (aggregateMirror != null) {
            return aggregateMirror.index();
        }
        return distinctAggregateIds();
    }

    /**
     * Forms and returns an {@link AggregateHistory} based on the
     * {@linkplain #historyBackward(AggregateReadRequest) aggregate history}.
     *
     * @param request
     *         the aggregate read request based on which to form a record
     * @return the record instance or {@code Optional.empty()} if the
     *         {@linkplain #historyBackward(AggregateReadRequest) aggregate history} is empty
     * @throws IllegalStateException if the storage was closed before
     */
    @SuppressWarnings("CheckReturnValue") // calling builder method
    @Override
    public Optional<AggregateHistory> read(AggregateReadRequest<I> request) {
        ReadOperation<I> op = new ReadOperation<>(this, request);
        return op.perform();
    }

    /**
     * Writes events into the storage.
     *
     * <p><b>NOTE</b>: does not rewrite any events. Several events can be associated with one
     * aggregate ID.
     *
     * @param id
     *         the ID for the record
     * @param events non empty aggregate state record to store
     */
    @Override
    public void write(I id, AggregateHistory events) {
        checkNotClosedAndArguments(id, events);

        List<Event> eventList = events.getEventList();
        checkArgument(!eventList.isEmpty(), "Event list must not be empty.");

        for (Event event : eventList) {
            AggregateEventRecord record = toStorageRecord(event);
            writeRecord(id, record);
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
        AggregateEventRecord record = toStorageRecord(eventWithoutEnrichments);
        writeRecord(id, record);
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

        AggregateEventRecord record = toStorageRecord(snapshot);
        writeRecord(aggregateId, record);
    }

    private void checkNotClosedAndArguments(I id, Object argument) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(argument);
    }

    private static AggregateEventRecord toStorageRecord(Event event) {
        checkArgument(event.hasContext(), "Event context must be set.");
        EventContext context = event.context();

        String eventIdStr = Identifier.toString(event.getId());
        checkNotEmptyOrBlank(eventIdStr, "Event ID cannot be empty or blank.");

        checkArgument(event.hasMessage(), "Event message must be set.");

        Timestamp timestamp = checkValid(context.getTimestamp());

        return AggregateEventRecord.newBuilder()
                                   .setTimestamp(timestamp)
                                   .setEvent(event)
                                   .build();
    }

    private static AggregateEventRecord toStorageRecord(Snapshot snapshot) {
        Timestamp value = checkValid(snapshot.getTimestamp());
        return AggregateEventRecord.newBuilder()
                                   .setTimestamp(value)
                                   .setSnapshot(snapshot)
                                   .build();
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
    protected abstract void writeRecord(I id, AggregateEventRecord record);

    /**
     * Creates iterator of aggregate event history with the reverse traversal.
     *
     * <p>Records are sorted by timestamp descending (from newer to older).
     * The iterator is empty if there's no history for the aggregate with passed ID.
     *
     * @param request
     *         the read request
     * @return new iterator instance
     */
    protected abstract Iterator<AggregateEventRecord>
    historyBackward(AggregateReadRequest<I> request);

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
    protected abstract void truncate(int snapshotIndex);

    /**
     * Drops all records older than {@code date} but not newer than the Nth snapshot for each
     * entity.
     */
    protected abstract void truncate(int snapshotIndex, Timestamp date);

    /**
     * Obtains distinct aggregate IDs from the stored event records.
     */
    protected abstract Iterator<I> distinctAggregateIds();

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
