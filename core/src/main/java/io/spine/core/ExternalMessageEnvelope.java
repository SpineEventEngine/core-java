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
package io.spine.core;

import com.google.protobuf.Message;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An envelope for the messages, produced outside of the current bounded context.
 *
 * @author Alex Tymchenko
 */
public class ExternalMessageEnvelope extends AbstractMessageEnvelope<Message, Message> {

    private final Message id;
    private final Message message;
    private final MessageClass messageClass;
    private final ActorContext actorContext;

    private ExternalMessageEnvelope(Event event) {
        super(event);

        this.id = event.getId();
        this.message = event.getMessage();
        this.messageClass = EventClass.of(event);
        this.actorContext = event.getContext().getCommandContext().getActorContext();
    }

    public static ExternalMessageEnvelope of(Event event) {
        checkNotNull(event);
        return new ExternalMessageEnvelope(event);
    }

    @Override
    public Message getId() {
        return id;
    }

    @Override
    public Message getMessage() {
        return message;
    }

    @Override
    public MessageClass getMessageClass() {
        return messageClass;
    }

    @Override
    public ActorContext getActorContext() {
        return actorContext;
    }

    @Override
    public void setOriginContext(EventContext.Builder builder) {
        //TODO:2017-07-21:alex.tymchenko: deal with this with no `instanceof`.
    }
}
