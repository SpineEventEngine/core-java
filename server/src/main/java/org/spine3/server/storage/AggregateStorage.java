/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage;

import com.google.protobuf.Any;
import org.spine3.SPI;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.server.EntityId;
import org.spine3.server.aggregate.Snapshot;
import org.spine3.type.TypeName;

import javax.annotation.Nonnull;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.protobuf.TextFormat.shortDebugString;
import static org.spine3.base.Identifiers.idToString;

/**
 * An event-sourced storage of aggregate root events and snapshots.
 *
 * @param <I> the type of IDs of aggregates managed by this storage. See {@link EntityId} for supported types
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class AggregateStorage<I> extends AbstractStorage<I, AggregateEvents> {

    private static final String SNAPSHOT_TYPE_NAME = Snapshot.getDescriptor().getName();

    @Nonnull
    @Override
    public AggregateEvents read(I aggregateId) {
        checkNotClosed();
        checkNotNull(aggregateId);

        final Deque<Event> history = newLinkedList();
        Snapshot snapshot = null;

        final Iterator<AggregateStorageRecord> historyBackward = historyBackward(aggregateId);

        while (historyBackward.hasNext()
                && snapshot == null) {

            final AggregateStorageRecord record = historyBackward.next();

            switch (record.getKindCase()) {
                case EVENT:
                    history.addFirst(record.getEvent());
                    break;
                case SNAPSHOT:
                    snapshot = record.getSnapshot();
                    break;
                case KIND_NOT_SET:
                    throw new IllegalStateException("Event or snapshot missing in record: \"" +
                            shortDebugString(record) + '\"');
            }
        }

        final AggregateEvents.Builder builder = AggregateEvents.newBuilder();
        if (snapshot != null) {
            builder.setSnapshot(snapshot);
        }
        builder.addAllEvent(history);

        return builder.build();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if event list is empty
     */
    @Override
    public void write(I id, AggregateEvents events) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(events);
        final List<Event> eventList = events.getEventList();
        checkArgument(!eventList.isEmpty(), "Event list must not be empty.");

        for (final Event event : eventList) {
            final AggregateStorageRecord record = toStorageRecord(event);
            checkRecord(record);
            writeInternal(id, record);
        }
    }

    /**
     * Writes an event to the storage by an aggregate ID.
     *
     * @param id the aggregate ID
     * @param event the event to write
     * @throws IllegalStateException if the storage is closed
     */
    public void writeEvent(I id, Event event) {
        checkNotClosed();
        checkNotNull(id);
        checkNotNull(event);

        final AggregateStorageRecord record = toStorageRecord(event);
        checkRecord(record);
        writeInternal(id, record);
    }

    /**
     * Writes a {@code snapshot} by an {@code aggregateId} to the storage.
     *
     * @param aggregateId an ID of an aggregate of which the snapshot is made
     * @param snapshot the snapshot of the aggregate
     * @throws IllegalStateException if the storage is closed
     */
    public void write(I aggregateId, Snapshot snapshot) {
        checkNotClosed();
        checkNotNull(aggregateId);
        checkNotNull(snapshot);

        final AggregateStorageRecord record = AggregateStorageRecord.newBuilder()
                .setTimestamp(snapshot.getTimestamp())
                .setEventType(SNAPSHOT_TYPE_NAME)
                .setEventId("") // No event ID for snapshots because it's not a domain event.
                .setVersion(snapshot.getVersion())
                .setSnapshot(snapshot)
                .build();
        checkRecord(record);
        writeInternal(aggregateId, record);
    }

    private static AggregateStorageRecord toStorageRecord(Event event) {
        final EventContext context = event.getContext();
        final EventId eventId = context.getEventId();
        final String eventIdStr = idToString(eventId);
        final Any eventMsg = event.getMessage();
        final String typeName = TypeName.ofEnclosed(eventMsg).nameOnly();

        final AggregateStorageRecord.Builder builder = AggregateStorageRecord.newBuilder()
                .setTimestamp(context.getTimestamp())
                .setEventType(typeName)
                .setEventId(eventIdStr)
                .setVersion(context.getVersion())
                .setEvent(event);
        return builder.build();
    }

    private static void checkRecord(AggregateStorageRecord record) {
        checkArgument(record.hasTimestamp(), "Storage record must have a timestamp.");
        final String eventType = record.getEventType();
        checkArgument(!eventType.isEmpty(), "Event type is an empty string.");
        checkArgument(eventType.trim().length() > 0, "Event type is a blank string.");
        final Event event = record.getEvent();
        final Snapshot snapshot = record.getSnapshot();
        checkArgument(event.hasMessage() || snapshot.hasState(), "Record must have an event or a snapshot with a state.");
    }

    // Storage implementation API.

    /**
     * Writes the passed record into the storage.
     *
     * @param id the aggregate ID
     * @param record the record to write
     */
    protected abstract void writeInternal(I id, AggregateStorageRecord record);

    /**
     * Creates iterator of aggregate event history with the reverse traversal.
     * Records are sorted by timestamp descending (from newer to older).
     *
     * @param id aggregate ID
     * @return new iterator instance, the iterator is empty if there's no history for the aggregate with passed ID
     */
    protected abstract Iterator<AggregateStorageRecord> historyBackward(I id);
}
