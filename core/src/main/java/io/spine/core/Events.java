/*
 * Copyright 2019, TeamDev. All rights reserved.
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
package io.spine.core;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.protobuf.Messages;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;
import io.spine.type.TypeUrl;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.spine.core.EventContext.OriginCase.EVENT_CONTEXT;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static io.spine.validate.Validate.isDefault;
import static java.util.stream.Collectors.toList;

/**
 * Utility class for working with {@link Event} objects.
 */
@SuppressWarnings("ClassWithTooManyMethods") // Lots of event-related utility methods.
public final class Events {

    /** Compares two events by their timestamps. */
    private static final Comparator<Event> eventComparator = (o1, o2) -> {
        Timestamp timestamp1 = getTimestamp(o1);
        Timestamp timestamp2 = getTimestamp(o2);
        return Timestamps.compare(timestamp1, timestamp2);
    };

    /** The stringifier for event IDs. */
    private static final Stringifier<EventId> idStringifier = new EventIdStringifier();

    static {
        StringifierRegistry.getInstance()
                           .register(idStringifier(), EventId.class);
    }

    /** Prevents instantiation of this utility class. */
    private Events() {
    }

    /**
     * Creates a new {@link EventId} based on random UUID.
     *
     * @return new UUID-based event ID
     * @implNote This method does not use the {@link Identifier#generate(Class)} API because
     *         {@code EventId} does not conform to the contract of {@link io.spine.base.UuidValue}.
     *         This is done so for being able to have event identifiers with non-UUID values.
     */
    public static EventId generateId() {
        String value = UUID.randomUUID()
                           .toString();
        return EventId.newBuilder()
                      .setValue(value)
                      .build();
    }

    /**
     * Sorts the given event record list by the event timestamps.
     *
     * @param events the event record list to sort
     */
    public static void sort(List<Event> events) {
        checkNotNull(events);
        events.sort(eventComparator());
    }

    /**
     * Returns comparator which compares events by their timestamp
     * in chronological order.
     */
    public static Comparator<Event> eventComparator() {
        return eventComparator;
    }

    /**
     * Obtains the timestamp of the event.
     */
    public static Timestamp getTimestamp(Event event) {
        checkNotNull(event);
        Timestamp result = event.getContext()
                                .getTimestamp();
        return result;
    }

    /**
     * Extracts the event message from the passed event.
     *
     * @param event an event to get message from
     */
    public static EventMessage getMessage(Event event) {
        checkNotNull(event);
        Any any = event.getMessage();
        EventMessage result = (EventMessage) unpack(any);
        return result;
    }

    /**
     * Extract event messages from the passed events.
     */
    public static List<? extends EventMessage> toMessages(List<Event> events) {
       checkNotNull(events);
        List<EventMessage> result =
                events.stream()
                      .map(Events::getMessage)
                      .collect(toList());
        return result;
    }

    /**
     * Extracts an event message if the passed instance is an {@link Event} object or {@link Any},
     * otherwise returns the passed message.
     */
    public static EventMessage ensureMessage(Message eventOrMessage) {
        checkNotNull(eventOrMessage);
        if (eventOrMessage instanceof Event) {
            return getMessage((Event) eventOrMessage);
        }
        Message unpacked = Messages.ensureMessage(eventOrMessage);
        return (EventMessage) unpacked;
    }

    /**
     * Obtains the actor user ID from the passed {@code EventContext}.
     *
     * <p>The 'actor' is the user responsible for producing the given event.
     *
     * <p>It is obtained as follows:
     * <ul>
     *     <li>For the events generated from commands, the actor context is taken from the
     *         enclosing command context.
     *     <li>For the event react chain, the command context of the topmost event is used.
     *     <li>For the imported events, the separate import context contains information about an
     *         actor.
     * </ul>
     *
     * <p>If the given event context contains no origin, an {@link IllegalArgumentException} is
     * thrown as it contradicts the Spine validation rules.
     */
    public static UserId getActor(EventContext context) {
        checkNotNull(context);
        ActorContext actorContext = retrieveActorContext(context);
        UserId result = actorContext.getActor();
        return result;
    }

