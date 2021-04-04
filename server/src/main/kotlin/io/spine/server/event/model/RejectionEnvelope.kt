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

package io.spine.server.event.model;

import io.spine.base.CommandMessage;
import io.spine.base.RejectionMessage;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.TenantId;
import io.spine.server.type.AbstractMessageEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.SignalEnvelope;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The holder of a rejection {@code Event} which provides convenient access to its properties.
 */
final class RejectionEnvelope
        extends AbstractMessageEnvelope<EventId, Event, EventContext>
        implements SignalEnvelope<EventId, Event, EventContext> {

    private final EventEnvelope delegate;

    /**
     * Creates a new instance from the given rejection event envelope.
     *
     * @param delegate an envelope with a rejection event
     */
    RejectionEnvelope(EventEnvelope delegate) {
        super(delegate.outerObject());
        checkArgument(delegate.isRejection());
        this.delegate = delegate;
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

    RejectionMessage rejectionMessage() {
        return message();
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

    /** Obtains the command which caused the rejection. */
    private Command command() {
        return context().getRejection()
                        .getCommand();
    }

    /** Obtains the message of the command which cased the rejection. */
    CommandMessage commandMessage() {
        return command().enclosedMessage();
    }

    /** Obtains the context of the rejected command. */
    CommandContext commandContext() {
        return command().context();
    }
}
