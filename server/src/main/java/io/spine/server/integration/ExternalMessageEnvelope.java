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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.type.AbstractMessageEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * An envelope for the messages produced outside of the current Bounded Context.
 */
public final class ExternalMessageEnvelope
        extends AbstractMessageEnvelope<Message, ExternalMessage, ActorContext> {

    /** An identifier of the original message (for example, event ID). */
    private final Message id;

    /** An original message (e.g. instance of {@code io.spine.sample.TaskCreated}). */
    private final Message message;

    /** A message class of the original message (for example, {@code io.spine.sample.TaskCreated} class). */
    private final MessageClass messageClass;

    /** An actor context representing the environment in which the original message was created. */
    private final ActorContext actorContext;

    private ExternalMessageEnvelope(ExternalMessage externalMessage, Message originalMessage) {
        super(externalMessage);

        this.id = externalMessage.getId();
        this.message = originalMessage;
        this.messageClass = ExternalMessageClass.of(this.message.getClass());
        this.actorContext = externalMessage.getActorContext();
    }

    /**
     * Creates a new instance of {@code ExternalMessageEnvelope} from the {@link ExternalMessage}
     * instance and the message transferred inside the {@code ExternalMessage} such as
     * a {@code io.spine.sample.TaskCreated} event message.
     *
     * <p>This factory method provides an optimal performance of
     * the {@code ExternalMessageEnvelope} creation. It allows to avoid unpacking the original
     * message from the {@code ExternalMessage} instance.
     *
     * @param externalMessage the instance of {@code ExternalMessage} to wrap into an envelope
     * @param originalMessage the message instance, which was originally transferred inside the
     *                        {@code externalMessage} such as a {@code io.spine.sample.TaskCreated}
     *                        event message.
     * @return the new instance of external message envelope.
     */
    public static ExternalMessageEnvelope of(ExternalMessage externalMessage,
                                             Message originalMessage) {
        checkNotNull(externalMessage);
        return new ExternalMessageEnvelope(externalMessage, originalMessage);
    }

    @Override
    public Message id() {
        return id;
    }

    /**
     * Obtains an originally transferred message. For instance, {@code io.spine.sample.TaskCreated}
     * event message may returned for an external event transferred inside
     * of this envelope instance.
     *
     * @return the instance of origin message
     */
    @Override
    public Message message() {
        return message;
    }

    /**
     * Obtains a message class of an originally transferred message such as
     * {@code io.spine.sample.TaskCreated} class.
     *
     * @return the event message
     * @see #message()
     * @see #of(ExternalMessage, Message)
     */
    @Override
    public MessageClass messageClass() {
        return messageClass;
    }

    @Override
    public ActorContext context() {
        return actorContext;
    }

    /**
     * Converts this instance to an envelope of the external event.
     */
    public EventEnvelope toEventEnvelope() {
        ExternalMessage externalMessage = outerObject();
        Event event = unpack(externalMessage.getOriginalMessage(), Event.class);
        EventEnvelope result = EventEnvelope.of(event);
        return result;
    }

    /**
     * This method is not supported and always throws {@link UnsupportedOperationException}.
     *
     * <p> This should never happen as no event is caused directly by an {@code ExternalMessage}.
     *
     * <p> Instead, the external messages are consumed by an anti-corruption layer such as
     * external reactor or subscriber methods in the destination Bounded Context.
     * 
     * @param builder not used
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setOriginFields(EventContext.Builder builder) {
        throw newIllegalStateException(
                "An external message like this (%s) may not be a direct origin of any event.",
                this
        );
    }
}
