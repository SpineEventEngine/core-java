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

package io.spine.server.command.model;

import com.google.protobuf.Message;
import io.spine.base.ThrowableMessage;
import io.spine.core.Event;
import io.spine.core.MessageEnvelope;
import io.spine.core.RejectionEventContext;
import io.spine.core.Version;
import io.spine.server.event.EventFactory;
import io.spine.server.model.EventProducer;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * @author Dmytro Dashenkov
 */
final class RejectionCommandHandlerResult extends CommandHandlerMethod.Result {

    private final RejectionEventContext context;

    private RejectionCommandHandlerResult(EventProducer producer,
                                          Object rawMethodResult,
                                          RejectionEventContext context) {
        super(producer, rawMethodResult);
        this.context = context;
    }

    static RejectionCommandHandlerResult of(ThrowableMessage rejection,
                                            RejectionEventContext context,
                                            EventProducer eventProducer) {
        Message thrownMessage = rejection.getMessageThrown();
        return new RejectionCommandHandlerResult(eventProducer, thrownMessage, context);
    }

    @Override
    protected Function<Message, Event> toEvent(MessageEnvelope origin) {
        EventProducer producer = producer();
        Function<Message, Event> function = new ToEvent(producer, origin, context);
        return function;
    }

    /**
     * Converts a rejection event message into an {@link Event}.
     */
    private static final class ToEvent implements Function<Message, Event> {

        private final EventFactory eventFactory;
        private final @Nullable Version version;
        private final RejectionEventContext context;

        private ToEvent(EventProducer producer,
                        MessageEnvelope origin,
                        RejectionEventContext context) {
            this.context = context;
            this.eventFactory = EventFactory.on(origin, producer.getProducerId());
            this.version = producer.getVersion();
        }

        @Override
        public Event apply(Message message) {
            return eventFactory.createRejectionEvent(message, version, context);
        }
    }
}
