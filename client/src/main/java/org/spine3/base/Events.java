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
package org.spine3.base;

import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Timestamps2;
import org.spine3.protobuf.TypeName;
import org.spine3.users.UserId;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;

/**
 * Utility class for working with {@link Event} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public class Events {

    private Events() {
        // Prevent instantiation of this utility class.
    }

    /** Compares two events by their timestamps. */
    private static final Comparator<Event> EVENT_TIMESTAMP_COMPARATOR = new Comparator<Event>() {
        @Override
        public int compare(Event o1, Event o2) {
            final Timestamp timestamp1 = getTimestamp(o1);
            final Timestamp timestamp2 = getTimestamp(o2);
            return Timestamps2.compare(timestamp1, timestamp2);
        }
    };

    /** Generates a new random UUID-based {@code EventId}. */
    public static EventId generateId() {
        final String value = Identifiers.newUuid();
        return EventId.newBuilder()
                      .setUuid(value)
                      .build();
    }

    /**
     * Sorts the given event record list by the event timestamps.
     *
     * @param events the event record list to sort
     */
    public static void sort(List<Event> events) {
        checkNotNull(events);
        Collections.sort(events, eventComparator());
    }

    /**
     * Returns comparator which compares events by their timestamp
     * in chronological order.
     */
    public static Comparator<Event> eventComparator() {
        return EVENT_TIMESTAMP_COMPARATOR;
    }

    /**
     * Obtains the timestamp of the event.
     */
    public static Timestamp getTimestamp(Event event) {
        checkNotNull(event);
        final Timestamp result = event.getContext().getTimestamp();
        return result;
    }

    /**
     * Creates a new {@code Event} instance.
     *
     * @param messageOrAny the event message or {@code Any} containing the message
     * @param context      the event context
     * @return created event instance
     */
    public static Event createEvent(Message messageOrAny, EventContext context) {
        checkNotNull(messageOrAny);
        checkNotNull(context);
        final Any packed = (messageOrAny instanceof Any)
                           ? (Any)messageOrAny
                           : pack(messageOrAny);
        final Event result = Event.newBuilder()
                                  .setMessage(packed)
                                  .setContext(context)
                                  .build();
        return result;
    }

    /**
     * Creates {@code Event} instance for import or integration operations.
     *
     * @param event      the event message
     * @param producerId the ID of an entity which is generating the event
     * @return event with data from an external source
     */
    public static Event createImportEvent(Message event, Message producerId) {
        checkNotNull(event);
        checkNotNull(producerId);
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
        checkNotNull(event);
        final Any any = event.getMessage();
        final M result = unpack(any);
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
        checkNotNull(context);
        final CommandContext commandContext = checkNotNull(context).getCommandContext();
        return commandContext.getActor();
    }

    /**
     * Obtains event producer ID from the passed {@code EventContext} and casts it to the
     * {@code <I>} type.
     *
     * @param context the event context to to get the event producer ID
     * @param <I>     the type of the producer ID wrapped in the passed {@code EventContext}
     * @return producer ID
     */
    public static <I> I getProducer(EventContext context) {
        checkNotNull(context);
        final Object aggregateId = Identifiers.idFromAny(context.getProducerId());
        @SuppressWarnings("unchecked") // It is the caller's responsibility to know the type of the wrapped ID.
        final I id = (I) aggregateId;
        return id;

    }

    /**
     * Creates {@code EventContext} instance for an event generated during data import.
     *
     * <p>The method does not set {@code CommandContext} because there was no command.
     *
     * <p>The {@code version} attribute is not populated either.
     * It is the responsibility of the target aggregate to populate the missing fields.
     *
     * @param producerId the ID of the producer which generates the event
     * @return new instance of {@code EventContext} for the imported event
     */
    public static EventContext createImportEventContext(Message producerId) {
        checkNotNull(producerId);
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(generateId())
                                                         .setTimestamp(getCurrentTime())
                                                         .setProducerId(pack(producerId));
        return builder.build();
    }

    /**
     * Verifies if the enrichment is not disabled in the passed event.
     */
    public static boolean isEnrichmentEnabled(Event event) {
        checkNotNull(event);
        final EventContext context = event.getContext();
        final boolean isEnabled =
                context.getEnrichment()
                       .getModeCase() != Enrichment.ModeCase.DO_NOT_ENRICH;
        return isEnabled;
    }

    /**
     * Returns all enrichments from the context.
     *
     * @param context a context to get enrichments from
     * @return an optional of enrichments
     */
    public static Optional<Enrichment.Container> getEnrichments(EventContext context) {
        checkNotNull(context);
        if (context.getEnrichment()
                   .getModeCase() == Enrichment.ModeCase.CONTAINER) {
            return Optional.of(context.getEnrichment().getContainer());
        }
        return Optional.absent();
    }

    /**
     * Return a specific enrichment from the context.
     *
     * @param enrichmentClass a class of the event enrichment
     * @param context         a context to get an enrichment from
     * @param <E>             a type of the event enrichment
     * @return an optional of the enrichment
     */
    public static <E extends Message> Optional<E> getEnrichment(Class<E> enrichmentClass,
                                                                EventContext context) {
        checkNotNull(enrichmentClass);
        checkNotNull(context);
        final Optional<Enrichment.Container> value = getEnrichments(context);
        if (!value.isPresent()) {
            return Optional.absent();
        }
        final Enrichment.Container enrichments = value.get();
        final String typeName = TypeName.of(enrichmentClass);
        final Any any = enrichments.getItemsMap()
                                   .get(typeName);
        if (any == null) {
            return Optional.absent();
        }
        final E result = unpack(any);
        return Optional.fromNullable(result);
    }
}
