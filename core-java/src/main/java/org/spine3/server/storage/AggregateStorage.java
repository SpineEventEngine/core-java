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
import com.google.protobuf.TextFormat;
import org.spine3.base.EventRecord;
import org.spine3.server.AggregateEvents;
import org.spine3.server.AggregateStorageRecord;
import org.spine3.server.Snapshot;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * An event-sourced storage of aggregate root events and snapshots.
 *
 * @author Alexander Yevsyukov
 */
public abstract class AggregateStorage<I> {

    public AggregateEvents load(I aggregateId) {
        Deque<EventRecord> history = Lists.newLinkedList();
        Snapshot snapshot = null;
        final Iterator<AggregateStorageRecord> iterator = historyBackward(aggregateId);
        while (iterator.hasNext() && snapshot == null) {
            AggregateStorageRecord record = iterator.next();

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

    //TODO:2015-09-18:alexander.yevsyukov: The problem is snapshot creation and event time are not sequential.
    // event happened before we create a snapshot because we make a snapshot when we realize we have far too many events.

    public abstract void store(Snapshot snapshot);

    public abstract void store(EventRecord record);

    //TODO:2015-09-18:alexander.yevsyukov: Implement

    // Internal implementation API.

    protected abstract void write(AggregateStorageRecord r);

    protected abstract List<AggregateStorageRecord> read(I id);

    protected abstract Iterator<AggregateStorageRecord> historyBackward(I id);

}
