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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.SPI;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.server.event.EventFilter;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.server.Identifiers.idToString;

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

    /**
     * Converts EventStorageRecord to Event.
     */
    protected static Event toEvent(EventStorageRecord record) {
        final Event.Builder builder = Event.newBuilder()
                .setMessage(record.getMessage())
                .setContext(record.getContext());
        return builder.build();
    }

    /**
     * Converts EventStorageRecords to Events.
     */
    @VisibleForTesting
    /* package */ static List<Event> toEventList(List<EventStorageRecord> records) {
        return Lists.transform(records, TO_EVENT);
    }

    /**
     * Converts EventStorageRecords to Events.
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    @VisibleForTesting
    /* package */ static List<Event> toEventList(EventStorageRecord... records) {
        return Lists.transform(ImmutableList.copyOf(records), TO_EVENT);
    }

    /**
     * Converts {@code EventStorageRecord}s to {@code Event}s.
     */
    protected static Iterator<Event> toEventIterator(Iterator<EventStorageRecord> records) {
        return Iterators.transform(records, TO_EVENT);
    }

    @Override
    public void write(EventId id, Event event) {
        checkNotClosed();
        final String idString = id.getUuid();
        checkArgument(!idString.isEmpty(), "Event ID must not be empty.");
        checkNotNull(event);

        final EventStorageRecord storeRecord = toEventStorageRecord(idString, event);
        writeInternal(storeRecord);
    }

    @Nullable
    @Override
    public Event read(EventId id) {
        checkNotClosed();
        checkNotNull(id);

        final EventStorageRecord record = readInternal(id);
        if (record == null) {
            return null;
        }
        final Event result = toEvent(record);
        return result;
    }

    /**
     * Creates storage record for the passed {@link Event}.
     */
    @VisibleForTesting
    /* package */ static EventStorageRecord toEventStorageRecord(String eventId, Event event) {
        final Any message = event.getMessage();
        final EventContext context = event.getContext();
        final TypeName typeName = TypeName.ofEnclosed(message);
        final Message aggregateId = fromAny(context.getAggregateId());
        final String aggregateIdString = idToString(aggregateId);
        final EventStorageRecord.Builder builder = EventStorageRecord.newBuilder()
                .setTimestamp(context.getTimestamp())
                .setEventType(typeName.nameOnly())
                .setAggregateId(aggregateIdString)
                .setEventId(eventId)
                .setMessage(message)
                .setContext(context);
        return builder.build();
    }

    /**
     * Returns iterator through events matching the passed query.
     *
     * @param query a filtering query
     * @return iterator instance
     */
    public abstract Iterator<Event> iterator(EventStreamQuery query);

    /**
     * Writes record into the storage.
     *
     * @param record the record to write
     */
    protected abstract void writeInternal(EventStorageRecord record);

    /**
     * Reads storage format record.
     * @param eventId the ID of the event to read
     * @return the record instance of null if there's not record with such ID
     */
    @Nullable
    protected abstract EventStorageRecord readInternal(EventId eventId);

    /**
     * The predicate for filtering {@code Event} instances by
     * {@link EventStreamQuery}.
     */
    public static class MatchesStreamQuery implements Predicate<Event> {

        private final EventStreamQuery query;
        private final Predicate<Event> timePredicate;

        @SuppressWarnings({"MethodWithMoreThanThreeNegations", "IfMayBeConditional"})
        public MatchesStreamQuery(EventStreamQuery query) {
            this.query = query;

            final Timestamp after = query.getAfter();
            final Timestamp before = query.getBefore();

            final boolean afterSpecified = query.hasAfter();
            final boolean beforeSpecified = query.hasBefore();

            //noinspection IfStatementWithTooManyBranches
            if (afterSpecified && !beforeSpecified) {
                this.timePredicate = new Events.IsAfter(after);
            } else if (!afterSpecified && beforeSpecified) {
                this.timePredicate = new Events.IsBefore(before);
            } else if (afterSpecified /* && beforeSpecified is true here too */){
                this.timePredicate = new Events.IsBetween(after, before);
            } else { // No timestamps specified.
                this.timePredicate = Predicates.alwaysTrue();
            }
        }

        @Override
        public boolean apply(@Nullable Event input) {
            if (!timePredicate.apply(input)) {
                return false;
            }

            for (EventFilter filter : query.getFilterList()) {
                final Predicate<Event> filterPredicate = new MatchFilter(filter);
                if (!filterPredicate.apply(input)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * The predicate for filtering events by {@link EventFilter}.
     */
    private static class MatchFilter implements Predicate<Event> {

        private final EventFilter filter;
        private final TypeName eventType;

        public MatchFilter(EventFilter filter) {
            this.filter = filter;
            this.eventType = TypeName.of(filter.getEventType());
        }

        @Override
        public boolean apply(@Nullable Event event) {
            if (event == null) {
                return false;
            }

            final boolean specifiedEventType = !eventType.value().isEmpty();
            if (specifiedEventType) {
                final Message message = Events.getMessage(event);
                final TypeName actualType = TypeName.of(message);
                if (!eventType.equals(actualType)) {
                    return false;
                }
            }

            final List<Any> aggregateIdList = filter.getAggregateIdList();
            if (aggregateIdList.isEmpty()) {
                return true;
            } else {
                final EventContext context = event.getContext();
                final Any aggregateId = context.getAggregateId();
                final boolean matches = aggregateIdList.contains(aggregateId);
                return matches;
            }
        }
    }
}
