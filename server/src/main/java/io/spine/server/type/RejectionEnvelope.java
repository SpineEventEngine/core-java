/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.type;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.base.RejectionMessage;
import io.spine.base.RejectionThrowable;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.TenantId;
import io.spine.server.event.RejectionFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.TextFormat.shortDebugString;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * The holder of a rejection {@code Event} which provides convenient access to its properties.
 */
public final class RejectionEnvelope
        extends AbstractMessageEnvelope<EventId, Event, EventContext>
        implements SignalEnvelope<EventId, Event, EventContext> {

    /**
     * A placeholder for telling that a producer of a rejection is not known.
     *
     * <p>Represented by a packed {@link com.google.protobuf.StringValue StringValue} of
     * {@code "Unknown"}.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection") // Used in another context.
    public static final Any PRODUCER_UNKNOWN = Identifier.pack("Unknown");

    private final EventEnvelope delegate;

    private RejectionEnvelope(Event rejection) {
        super(rejection);
        this.delegate = EventEnvelope.of(rejection);
    }

    /**
     * Creates a new instance from the given rejection event.
     *
     * <p>Throws an {@link IllegalArgumentException} if the passed wrapping object
     *  does not contain a rejection message.
     *
     * @param rejection the rejection event
     * @return new instance
     */
    public static RejectionEnvelope from(Event rejection) {
        checkNotNull(rejection);
        if (!rejection.isRejection()) {
            throw newIllegalArgumentException(
                    "The passed event does not contain a rejection message: `%s`",
                    shortDebugString(rejection)
            );
        }
        return new RejectionEnvelope(rejection);
    }

    /**
     * Creates a new instance from the given rejection event envelope.
     *
     * @deprecated please use {@link #from(Event)}
     */
    @Deprecated
    public static RejectionEnvelope from(EventEnvelope event) {
        return from(event.outerObject());
    }

    /**
     * Creates an wrapped instance of a rejection event from the rejected command and
     * a {@link Throwable} caused by the {@link RejectionThrowable}.
     *
     * <p>If the producer is not {@linkplain RejectionThrowable#initProducer(Any) set}, uses
     * the {@link #PRODUCER_UNKNOWN} as the producer.
     *
     * @param origin    the rejected command
     * @param throwable the caught error
     * @return new instance of {@code Rejection}
     * @throws IllegalArgumentException if the given {@link Throwable} is not caused by
     *                                  a {@link RejectionThrowable}
     */
    public static RejectionEnvelope from(CommandEnvelope origin, Throwable throwable) {
        checkNotNull(origin);
        checkNotNull(throwable);
        RejectionFactory factory = new RejectionFactory(origin.outerObject(), throwable);
        Event rejection = factory.createRejection();
        return from(rejection);
    }


    @Override
    public TenantId tenantId() {
        return delegate.tenantId();
    }

    @Override
    public EventId id() {
        return delegate.id();
    }

    @Override
    public RejectionMessage message() {
        return (RejectionMessage) delegate.message();
    }

    @Override
    public EventClass messageClass() {
        EventClass eventClass = delegate.messageClass();
        @SuppressWarnings("unchecked") // Checked at runtime.
        Class<? extends RejectionMessage> value =
                (Class<? extends RejectionMessage>) eventClass.value();
        EventClass rejectionClass = EventClass.from(value);
        return rejectionClass;
    }

    @Override
    public EventContext context() {
        return delegate.context();
    }

    @VisibleForTesting
    public EventEnvelope getEvent() {
        return delegate;
    }

    /**
     * Obtains the command which cased the rejection.
     *
     * @return the rejected command
     */
    public Command origin() {
        return context().getRejection()
                        .getCommand();
    }

    /**
     * Obtains the command which cased the rejection.
     *
     * @deprecated please use {@link #origin()}.
     */
    @Deprecated
    public Command getOrigin() {
        return origin();
    }

    /**
     * Obtains the message of the command which cased the rejection.
     *
     * @return the rejected command message
     */
    public CommandMessage originMessage() {
        CommandMessage commandMessage =
                context().getRejection()
                         .getCommand()
                         .enclosedMessage();
        return commandMessage;
    }

    /**
     * Obtains the origin command message.
     *
     * @return the rejected command message
     * @deprecated please use {@link #originMessage()}.
     */
    @Deprecated
    public CommandMessage getOriginMessage() {
        return originMessage();
    }
}
