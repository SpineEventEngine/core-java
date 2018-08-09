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

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * @author Dmytro Dashenkov
 */
public final class RejectionEnvelope
        extends AbstractMessageEnvelope<EventId, Event, EventContext>
        implements ActorMessageEnvelope<EventId, Event, EventContext> {

    private final EventEnvelope event;

    private RejectionEnvelope(EventEnvelope event) {
        super(event.getOuterObject());
        this.event = event;
    }

    public static RejectionEnvelope from(EventEnvelope event) {
        checkNotNull(event);
        checkArgument(event.isRejection(), "%s is not a rejection", event.getMessageClass());
        return new RejectionEnvelope(event);
    }

    @Override
    public TenantId getTenantId() {
        return event.getTenantId();
    }

    @Override
    public ActorContext getActorContext() {
        return event.getActorContext();
    }

    @Override
    public EventId getId() {
        return event.getId();
    }

    @Override
    public Message getMessage() {
        return event.getMessage();
    }

    @Override
    public RejectionClass getMessageClass() {
        EventClass eventClass = event.getMessageClass();
        RejectionClass rejectionClass = RejectionClass.of(eventClass.value());
        return rejectionClass;
    }

    @Override
    public EventContext getMessageContext() {
        return event.getMessageContext();
    }

    @Override
    public void setOriginFields(EventContext.Builder builder) {
        event.setOriginFields(builder);
    }

    /**
     * Obtains the origin {@linkplain DispatchedCommand command} of the rejection represented by
     * this envelope.
     *
     * <p>Throws an {@linkplain IllegalStateException} if this event is not a rejection.
     *
     * @return the rejected command
     */
    public DispatchedCommand getOrigin() {
        EventContext context = getMessageContext();
        RejectionEventContext rejectionContext = getMessageContext().getRejection();
        Any commandMessage = rejectionContext.getCommandMessage();
        CommandContext commandContext = context.getCommandContext();
        DispatchedCommand result = DispatchedCommand
                .newBuilder()
                .setMessage(commandMessage)
                .setContext(commandContext)
                .build();
        return result;
    }

    public Message getOriginMessage() {
        RejectionEventContext context = getMessageContext().getRejection();
        Any commandMessage = context.getCommandMessage();
        return unpack(commandMessage);
    }
}
