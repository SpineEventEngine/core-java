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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.ExternalMessageEnvelope;
import io.spine.server.bus.Bus;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageTap;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.type.MessageClass;

import java.util.Deque;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;

/**
 * Dispatches external messages from and to the current bounded context.
 *
 * @author Alex Tymchenko
 */
public class IntegrationBus extends Bus<Message,
                                        ExternalMessageEnvelope,
                                        MessageClass,
                                        ExternalMessageDispatcher<?>> {

    private IntegrationBus(Builder builder) {
        super();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    protected LocalDispatcherRegistry createRegistry() {
        return new LocalDispatcherRegistry();
    }

    @Override
    protected DeadMessageTap<ExternalMessageEnvelope> getDeadMessageHandler() {
        return DeadExternalMessageTap.INSTANCE;
    }

    @Override
    protected EnvelopeValidator<ExternalMessageEnvelope> getValidator() {
        return null;
    }

    @Override
    protected Deque<BusFilter<ExternalMessageEnvelope>> createFilterChain() {
        return newLinkedList();
    }


    @Override
    protected ExternalMessageEnvelope toEnvelope(Message message) {
        return null;
    }

    @Override
    protected Ack doPost(ExternalMessageEnvelope envelope) {
        return null;
    }

    @Override
    protected void store(Iterable<Message> messages) {
    }

    @Override
    public void register(ExternalMessageDispatcher dispatcher) {
        
        super.register(dispatcher);
    }

    public static class Builder
            extends Bus.AbstractBuilder<ExternalMessageEnvelope, Message, Builder> {

        @Override
        public IntegrationBus build() {
            return new IntegrationBus(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    /**
     * A registry of subscribers which {@linkplain io.spine.core.Subscribe#external() subscribe}
     * to handle external messages.
     */
    private static class LocalDispatcherRegistry
            extends DispatcherRegistry<MessageClass, ExternalMessageDispatcher<?>> {
        @Override
        protected void checkDispatcher(ExternalMessageDispatcher dispatcher)
                throws IllegalArgumentException {
            // Do not call `super()`, as long as we don't want to enforce
            // non-empty message class set for an external message dispatcher.
            checkNotNull(dispatcher);
        }
    }

    /**
     * Produces an {@link UnsupportedExternalMessageException} upon capturing an external message,
     * which has no targets to be dispatched to.
     */
    private enum DeadExternalMessageTap implements DeadMessageTap<ExternalMessageEnvelope> {
        INSTANCE;

        @Override
        public UnsupportedExternalMessageException capture(ExternalMessageEnvelope envelope) {
            final Message message = envelope.getMessage();
            final UnsupportedExternalMessageException exception =
                    new UnsupportedExternalMessageException(message);
            return exception;
        }
    }
}
