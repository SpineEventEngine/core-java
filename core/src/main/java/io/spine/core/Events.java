/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.protobuf.Messages;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.spine.core.EventContext.OriginCase.EVENT_CONTEXT;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static io.spine.validate.Validate.isDefault;
import static java.util.stream.Collectors.toList;

/**
 * Utility class for working with {@link Event} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ClassWithTooManyMethods") // OK for this utility class.
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
    private Events() {}

    /**
     * Creates a new {@link EventId} based on random UUID.
     *
     * @return new UUID-based event ID
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
    public static Message getMessage(Event event) {
        checkNotNull(event);
        Any any = event.getMessage();
        Message result = unpack(any);
        return result;
    }

    /**
     * Extract event messages from the passed events.
     */
    public static List<? extends Message> toMessages(List<Event> events) {
       checkNotNull(events);
        List<Message> result =
                events.stream()
                      .map(Events::getMessage)
                      .collect(toList());
        return result;
    }

    /**
     * Extracts an event message if the passed instance is an {@link Event} object or {@link Any},
     * otherwise returns the passed message.
     */
    public static Message ensureMessage(Message eventOrMessage) {
        checkNotNull(eventOrMessage);
        if (eventOrMessage instanceof Event) {
            return getMessage((Event) eventOrMessage);
        }
        return Messages.ensureMessage(eventOrMessage);
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
        CommandContext commandContext = checkNotNull(context).getCommandContext();
        return commandContext.getActorContext()
                             .getActor();
    }

    /**
     * Obtains event producer ID from the passed {@code EventContext} and casts it to the
     * {@code <I>} type.
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
    public static RejectionEventContext rejectionContext(Message commandMessage,
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
     * Obtains a {@link TenantId} from the given {@link Event}.
     *
     * <p>The {@code TenantId} is retrieved by traversing the passed {@code Event}s context. It is
     * stored in the initial {@link CommandContext} and can be retrieved from the events origin 
     * command or rejection context. 
     *
     * @return a tenant ID available by traversing event context back to original command
     *         context or a default empty tenant ID if no tenant ID is found this way
     */
    @Internal
    public static TenantId getTenantId(Event event) {
        checkNotNull(event);

        Optional<CommandContext> commandContext = findCommandContext(event.getContext());

        if (!commandContext.isPresent()) {
            return TenantId.getDefaultInstance();
        }

        TenantId result = Commands.getTenantId(commandContext.get());
        return result;
    }

    /**
     * Obtains a context of the command, which lead to this event.
     *
     * <p> The context is obtained by traversing the events origin for a valid context source. 
     * There can be two sources for the command context:
     * <ol>
     *     <li>The command context set as the event origin.</li>
     *     <li>The command set as a field of a rejection context if an event was generated in a 
     *     response to a rejection.</li>
     * </ol>
     *
     * <p>If at some point the event origin is not set the {@link Optional#empty()} is returned.
     */
    private static Optional<CommandContext> findCommandContext(EventContext eventContext) {
        CommandContext commandContext = null;
        EventContext ctx = eventContext;

        while (commandContext == null) {
            switch (ctx.getOriginCase()) {
                case EVENT_CONTEXT:
                    ctx = ctx.getEventContext();
                    break;
                case COMMAND_CONTEXT:
                    commandContext = ctx.getCommandContext();
                    break;
                case ORIGIN_NOT_SET:
                default:
                    return Optional.empty();
            }
        }

        return Optional.of(commandContext);
    }

    /**
     * Obtains an {@code ActorContext} from the passed {@code EventContext}.
     *
     * <p>Traverses the origin chain stored in the {@code EventContext}.
     *
     * @throws IllegalStateException if the actor context could not be found in the origin chain
     *  of the passed event context
     */
    @Internal
    public static ActorContext getActorContextOrThrow(EventContext eventContext) {
        Optional<CommandContext> optional = findCommandContext(eventContext);
        checkState(optional.isPresent(), "Unable to find origin CommandContext");
        CommandContext commandContext = optional.get();
        ActorContext result = commandContext.getActorContext();
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
    public static <M> Iterable<M> nothing() {
        return ImmutableList.of();
    }

    /**
     * Analyzes the event context and determines if the event has been produced outside
     * of the current BoundedContext
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
}
