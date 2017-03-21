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

package org.spine3.server.aggregate;

import com.google.common.base.Optional;
import com.google.protobuf.Timestamp;
import org.spine3.annotations.SPI;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.server.storage.AbstractStorage;
import org.spine3.server.storage.StorageWithLifecycleFlags;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.protobuf.TextFormat.shortDebugString;
import static com.google.protobuf.util.Timestamps.checkValid;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.util.Exceptions.newIllegalStateException;
import static org.spine3.validate.Validate.checkNotEmptyOrBlank;

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

    protected AggregateStorage(boolean multitenant) {
        super(multitenant);
    }

    @Override
    public Optional<AggregateStateRecord> read(I aggregateId) {
        checkNotClosed();
        checkNotNull(aggregateId);

        final Deque<Event> history = newLinkedList();
        Snapshot snapshot = null;

        final Iterator<AggregateEventRecord> historyBackward = historyBackward(aggregateId);

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

        return Optional.of(builder.build());
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

        final String eventIdStr = idToString(context.getEventId());
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
     * @param id aggregate ID
     * @return new iterator instance
     */
    protected abstract Iterator<AggregateEventRecord> historyBackward(I id);
}
