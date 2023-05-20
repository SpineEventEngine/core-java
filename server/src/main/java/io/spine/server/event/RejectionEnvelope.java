/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.base.RejectionMessage;
import io.spine.base.ThrowableMessage;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.RejectionEventContext;
import io.spine.core.TenantId;
import io.spine.server.type.AbstractMessageEnvelope;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.SignalEnvelope;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.base.Throwables.getStackTraceAsString;

/**
 * The holder of a rejection {@code Event} which provides convenient access to its properties.
 */
public final class RejectionEnvelope
        extends AbstractMessageEnvelope<EventId, Event, EventContext>
        implements SignalEnvelope<EventId, Event, EventContext> {

    /**
     * The default producer ID for rejection events.
     *
     * <p>Represented by a packed {@link com.google.protobuf.StringValue StringValue} of
     * {@code "Unknown"}.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection") // Coincidence
    private static final Any DEFAULT_EVENT_PRODUCER = Identifier.pack("Unknown");

    private final EventEnvelope event;

    private RejectionEnvelope(EventEnvelope event) {
        super(event.outerObject());
        this.event = event;
    }

    /**
     * Creates a new {@code RejectionEnvelope} from the given event.
     *
     * <p>Throws an {@link IllegalArgumentException} if the given event is not a rejection.
     *
     * @param event the rejection event
     * @return new
     */
    public static RejectionEnvelope from(EventEnvelope event) {
        checkNotNull(event);
        checkArgument(event.isRejection(), "`%s` is not a rejection", event.messageClass());
        return new RejectionEnvelope(event);
    }

    /**
     * Creates an instance of {@code Rejection} from the rejected command and a {@link Throwable}
     * caused by the {@link ThrowableMessage}.
     *
     * <p>If the producer is not {@linkplain ThrowableMessage#initProducer(Any) set}, uses
     * the {@link #DEFAULT_EVENT_PRODUCER} as the producer.
     *
     * @param origin    the rejected command
     * @param throwable the caught error
     * @return new instance of {@code Rejection}
     * @throws IllegalArgumentException if the given {@link Throwable} is not caused by
     *                                  a {@link ThrowableMessage}
     */
    public static RejectionEnvelope from(CommandEnvelope origin, Throwable throwable) {
        checkNotNull(origin);
        checkNotNull(throwable);

        ThrowableMessage throwableMessage = unwrap(throwable);
        Event rejectionEvent = produceEvent(origin, throwableMessage);
        EventEnvelope event = EventEnvelope.of(rejectionEvent);

        return from(event);
    }

    private static ThrowableMessage unwrap(Throwable causedByRejection) {
        Throwable cause = getRootCause(causedByRejection);
        boolean correctType = cause instanceof ThrowableMessage;
        checkArgument(correctType);
        ThrowableMessage throwableMessage = (ThrowableMessage) cause;
        return throwableMessage;
    }

    private static Event produceEvent(CommandEnvelope origin, ThrowableMessage throwableMessage) {
        Any producerId = throwableMessage.producerId()
                                         .orElse(DEFAULT_EVENT_PRODUCER);
        EventFactory factory = EventFactory.on(origin, producerId);
        RejectionMessage thrownMessage = throwableMessage.messageThrown();
        RejectionEventContext context = rejectionContext(origin.outerObject(), throwableMessage);
        Event rejectionEvent = factory.createRejectionEvent(thrownMessage, null, context);
        return rejectionEvent;
    }

    /**
     * Constructs a new {@link RejectionEventContext} from the given command message and
     * {@link ThrowableMessage}.
     *
     * @param command
     *         the rejected command
     * @param throwableMessage
     *         the thrown rejection
     * @return the new instance of {@code RejectionEventContext}
     */
    private static RejectionEventContext rejectionContext(Command command,
                                                          ThrowableMessage throwableMessage) {
        checkNotNull(command);
        checkNotNull(throwableMessage);

        String stacktrace = getStackTraceAsString(throwableMessage);
        return RejectionEventContext
                .newBuilder()
                .setCommand(command)
                .setStacktrace(stacktrace)
                .build();
    }

    @Override
    public TenantId tenantId() {
        return event.tenantId();
    }

    @Override
    public EventId id() {
        return event.id();
    }

    @Override
    public RejectionMessage message() {
        return (RejectionMessage) event.message();
    }

    @Override
    public EventClass messageClass() {
        EventClass eventClass = event.messageClass();
        @SuppressWarnings("unchecked") // Checked at runtime.
        Class<? extends RejectionMessage> value =
                (Class<? extends RejectionMessage>) eventClass.value();
        EventClass rejectionClass = EventClass.from(value);
        return rejectionClass;
    }

    @Override
    public EventContext context() {
        return event.context();
    }

    @VisibleForTesting
    public EventEnvelope getEvent() {
        return event;
    }

    /**
     * Obtains the origin command.
     *
     * @return the rejected command
     */
    public Command getOrigin() {
        return context().getRejection()
                        .getCommand();
    }

    /**
     * Obtains the origin command message.
     *
     * @return the rejected command message
     */
    public CommandMessage getOriginMessage() {
        CommandMessage commandMessage = context().getRejection()
                                                 .getCommand()
                                                 .enclosedMessage();
        return commandMessage;
    }
}
