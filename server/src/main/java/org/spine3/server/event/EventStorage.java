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

package org.spine3.server.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import org.spine3.SPI;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.server.event.storage.EventStorageRecord;
import org.spine3.server.storage.AbstractStorage;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Timestamps.checkValid;
import static org.spine3.base.Stringifiers.idToString;
import static org.spine3.protobuf.TypeUrl.ofEnclosed;
import static org.spine3.validate.Validate.checkNotEmptyOrBlank;
import static org.spine3.validate.Validate.checkValid;

/**
 * A storage used by {@link EventStore} for keeping event data.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class EventStorage extends AbstractStorage<EventId, Event> {

    private static final Function<EventStorageRecord, Event> TO_EVENT = new Function<EventStorageRecord, Event>() {
        @Override
        public Event apply(@Nullable EventStorageRecord input) {
            if (input == null) {
                return Event.getDefaultInstance();
            }
            final Event result = toEvent(input);
            return result;
        }
    };

    protected EventStorage(boolean multitenant) {
        super(multitenant);
    }

    @Override
    public void write(EventId id, Event event) {
        checkNotClosed();
        checkNotNull(event);

        final EventStorageRecord record = toEventStorageRecord(id, event);
        writeRecord(record);
    }

    @Override
    public Optional<Event> read(EventId id) {
        checkNotClosed();
        checkNotNull(id);

        final Optional<EventStorageRecord> record = readRecord(id);
        if (!record.isPresent()) {
            return Optional.absent();
        }
        final Event result = toEvent(record.get());
        return Optional.of(result);
    }

    /**
     * Returns iterator through events matching the passed query.
     *
     * @param query a filtering query
     * @return iterator instance
     */
    protected abstract Iterator<Event> iterator(EventStreamQuery query);

    /**
     * Writes record into the storage.
     *
     * @param record the record to write
     */
    protected abstract void writeRecord(EventStorageRecord record);

    /**
     * Reads storage format record.
     *
     * @param eventId the ID of the event to read
     * @return the record instance of null if there's not record with such ID
     */
    protected abstract Optional<EventStorageRecord> readRecord(EventId eventId);

    /** Converts EventStorageRecord to Event. */
    protected static Event toEvent(EventStorageRecord record) {
        final Event event = Events.createEvent(record.getMessage(), record.getContext());
        return event;
    }

    /** Converts EventStorageRecords to Events. */
    @VisibleForTesting
    static List<Event> toEventList(List<EventStorageRecord> records) {
        return Lists.transform(records, TO_EVENT);
    }

    /** Converts EventStorageRecords to Events. */
    @SuppressWarnings("OverloadedVarargsMethod")
    @VisibleForTesting
    static List<Event> toEventList(EventStorageRecord... records) {
        return toEventList(ImmutableList.copyOf(records));
    }

    /** Converts {@code EventStorageRecord}s to {@code Event}s. */
    protected static Iterator<Event> toEventIterator(Iterator<EventStorageRecord> records) {
        return Iterators.transform(records, TO_EVENT);
    }

    /** Creates storage record for the passed {@link Event}. */
    @VisibleForTesting
    static EventStorageRecord toEventStorageRecord(EventId eventId, Event event) {
        final String eventIdString = checkValid(eventId).getUuid();
        final Any message = event.getMessage();
        final EventContext context = event.getContext();
        final String eventType = ofEnclosed(message).getTypeName();
        checkNotEmptyOrBlank(eventType, "event type");
        final String producerId = idToString(Events.getProducer(context));
        checkNotEmptyOrBlank(producerId, "producer ID");
        final Timestamp timestamp = checkValid(context.getTimestamp());
        final EventStorageRecord.Builder builder = EventStorageRecord.newBuilder()
                                                                     .setTimestamp(timestamp)
                                                                     .setEventType(eventType)
                                                                     .setProducerId(producerId)
                                                                     .setEventId(eventIdString)
                                                                     .setMessage(message)
                                                                     .setContext(context);
        return builder.build();
    }
}