    /**
     * Obtains event producer ID from the passed {@code EventContext}.
     *
     * @param context the event context to to get the event producer ID
     * @return the producer ID
     */
    public static Object getProducer(EventContext context) {
        checkNotNull(context);
        return Identifier.unpack(context.getProducerId());
    }

    /**
     * Obtains the ID of the root command, which lead to this event.
     * 
     * <p> In case the passed {@code Event} instance is a reaction to another {@code Event}, 
     * the identifier of the very first command in this chain is returned.
     *
     * @param event the event to get the root command ID for
     * @return the root command ID
     */
    public static CommandId getRootCommandId(Event event) {
        checkNotNull(event);
        EventContext context = event.getContext();
        CommandId id = context.getRootCommandId();
        return id;
    }

    /**
     * Obtains the stringifier for event IDs.
     */
    public static Stringifier<EventId> idStringifier() {
        return idStringifier;
    }

    /**
     * Ensures that the passed ID is valid.
     *
     * @param id an ID to check
     * @throws IllegalArgumentException if the ID string value is empty or blank
     */
    public static EventId checkValid(EventId id) {
        checkNotNull(id);
        checkNotEmptyOrBlank(id.getValue(), "event ID");
        return id;
    }

    /**
     * Checks whether or not the given event is a rejection event.
     *
     * @param event the event to check
     * @return {@code true} if the given event is a rejection, {@code false} otherwise
     */
    public static boolean isRejection(Event event) {
        checkNotNull(event);
        EventContext context = event.getContext();
        boolean result = context.hasRejection()
                      || !isDefault(context.getRejection());
        return result;
    }

    /**
     * Constructs a new {@link RejectionEventContext} from the given command message and
     * {@link ThrowableMessage}.
     *
     * @param commandMessage   rejected command
     * @param throwableMessage thrown rejection
     * @return new instance of {@code RejectionEventContext}
     */
    public static RejectionEventContext rejectionContext(CommandMessage commandMessage,
                                                         ThrowableMessage throwableMessage) {
        checkNotNull(commandMessage);
        checkNotNull(throwableMessage);

        String stacktrace = getStackTraceAsString(throwableMessage);
        return RejectionEventContext.newBuilder()
                                    .setCommandMessage(pack(commandMessage))
                                    .setStacktrace(stacktrace)
                                    .build();
    }

    /**
     * Obtains a type URL of the event message enclosed by a given {@code Event}.
     */
    public static TypeUrl typeUrl(Event event) {
        String typeUrl = event.getMessage()
                              .getTypeUrl();
        TypeUrl result = TypeUrl.parse(typeUrl);
        return result;
    }

    /**
     * Obtains a {@link TenantId} from the {@linkplain #getActorContext(Event) actor context}
     * of the given {@link Event}.
     *
     * @return a tenant ID from the actor context of the event
     */
    @Internal
    public static TenantId getTenantId(Event event) {
        checkNotNull(event);
        ActorContext actorContext = getActorContext(event);
        return actorContext.getTenantId();
    }

    /**
     * Obtains the actor context of the event.
     *
     * <p>The {@code ActorContext} is retrieved by traversing {@code Event}s context
     * and can be retrieved from the following places:
     * <ul>
     *     <li>the import context of the event;
     *     <li>the actor context of the command context of this event;
     *     <li>the actor context of the command context of the origin event of any depth.
     * </ul>
     *
     * @return the actor context of the wrapped event
     */
    @Internal
    public static ActorContext getActorContext(Event event) {
        checkNotNull(event);
        EventContext eventContext = event.getContext();
        ActorContext result = retrieveActorContext(eventContext);
        return result;
    }

