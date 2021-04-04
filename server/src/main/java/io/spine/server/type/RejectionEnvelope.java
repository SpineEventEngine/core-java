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

import io.spine.base.CommandMessage;
import io.spine.base.RejectionMessage;
import io.spine.base.RejectionThrowable;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.RejectionEventContext;
import io.spine.core.TenantId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.event.RejectionFactoryKt.reject;

/**
 * The holder of a rejection {@code Event} which provides convenient access to its properties.
 */
public final class RejectionEnvelope
        extends AbstractMessageEnvelope<EventId, Event, EventContext>
        implements SignalEnvelope<EventId, Event, EventContext> {

    private final EventEnvelope delegate;

    private RejectionEnvelope(Event rejection) {
        super(rejection);
        this.delegate = EventEnvelope.of(rejection);
    }

    private RejectionEnvelope(EventEnvelope delegate) {
        super(delegate.outerObject());
        this.delegate = delegate;
    }

    /**
     * Creates a new instance from the given rejection event envelope.
     */
    public static RejectionEnvelope from(EventEnvelope delegate) {
        checkArgument(delegate.isRejection());
        return new RejectionEnvelope(delegate);
    }

    /**
     * Creates an wrapped instance of a rejection event from the rejected command and
     * a {@link Throwable} caused by the {@link RejectionThrowable}.
     *
     * @param origin
     *         the rejected command
     * @param throwable
     *         the caught error
     * @return new instance of {@code Rejection}
     * @throws IllegalArgumentException
     *         if the given {@link Throwable} is not caused by
     *         a {@link RejectionThrowable}
     */
    public static RejectionEnvelope from(Command origin, Throwable throwable) {
        checkNotNull(origin);
        checkNotNull(throwable);
        Event rejection = reject(origin, throwable);
        return new RejectionEnvelope(rejection);
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

    private RejectionEventContext rejectionContext() {
        return context().getRejection();
    }

    /**
     * Obtains the command which cased the rejection.
     *
     * @return the rejected command
     */
    public Command origin() {
        return rejectionContext().getCommand();
    }

    /**
     * Obtains the message of the command which cased the rejection.
     *
     * @return the rejected command message
     */
    public CommandMessage originMessage() {
        CommandMessage commandMessage = origin().enclosedMessage();
        return commandMessage;
    }
}
