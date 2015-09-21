/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.TextFormat;
import org.spine3.TypeName;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.EventRecord;
import org.spine3.server.Entity;
import org.spine3.server.aggregate.Snapshot;
import org.spine3.util.Events;

import java.util.Deque;
import java.util.Iterator;

/**
 * An event-sourced storage of aggregate root events and snapshots.
 *
 * @param <I> the type of IDs of aggregates managed by this storage
 * @author Alexander Yevsyukov
 */
public abstract class AggregateStorage<I> {

    public AggregateEvents load(I aggregateId) {
        Deque<EventRecord> history = Lists.newLinkedList();
        Snapshot snapshot = null;

        final Iterator<AggregateStorageRecord> historyBackward = historyBackward(aggregateId);
        while (historyBackward.hasNext()
                && snapshot == null) {
            AggregateStorageRecord record = historyBackward.next();
            switch (record.getKindCase()) {
                case EVENT_RECORD:
                    history.addFirst(record.getEventRecord());
                    break;
                case SNAPSHOT:
                    snapshot = record.getSnapshot();
                    break;
                case KIND_NOT_SET:
                    throw new IllegalStateException("Event record or snapshot missing in " + TextFormat.shortDebugString(record));
            }
        }

        AggregateEvents.Builder builder = AggregateEvents.newBuilder();
        if (snapshot != null) {
            builder.setSnapshot(snapshot);
        }
        builder.addAllEventRecord(history);

        return builder.build();
    }

    private static final String SNAPSHOT_TYPE_NAME = Snapshot.getDescriptor().getName();

    public void store(I aggregateId, Snapshot snapshot) {
        AggregateStorageRecord.Builder builder = AggregateStorageRecord.newBuilder()
                .setTimestamp(snapshot.getTimestamp())
                .setAggregateId(Entity.idToString(aggregateId))
                .setEventType(SNAPSHOT_TYPE_NAME)
                .setEventId("") // No event ID for snapshots
                .setVersion(snapshot.getVersion())
                .setSnapshot(snapshot);
        write(builder.build());
    }

    public void store(EventRecord record) {
        final EventContext context = record.getContext();
        final Any event = record.getEvent();
        final String aggregateId = Entity.idToString(context.getAggregateId());
        final EventId eventId = context.getEventId();
        final String eventIdStr = Entity.idToString(eventId);
        final String typeName = TypeName.ofEnclosed(event).nameOnly();
        AggregateStorageRecord.Builder builder = AggregateStorageRecord.newBuilder()
                .setTimestamp(Events.getTimestamp(eventId))
                .setAggregateId(aggregateId)
                .setEventType(typeName)
                .setEventId(eventIdStr)
                .setVersion(context.getVersion())
                .setEventRecord(record);
        write(builder.build());
    }

    // Storage implementation API.

    /**
     * Writes the passed record into the storage.
     *
     * @param r the record to write
     */
    protected abstract void write(AggregateStorageRecord r);

    /**
     * Creates iterator of aggregate event history with the reverse traversal.
     *
     * @param id aggregate ID
     * @return new iterator instance, the iterator is empty if there's no history for the aggregate with passed ID
     */
    protected abstract Iterator<AggregateStorageRecord> historyBackward(I id);

}
