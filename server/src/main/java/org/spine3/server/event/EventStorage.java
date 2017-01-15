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
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
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
import org.spine3.base.FieldFilter;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.reflect.Classes;
import org.spine3.server.storage.AbstractStorage;
import org.spine3.server.storage.EventStorageRecord;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.protobuf.TypeUrl.ofEnclosed;
import static org.spine3.validate.Validate.checkNotEmptyOrBlank;
import static org.spine3.validate.Validate.checkPositive;
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
    public Event read(EventId id) {
        checkNotClosed();
        checkNotNull(id);

        final EventStorageRecord record = readRecord(id);
        if (record == null) {
            return Event.getDefaultInstance();
        }
        final Event result = toEvent(record);
        return result;
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
    protected abstract void writeRecord(EventStorageRecord record);

    /**
     * Reads storage format record.
     *
     * @param eventId the ID of the event to read
     * @return the record instance of null if there's not record with such ID
     */
    @Nullable
    protected abstract EventStorageRecord readRecord(EventId eventId);

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
        final Timestamp timestamp = checkPositive(context.getTimestamp(), "event time");
        final EventStorageRecord.Builder builder = EventStorageRecord.newBuilder()
                                                                     .setTimestamp(timestamp)
                                                                     .setEventType(eventType)
                                                                     .setProducerId(producerId)
                                                                     .setEventId(eventIdString)
                                                                     .setMessage(message)
                                                                     .setContext(context);
        return builder.build();
    }

    /**
     * The predicate for filtering {@code Event} instances by
     * {@link EventStreamQuery}.
     */
    public static class MatchesStreamQuery implements Predicate<Event> {

        private final EventStreamQuery query;
        private final Predicate<Event> timePredicate;

        @SuppressWarnings("IfMayBeConditional")
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
            } else if (afterSpecified /* && beforeSpecified is true here too */) {
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
            final List<EventFilter> filterList = query.getFilterList();
            if (filterList.isEmpty()) {
                return true; // The time range matches, and no filters specified.
            }
            // Check if one of the filters matches. If so, the event matches.
            for (EventFilter filter : filterList) {
                final Predicate<Event> filterPredicate = new MatchFilter(filter);
                if (filterPredicate.apply(input)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** The predicate for filtering events by {@link EventFilter}. */
    private static class MatchFilter implements Predicate<Event> {

        /**
         * The type URL of events to accept.
         *
         * <p>If null, all events are accepted.
         */
        @Nullable
        private final TypeUrl eventTypeUrl;

        /**
         * The list of aggregate IDs of which events to accept.
         *
         * <p>If null, all IDs are accepted.
         */
        @Nullable
        private final List<Any> aggregateIds;

        private final Collection<FieldFilter> eventFieldFilters;
        private final Collection<FieldFilter> contextFieldFilters;

        private static final Function<Any, Message> ANY_UNPACKER = new Function<Any, Message>() {
            @Nullable
            @Override
            public Message apply(@Nullable Any input) {
                if (input == null) {
                    return null;
                }

                return AnyPacker.unpack(input);
            }
        };

        private MatchFilter(EventFilter filter) {
            final String eventType = filter.getEventType();
            this.eventTypeUrl = eventType.isEmpty()
                                ? null
                                : TypeUrl.of(eventType);
            final List<Any> aggregateIdList = filter.getAggregateIdList();
            this.aggregateIds = aggregateIdList.isEmpty()
                                ? null
                                : aggregateIdList;
            this.eventFieldFilters = filter.getEventFieldFilterList();
            this.contextFieldFilters = filter.getContextFieldFilterList();
        }

        @SuppressWarnings("MethodWithMoreThanThreeNegations") // OK as we want tracability of exits.
        @Override
        public boolean apply(@Nullable Event event) {
            if (event == null) {
                return false;
            }

            final Message message = Events.getMessage(event);
            final EventContext context = event.getContext();

            if (!checkEventType(message)) {
                return false;
            }

            if (!checkAggregateIds(context)) {
                return false;
            }

            if (!checkEventFields(message)) {
                return false;
            }

            if (!checkContextFields(context)) {
                return false;
            }

            return true;
        }

        private boolean checkAggregateIds(EventContext context) {
            if (aggregateIds == null) {
                return true;
            }
            final Any aggregateId = context.getProducerId();
            final boolean result = aggregateIds.contains(aggregateId);
            return result;
        }

        private boolean checkEventType(Message message) {
            final TypeUrl actualTypeUrl = TypeUrl.of(message);
            if (eventTypeUrl == null) {
                return true;
            }
            final boolean result = actualTypeUrl.equals(eventTypeUrl);
            return result;
        }

        private boolean checkContextFields(EventContext context) {
            for (FieldFilter filter : contextFieldFilters) {
                final boolean matchesFilter = checkFields(context, filter);
                if (!matchesFilter) {
                    return false;
                }
            }
            return true;
        }

        private boolean checkEventFields(Message message) {
            for (FieldFilter filter : eventFieldFilters) {
                final boolean matchesFilter = checkFields(message, filter);
                if (!matchesFilter) {
                    return false;
                }
            }
            return true;
        }

        private static boolean checkFields(
                Message object,
                @SuppressWarnings("TypeMayBeWeakened") /*BuilderOrType interface*/ FieldFilter filter) {
            final String fieldPath = filter.getFieldPath();
            final String fieldName = fieldPath.substring(fieldPath.lastIndexOf('.') + 1);
            checkArgument(!Strings.isNullOrEmpty(fieldName), "Field filter " + filter.toString() + " is invalid");

            final Collection<Any> expectedAnys = filter.getValueList();
            final Collection<Message> expectedValues = Collections2.transform(expectedAnys, ANY_UNPACKER);
            Message actualValue;

            try {
                final Method getter = Classes.getGetterForField(object.getClass(), fieldName);
                actualValue = (Message) getter.invoke(object);
                if (actualValue instanceof Any) {
                    actualValue = AnyPacker.unpack((Any) actualValue);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                // Wrong Message class -> does not satisfy the criteria
                return false;
            }

            final boolean result = expectedValues.contains(actualValue);
            return result;
        }
    }
}
