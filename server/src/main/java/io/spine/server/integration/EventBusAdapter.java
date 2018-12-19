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
package io.spine.server.integration;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Events.getMessage;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * An adapter for {@link EventBus} to use it along with {@link IntegrationBus}.
 *
 * @author Alex Tymchenko
 */
final class EventBusAdapter extends BusAdapter<EventEnvelope, EventDispatcher<?>> {

    EventBusAdapter(Builder builder) {
        super(builder);
    }

    static Builder builderWith(EventBus eventBus, BoundedContextName boundedContextName) {
        checkNotNull(eventBus);
        checkNotNull(boundedContextName);
        return new Builder(eventBus, boundedContextName);
    }

    @Override
    ExternalMessageEnvelope toExternalEnvelope(ExternalMessage message) {
        Message unpacked = unpack(message.getOriginalMessage());
        Event event = (Event) unpacked;
        ExternalMessageEnvelope result = ExternalMessageEnvelope.of(message, getMessage(event));
        return result;
    }

    @Override
    ExternalMessageEnvelope markExternal(ExternalMessage externalMsg) {
        Any packedEvent = externalMsg.getOriginalMessage();
        Event event = unpack(packedEvent, Event.class);
        Event.Builder eventBuilder = event.toBuilder();
        EventContext modifiedContext = eventBuilder.getContext()
                                                   .toBuilder()
                                                   .setExternal(true)
                                                   .build();

        Event marked = eventBuilder.setContext(modifiedContext)
                                   .build();
        ExternalMessage result = ExternalMessages.of(marked, externalMsg.getBoundedContextName());
        return ExternalMessageEnvelope.of(result, getMessage(event));
    }

    @Override
    boolean accepts(Class<? extends Message> messageClass) {
        return Event.class == messageClass;
    }

    @Override
    EventDispatcher<?> createDispatcher(Class<? extends Message> messageClass) {
        @SuppressWarnings("unchecked") // Logically checked.
        Class<? extends EventMessage> eventClass = (Class<? extends EventMessage>) messageClass;
        EventClass eventType = EventClass.from(eventClass);
        DomesticEventPublisher result = new DomesticEventPublisher(getBoundedContextName(),
                                                                   getPublisherHub(),
                                                                   eventType);
        return result;
    }

    static class Builder extends AbstractBuilder<Builder, EventEnvelope, EventDispatcher<?>> {

        Builder(EventBus eventBus, BoundedContextName boundedContextName) {
            super(eventBus, boundedContextName);
        }

        @Override
        protected EventBusAdapter doBuild() {
            return new EventBusAdapter(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
