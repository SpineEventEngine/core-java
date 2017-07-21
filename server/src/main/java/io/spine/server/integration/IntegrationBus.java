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
import io.spine.core.ExternalMessageEnvelope;
import io.spine.core.IsSent;
import io.spine.server.bus.Bus;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageTap;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.type.MessageClass;

import java.util.Deque;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dispatches external messages from and to the current bounded context.
 *
 * @author Alex Tymchenko
 */
public class IntegrationBus extends Bus<Message,
                                        ExternalMessageEnvelope,
                                        MessageClass,
                                        ExternalMessageDispatcher> {

    private IntegrationBus(Builder builder) {

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    protected DeadMessageTap<ExternalMessageEnvelope> getDeadMessageHandler() {
        return null;
    }

    @Override
    protected EnvelopeValidator<ExternalMessageEnvelope> getValidator() {
        return null;
    }

    @Override
    protected LocalDispatcherRegistry createRegistry() {
        return new LocalDispatcherRegistry();
    }

    @Override
    protected Deque<BusFilter<ExternalMessageEnvelope>> createFilterChain() {
        return null;
    }

    @Override
    protected IdConverter<ExternalMessageEnvelope> getIdConverter() {
        return null;
    }

    @Override
    protected ExternalMessageEnvelope toEnvelope(Message message) {
        return null;
    }

    @Override
    protected IsSent doPost(ExternalMessageEnvelope envelope) {
        return null;
    }

    @Override
    protected void store(Iterable<Message> messages) {

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
            extends DispatcherRegistry<MessageClass, ExternalMessageDispatcher> {
        @Override
        protected void checkDispatcher(ExternalMessageDispatcher dispatcher)
                throws IllegalArgumentException {
            // Do not call `super()`, as long as we don't want to enforce
            // non-empty message class set for an external message dispatcher.
            checkNotNull(dispatcher);
        }
    }
}
