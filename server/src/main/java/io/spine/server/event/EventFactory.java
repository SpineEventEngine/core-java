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

package io.spine.server.event;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.Events;
import io.spine.core.MessageEnvelope;
import io.spine.core.RejectionEventContext;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeName;
import io.spine.validate.ValidationException;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.validate.Validate.checkValid;

/**
 * Produces events in response to a command.
 */
public class EventFactory {

    private final Any producerId;
    private final MessageEnvelope<?, ?, ?> origin;

    protected EventFactory(MessageEnvelope<?, ?, ?> origin, Any producerId) {
        this.origin = origin;
        this.producerId = producerId;
    }

    /**
     * Creates a new event factory for producing events in response to the passed message.
     *
     * @param origin
     *         the message in response to which events will be generated
     * @param producerId
     *         the ID of the entity producing the events
     * @return new event factory
     */
    public static EventFactory on(MessageEnvelope origin, Any producerId) {
        checkNotNull(origin);
        checkNotNull(producerId);
        return new EventFactory(origin, producerId);
    }

    /**
     * Creates an event for the passed event message.
     *
     * <p>The message passed is validated according to the constraints set in its Protobuf
     * definition. In case the message isn't valid, an {@linkplain ValidationException
     * exception} is thrown.
     *
     * <p>In the message is an instance of {@code Any}, it is unpacked for validation.
     *
     * <p>It is recommended to use a corresponding {@linkplain io.spine.validate.ValidatingBuilder
     * ValidatingBuilder} implementation to create a message.
     *
     * @param message
     *         the message of the event
     * @param version
     *         the version of the entity which produces the event
     * @throws ValidationException
     *         if the passed message does not satisfy the constraints
     *         set for it in its Protobuf definition
     */
    public Event createEvent(EventMessage message,
                             @Nullable Version version) throws ValidationException {
        EventContext context = createContext(version);
        return doCreateEvent(message, context);
    }

    /**
     * Creates a rejection event for the passed rejection message.
     *
     * @param message
     *         the rejection message
     * @param version
     *         the version of the event to create
     * @param rejectionContext
     *         the rejection context
     * @return new rejection event
     * @throws ValidationException
     *         if the passed message does not satisfy the constraints
     *         set for it in its Protobuf definition
     * @see #createEvent createEvent(Message, Version) - for general rules of the event
     *         construction
     */
    public Event createRejectionEvent(RejectionMessage message,
                                      @Nullable Version version,
                                      RejectionEventContext rejectionContext)
            throws ValidationException {
        EventContext context = createContext(version, rejectionContext);
        return doCreateEvent(message, context);
    }

    private static Event doCreateEvent(EventMessage message, EventContext context) {
        checkNotNull(message);
        validate(message);     // we must validate it now before emitting the next ID.

        EventId eventId = Events.generateId();
        Event result = createEvent(eventId, message, context);
        return result;
    }

    /**
     * Validates an event message according to their Protobuf definition.
     *
     * <p>If the given {@code messageOrAny} is an instance of {@code Any}, it is unpacked
     * for the validation.
     */
    private static void validate(Message messageOrAny) throws ValidationException {
        Message message = messageOrAny instanceof Any
                          ? AnyPacker.unpack((Any) messageOrAny)
                          : messageOrAny;
        checkArgument(messageOrAny instanceof EventMessage,
                      "%s is not an event type.", TypeName.of(messageOrAny));
        checkValid(message);
    }

    /**
     * Creates a new {@code Event} instance.
     *
     * @param id
     *         the ID of the event
     * @param message
     *         the event message
     * @param context
     *         the event context
     * @return created event instance
     */
    private static Event createEvent(EventId id, EventMessage message, EventContext context) {
        checkNotNull(message);
        checkNotNull(context);
        Any packed = pack(message);
        Event result = Event
                .newBuilder()
                .setId(id)
                .setMessage(packed)
                .setContext(context)
                .build();
        return result;
    }

    private EventContext createContext(@Nullable Version version) {
        EventContext result = newContext(version).build();
        return result;
    }

    private EventContext createContext(@Nullable Version version,
                                       RejectionEventContext rejectionContext) {
        EventContext result =
                newContext(version)
                        .setRejection(rejectionContext)
                        .build();
        return result;
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    private EventContext.Builder newContext(@Nullable Version version) {
        Timestamp timestamp = getCurrentTime();
        EventContext.Builder builder = EventContext
                .newBuilder()
                .setTimestamp(timestamp)
                .setProducerId(producerId);
        origin.setOriginFields(builder);
        if (version != null) {
            builder.setVersion(version);
        }
        return builder;
    }
}
