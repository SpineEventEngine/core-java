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
package org.spine3.base;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Timestamps;
import org.spine3.protobuf.TypeUrl;
import org.spine3.users.UserId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.spine3.protobuf.Timestamps.isBetween;

/**
 * Utility class for working with {@link Event} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class Events {

    private Events() {
    }

    /** Compares two events by their timestamps. */
    public static final Comparator<Event> EVENT_TIMESTAMP_COMPARATOR = new Comparator<Event>() {
        @Override
        public int compare(Event o1, Event o2) {
            final Timestamp timestamp1 = getTimestamp(o1);
            final Timestamp timestamp2 = getTimestamp(o2);
            return Timestamps.compare(timestamp1, timestamp2);
        }
    };

    /** Generates a new random UUID-based {@code EventId}. */
    public static EventId generateId() {
        final String value = UUID.randomUUID().toString();
        return EventId.newBuilder().setUuid(value).build();
    }

    /**
     * Sorts the given event record list by the event timestamps.
     *
     * @param events the event record list to sort
     */
    public static void sort(List<Event> events) {
        Collections.sort(events, EVENT_TIMESTAMP_COMPARATOR);
    }

    /** Obtains the timestamp of the event. */
    public static Timestamp getTimestamp(Event event) {
        final Timestamp result = event.getContext().getTimestamp();
        return result;
    }

    /** Creates a new {@code Event} instance. */
    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    public static Event createEvent(Message event, EventContext context) {
        return createEvent(AnyPacker.pack(event), context);
    }

    /** Creates a new {@code Event} instance. */
    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    public static Event createEvent(Any eventAny, EventContext context) {
        final Event result = Event.newBuilder()
                .setMessage(eventAny)
                .setContext(context)
                .build();
        return result;
    }

    /**
     * Creates {@code Event} instance for import or integration operations.
     *
     * @param event the event message
     * @param producerId the ID of an entity which is generating the event
     * @return event with data from an external source
     */
    public static Event createImportEvent(Message event, Message producerId) {
        final EventContext context = createImportEventContext(producerId);
        final Event result = createEvent(event, context);
        return result;
    }

    /**
     * Extracts the event message from the passed event.
     *
     * @param event an event to get message from
     */
    public static <M extends Message> M getMessage(Event event) {
        final Any any = event.getMessage();
        final M result = AnyPacker.unpack(any);
        return result;
    }

    /**
     * Obtains the actor user ID from the passed {@code EventContext}.
     *
     * <p>The 'actor' is the user who sent the command, which generated the event which context is
     * passed to this method.
     *
     * <p>This is a convenience method for obtaining actor in event subscriber methods.
     */
    public static UserId getActor(EventContext context) {
        final CommandContext commandContext = checkNotNull(context).getCommandContext();
        return commandContext.getActor();
    }

    /**
     * Obtains event producer ID from the passed {@code EventContext} and casts it to the
     * {@code <I>} type.
     *
     * @param context the event context to to get the event producer ID
     * @param <I> the type of the producer ID wrapped in the passed {@code EventContext}
     * @return producer ID
     */
    public static <I> I getProducer(EventContext context) {
        final Object aggregateId = Identifiers.idFromAny(context.getProducerId());
        @SuppressWarnings("unchecked") // It is the caller's responsibility to know the type of the wrapped ID.
        final I id = (I)aggregateId;
        return id;

    }

    /**
     * Creates {@code EventContext} instance for an event generated during data import.
     *
     * <p>The method does not set {@code CommandContext} because there was no command.
     *
     * <p>The {@code version} attribute is not populated either. It is the responsiblity of the
     * target aggregate to populate the missing fields.
     *
     * @param producerId the ID of the producer which generates the event
     * @return new instance of {@code EventContext} for the imported event
     */
    public static EventContext createImportEventContext(Message producerId) {
        checkNotNull(producerId);
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(generateId())
                                                         .setTimestamp(Timestamps.getCurrentTime())
                                                         .setProducerId(AnyPacker.pack(producerId));
        return builder.build();
    }

    /** The predicate to filter event records after some point in time. */
    public static class IsAfter implements Predicate<Event> {

        private final Timestamp timestamp;

        public IsAfter(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean apply(@Nullable Event record) {
            if (record == null) {
                return false;
            }
            final Timestamp ts = getTimestamp(record);
            final boolean result = Timestamps.compare(ts, this.timestamp) > 0;
            return result;
        }
    }

    /** The predicate to filter event records before some point in time. */
    public static class IsBefore implements Predicate<Event> {

        private final Timestamp timestamp;

        public IsBefore(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean apply(@Nullable Event record) {
            if (record == null) {
                return false;
            }

            final Timestamp ts = getTimestamp(record);
            final boolean result = Timestamps.compare(ts, this.timestamp) < 0;
            return result;
        }
    }

    /** The predicate to filter event records within a given time range. */
    public static class IsBetween implements Predicate<Event> {

        private final Timestamp start;
        private final Timestamp finish;

        public IsBetween(Timestamp start, Timestamp finish) {
            checkNotNull(start);
            checkNotNull(finish);
            checkArgument(Timestamps.compare(start, finish) < 0, "`start` must be before `finish`");
            this.start = start;
            this.finish = finish;
        }

        @Override
        public boolean apply(@Nullable Event event) {
            if (event == null) {
                return false;
            }

            final Timestamp ts = getTimestamp(event);
            final boolean result = isBetween(ts, start, finish);
            return result;
        }
    }

    /** Verifies if the enrichment is not disabled in the passed event. */
    public static boolean isEnrichmentEnabled(Event event) {
        final EventContext context = event.getContext();
        final EventContext.EnrichmentModeCase mode = context.getEnrichmentModeCase();
        final boolean isEnabled = mode != EventContext.EnrichmentModeCase.DO_NOT_ENRICH;
        return isEnabled;
    }

    /**
     * Returns all enrichments from the context.
     *
     * @param context a context to get enrichments from
     * @return an optional of enrichments
     */
    public static Optional<Enrichments> getEnrichments(EventContext context) {
        final EventContext.EnrichmentModeCase mode = context.getEnrichmentModeCase();
        if (mode == EventContext.EnrichmentModeCase.ENRICHMENTS) {
            return Optional.of(context.getEnrichments());
        }
        return Optional.absent();
    }

    /**
     * Return a specific enrichment from the context.
     *
     * @param enrichmentClass a class of the event enrichment
     * @param context a context to get an enrichment from
     * @param <E> a type of the event enrichment
     * @return an optional of the enrichment
     */
    public static <E extends Message> Optional<E> getEnrichment(Class<E> enrichmentClass, EventContext context) {
        final Optional<Enrichments> value = getEnrichments(context);
        if (!value.isPresent()) {
            return Optional.absent();
        }
        final Enrichments enrichments = value.get();
        final String typeName = TypeUrl.of(enrichmentClass)
                                       .getTypeName();
        final Any any = enrichments.getMap()
                                   .get(typeName);
        if (any == null) {
            return Optional.absent();
        }
        final E result = unpack(enrichmentClass, any);
        return Optional.fromNullable(result);
    }

    //TODO:2016-06-17:alexander.yevsyukov: Evaluate using this function instead of Messages.fromAny() in general.
    // The below approach may already work.

    private static <T extends Message> T unpack(Class<T> clazz, Any any) {
        final T result;
        try {
            result = any.unpack(clazz);
            return result;
        } catch (InvalidProtocolBufferException e) {
            throw propagate(e);
        }
    }
}
