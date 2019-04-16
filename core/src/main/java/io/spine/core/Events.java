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
import io.spine.annotation.Internal;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.protobuf.Messages;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;

import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static java.util.stream.Collectors.toList;

/**
 * Utility class for working with {@link Event} objects.
 */
public final class Events {

    /** The stringifier for event IDs. */
    private static final Stringifier<EventId> idStringifier = new EventIdStringifier();

    static {
        StringifierRegistry.instance()
                           .register(idStringifier(), EventId.class);
    }

    /** Prevents instantiation of this utility class. */
    private Events() {
    }

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
     * Extract event messages from the passed events.
     */
    public static List<? extends EventMessage> toMessages(List<Event> events) {
        checkNotNull(events);
        List<EventMessage> result =
                events.stream()
                      .map(Event::enclosedMessage)
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
            return ((Event) eventOrMessage).enclosedMessage();
        }
        Message unpacked = Messages.ensureMessage(eventOrMessage);
        return (EventMessage) unpacked;
    }

    //TODO:2019-04-16:alexander.yevsyukov: Move to EventContextMixin
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

    //TODO:2019-04-16:alexander.yevsyukov: Move to EventContextMixin
    /**
     * Obtains event producer ID from the passed {@code EventContext}.
     *
     * @param context
     *         the event context to to get the event producer ID
     * @return the producer ID
     */
    public static Object getProducer(EventContext context) {
        checkNotNull(context);
        return Identifier.unpack(context.getProducerId());
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
     * @param id
     *         an ID to check
     * @throws IllegalArgumentException
     *         if the ID string value is empty or blank
     */
    public static EventId checkValid(EventId id) {
        checkNotNull(id);
        checkNotEmptyOrBlank(id.getValue(), "event ID");
        return id;
    }

    //TODO:2019-04-16:alexander.yevsyukov: Move to ThrowableMessage
    /**
     * Constructs a new {@link RejectionEventContext} from the given command message and
     * {@link ThrowableMessage}.
     *
     * @param commandMessage
     *         rejected command
     * @param throwableMessage
     *         thrown rejection
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
    public static ActorContext actorContextOf(Event event) {
        checkNotNull(event);
        EventContext eventContext = event.context();
        ActorContext result = retrieveActorContext(eventContext);
        return result;
    }

    /**
     * Creates an empty {@link Iterable} over the messages of the type {@code <M>}.
     *
     * <p>This method is useful for returning empty result from reacting methods.
     *
     * @param <M>
     *         the type of messages
     * @return empty {@link Iterable}
     */
    public static <M extends EventMessage> Iterable<M> nothing() {
        return ImmutableList.of();
    }

    /**
     * Analyzes the event context and determines if the event has been produced outside
     * of the current {@code BoundedContext}.
     *
     * @param context
     *         the context of event
     * @return {@code true} if the event is external, {@code false} otherwise
     */
    @Internal
    public static boolean isExternal(EventContext context) {
        checkNotNull(context);
        return context.getExternal();
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
    static ActorContext retrieveActorContext(EventContext eventContext) {
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