    /**
     * Creates an empty {@link Iterable} over the messages of the type {@code <M>}.
     *
     * <p>This method is useful for returning empty result from reacting methods.
     *
     * @param <M> the type of messages
     * @return empty {@link Iterable}
     */
    public static <M extends EventMessage> Iterable<M> nothing() {
        return ImmutableList.of();
    }

    /**
     * Analyzes the event context and determines if the event has been produced outside
     * of the current {@code BoundedContext}.
     *
     * @param context the context of event
     * @return {@code true} if the event is external, {@code false} otherwise
     */
    @Internal
    public static boolean isExternal(EventContext context) {
        checkNotNull(context);
        return context.getExternal();
    }

    /**
     * Clears enrichments of the specified event.
     *
     * <p>Use this method to decrease a size of an event, if enrichments aren't important.
     *
     * <p>A result won't contain:
     * <ul>
     *     <li>the enrichment from the event context;</li>
     *     <li>the enrichment from the first-level origin.</li>
     * </ul>
     *
     * <p>Enrichments will not be removed from second-level and deeper origins,
     * because it's a heavy performance operation.
     *
     * @param event the event to clear enrichments
     * @return the event without enrichments
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    @Internal
    public static Event clearEnrichments(Event event) {
        EventContext context = event.getContext();
        EventContext.Builder resultContext = context.toBuilder()
                                                          .clearEnrichment();
        EventContext.OriginCase originCase = resultContext.getOriginCase();
        if (originCase == EVENT_CONTEXT) {
            resultContext.setEventContext(context.getEventContext()
                                                 .toBuilder()
                                                 .clearEnrichment());
        }
        Event result = event.toBuilder()
                            .setContext(resultContext)
                            .build();
        return result;
    }

    /**
     * Replaces the event version with the given {@code newVersion}.
     *
     * @param event      original event
     * @param newVersion the version to set
     * @return the copy of the original event but with the new version
     */
    @Internal
    public static Event substituteVersion(Event event, Version newVersion) {
        EventContext newContext = event.getContext()
                                       .toBuilder()
                                       .setVersion(newVersion)
                                       .build();
        Event result = event.toBuilder()
                            .setContext(newContext)
                            .build();
        return result;
    }

    /**
     * The stringifier of event IDs.
     */
    static class EventIdStringifier extends Stringifier<EventId> {

        @Override
        protected String toString(EventId eventId) {
            String result = eventId.getValue();
            return result;
        }

        @Override
        protected EventId fromString(String str) {
            EventId result = EventId.newBuilder()
                                    .setValue(str)
                                    .build();
            return result;
        }
    }

    /**
     * Obtains an actor context for the given event.
     *
     * <p>The context is obtained by traversing the events origin for a valid context source.
     * There can be three sources for the actor context:
     * <ol>
     *     <li>The command context set as the event origin.
     *     <li>The command context of an event which is an origin of this event.
     *     <li>The import context if the event is imported to an aggregate.
     * </ol>
     *
     * <p>If at some point the event origin is not set, an {@link IllegalArgumentException} is
     * thrown as it contradicts the Spine validation rules. See {@link EventContext} proto
     * declaration.
     */
    private static ActorContext retrieveActorContext(EventContext eventContext) {
        ActorContext actorContext = null;
        EventContext ctx = eventContext;

        while (actorContext == null) {
            switch (ctx.getOriginCase()) {
                case COMMAND_CONTEXT:
                    actorContext = ctx.getCommandContext()
                                      .getActorContext();
                    break;
                case EVENT_CONTEXT:
                    ctx = ctx.getEventContext();
                    break;
                case IMPORT_CONTEXT:
                    actorContext = ctx.getImportContext();
                    break;
                case ORIGIN_NOT_SET:
                default:
                    throw newIllegalArgumentException(
                            "The provided event context (id: %s) has no origin defined.",
                            eventContext.getEventId()
                                        .getValue()
                    );
            }
        }
        return actorContext;
    }
}
