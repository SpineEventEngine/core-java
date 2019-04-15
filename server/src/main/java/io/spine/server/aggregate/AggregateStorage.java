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
import io.spine.annotation.SPI;
import io.spine.base.Identifier;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.storage.AbstractStorage;
import io.spine.server.storage.StorageWithLifecycleFlags;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Timestamps.checkValid;
import static io.spine.core.Events.clearEnrichments;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;

/**
 * An event-sourced storage of aggregate part events and snapshots.
 *
 * @param <I> the type of IDs of aggregates managed by this storage
 */
@SPI
public abstract class AggregateStorage<I>
        extends AbstractStorage<I, AggregateHistory, AggregateReadRequest<I>>
        implements StorageWithLifecycleFlags<I, AggregateHistory, AggregateReadRequest<I>> {

    protected AggregateStorage(boolean multitenant) {
        super(multitenant);
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
     * Forms and returns an {@link AggregateHistory} based on the
     * {@linkplain #historyBackward(AggregateReadRequest) aggregate history}.
     *
     * @param request the aggregate read request based on which to form a record
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
     * <p>NOTE: does not rewrite any events. Several events can be associated with one aggregate ID.
     *
     * @param id     the ID for the record
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
     * <p>Before the storing, {@linkplain io.spine.core.Events#clearEnrichments(Event) enrichments}
     * will be removed from the event.
     *
     * @param id    the aggregate ID
     * @param event the event to write
     */
    void writeEvent(I id, Event event) {
        checkNotClosedAndArguments(id, event);

        Event eventWithoutEnrichments = clearEnrichments(event);
        AggregateEventRecord record = toStorageRecord(eventWithoutEnrichments);
        writeRecord(id, record);
    }

    /**
     * Writes a {@code snapshot} by an {@code aggregateId} to the storage.
     *
     * @param aggregateId an ID of an aggregate of which the snapshot is made
     * @param snapshot    the snapshot of the aggregate
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
        checkNotEmptyOrBlank(eventIdStr, "Event ID");

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

    /**
     * Reads a count of events which were saved to the storage after
     * the last snapshot was created,
     * <strong>or</strong> a count of all events if there were no snapshots yet.
     *
     * @param id an ID of an aggregate
     * @return an even count after the last snapshot
     */
    protected abstract int readEventCountAfterLastSnapshot(I id);

    /**
     * Writes a count of events which were saved to the storage after
     * the last snapshot was created, or a count of all events if there
     * were no snapshots yet.
     *
     * @param id         an ID of an aggregate
     * @param eventCount an even count after the last snapshot
     * @throws IllegalStateException if the storage is closed
     */
    protected abstract void writeEventCountAfterLastSnapshot(I id, int eventCount);

    // Storage implementation API.

    /**
     * Writes the passed record into the storage.
     *
     * @param id     the aggregate ID
     * @param record the record to write
     */
    protected abstract void writeRecord(I id, AggregateEventRecord record);

    /**
     * Creates iterator of aggregate event history with the reverse traversal.
     *
     * <p>Records are sorted by timestamp descending (from newer to older).
     * The iterator is empty if there's no history for the aggregate with passed ID.
     *
     * @param request the read request
     * @return new iterator instance
     */
    protected abstract Iterator<AggregateEventRecord> historyBackward(
            AggregateReadRequest<I> request);
}
