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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.protobuf.Timestamp;
import io.spine.Identifier;
import io.spine.annotation.SPI;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.storage.AbstractStorage;
import io.spine.server.storage.StorageWithLifecycleFlags;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.protobuf.TextFormat.shortDebugString;
import static com.google.protobuf.util.Timestamps.checkValid;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;

/**
 * An event-sourced storage of aggregate part events and snapshots.
 *
 * @param <I> the type of IDs of aggregates managed by this storage
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class AggregateStorage<I>
        extends AbstractStorage<I, AggregateStateRecord>
        implements StorageWithLifecycleFlags<I, AggregateStateRecord> {

    /**
     * The {@link AggregateRepository#snapshotTrigger snapshot trigger} value
     * of the repository, to which is assigned this storage.
     */
    private int snapshotTrigger = AggregateRepository.DEFAULT_SNAPSHOT_TRIGGER;

    protected AggregateStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * Forms and returns an {@link AggregateStateRecord} based on the
     * {@linkplain #historyBackward(Object) aggregate history}.
     *
     * @param aggregateId the aggregate ID for which to form a record
     * @return the record instance or {@code Optional.absent()} if the
     *         {@linkplain #historyBackward(Object) aggregate history} is empty
     * @throws IllegalStateException if the storage was closed before
     */
    @Override
    public Optional<AggregateStateRecord> read(I aggregateId) {
        checkNotClosed();
        checkNotNull(aggregateId);

        final Deque<Event> history = newLinkedList();
        Snapshot snapshot = null;

        final Iterator<AggregateEventRecord> historyBackward = historyBackward(aggregateId);

        if (!historyBackward.hasNext()) {
            return Optional.absent();
        }

        while (historyBackward.hasNext()
                && snapshot == null) {

            final AggregateEventRecord record = historyBackward.next();

            switch (record.getKindCase()) {
                case EVENT:
                    history.addFirst(record.getEvent());
                    break;
                case SNAPSHOT:
                    snapshot = record.getSnapshot();
                    break;
                case KIND_NOT_SET:
                default:
                    throw newIllegalStateException("Event or snapshot missing in record: \"%s\"",
                                                   shortDebugString(record));
            }
        }

        final AggregateStateRecord.Builder builder = AggregateStateRecord.newBuilder();
        if (snapshot != null) {
            builder.setSnapshot(snapshot);
        }
        builder.addAllEvent(history);

        final AggregateStateRecord result = builder.build();
        checkAggregateStateRecord(result);
        return Optional.of(result);
    }

    /**
     * Ensures that the {@link AggregateStateRecord} is valid.
     *
     * <p>{@link AggregateStateRecord} is considered valid when one of the following is true:
     * <ul>
     *     <li>{@linkplain AggregateStateRecord#getSnapshot() snapshot} is not default;
     *     <li>{@linkplain AggregateStateRecord#getEventList() event list} is not empty.
     * </ul>
     *
     * @param record the record to check
     * @throws IllegalStateException if the {@link AggregateStateRecord} is not valid
     */
    private static void checkAggregateStateRecord(AggregateStateRecord record)
            throws IllegalStateException {
        final boolean snapshotIsNotSet = record.getSnapshot()
                                               .equals(Snapshot.getDefaultInstance());
        if (snapshotIsNotSet && record.getEventList()
                                      .isEmpty()) {
            throw new IllegalStateException("AggregateStateRecord instance should have either "
                                                    + "snapshot or non-empty event list.");
        }
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
    public void write(I id, AggregateStateRecord events) {
        checkNotClosedAndArguments(id, events);

        final List<Event> eventList = events.getEventList();
        checkArgument(!eventList.isEmpty(), "Event list must not be empty.");

        for (final Event event : eventList) {
            final AggregateEventRecord record = toStorageRecord(event);
            writeRecord(id, record);
        }
    }

    /**
     * Writes an event to the storage by an aggregate ID.
     *
     * @param id    the aggregate ID
     * @param event the event to write
     */
    void writeEvent(I id, Event event) {
        checkNotClosedAndArguments(id, event);

        final AggregateEventRecord record = toStorageRecord(event);
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

        final AggregateEventRecord record = toStorageRecord(snapshot);
        writeRecord(aggregateId, record);
    }

    private void checkNotClosedAndArguments(I id, Object argument) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(argument);
    }

    private static AggregateEventRecord toStorageRecord(Event event) {
        checkArgument(event.hasContext(), "Event context must be set.");
        final EventContext context = event.getContext();

        final String eventIdStr = Identifier.toString(event.getId());
        checkNotEmptyOrBlank(eventIdStr, "Event ID");

        checkArgument(event.hasMessage(), "Event message must be set.");

        final Timestamp timestamp = checkValid(context.getTimestamp());

        return AggregateEventRecord.newBuilder()
                                   .setTimestamp(timestamp)
                                   .setEvent(event)
                                   .build();
    }

    private static AggregateEventRecord toStorageRecord(Snapshot snapshot) {
        final Timestamp value = checkValid(snapshot.getTimestamp());
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
     * The iterator is empty if there's no history for the aggregate with passed ID
     *
     * <p>Use {@link #snapshotTrigger} for the history reading optimization.
     * To load an aggregate in the default scenario, required only
     * the last {@link Snapshot} and events occurred after creation of this snapshot.
     * So instead of reading all {@linkplain AggregateEventRecord event records},
     * it is reasonable to read them by batches equal to the snapshot trigger value.
     *
     * @param id aggregate ID
     * @return new iterator instance
     */
    protected abstract Iterator<AggregateEventRecord> historyBackward(I id);

    /**
     * Returns the number of events until a next {@code Snapshot} is made.
     *
     * @return a positive integer value
     */
    @VisibleForTesting
    public int getSnapshotTrigger() {
        return snapshotTrigger;
    }

    /**
     * Changes the number of events between making aggregate snapshots to the passed value.
     *
     * @param snapshotTrigger a positive number of the snapshot trigger
     */
    protected void setSnapshotTrigger(int snapshotTrigger) {
        checkArgument(snapshotTrigger > 0);
        this.snapshotTrigger = snapshotTrigger;
    }
}
