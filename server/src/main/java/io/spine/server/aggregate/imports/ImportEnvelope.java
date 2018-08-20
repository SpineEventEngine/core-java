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

package io.spine.server.aggregate.imports;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.AbstractMessageEnvelope;
import io.spine.core.ActorContext;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.type.MessageClass;

import java.util.Objects;

import static io.spine.base.Identifier.newUuid;

/**
 * The envelope with an event message and {@code ActorContext} to be dispatched
 * to aggregates that {@linkplain io.spine.server.aggregate.Apply#allowImport import events}.
 *
 * @author Alexander Yevsyukov
 */
final class ImportEnvelope extends AbstractMessageEnvelope<StringValue, Message, ActorContext> {

    private final StringValue id;
    private final ActorContext actorContext;
    private final EventClass eventClass;

    ImportEnvelope(Message eventMessage, ActorContext context) {
        super(eventMessage);
        this.id = StringValue.of(newUuid());
        this.eventClass = EventClass.of(eventMessage);
        this.actorContext = context;
    }

    @Override
    public StringValue getId() {
        return id;
    }

    @Override
    public MessageClass getMessageClass() {
        return eventClass;
    }

    /**
     * Does nothing since import has no origin message.
     */
    @Override
    public void setOriginFields(EventContext.Builder builder) {
        // Do nothing.
    }

    @Override
    public Message getMessage() {
        return getOuterObject();
    }

    @Override
    public ActorContext getMessageContext() {
        return actorContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImportEnvelope)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ImportEnvelope envelope = (ImportEnvelope) o;
        return Objects.equals(id, envelope.id) &&
                Objects.equals(actorContext, envelope.actorContext) &&
                Objects.equals(eventClass, envelope.eventClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, actorContext, eventClass);
    }
}
