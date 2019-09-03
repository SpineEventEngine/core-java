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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.transport.PublisherHub;
import io.spine.server.type.EventClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * An adapter between the {@link IntegrationBus} and the {@link EventBus}.
 */
final class BusAdapter {

    /**
     * The wrapped local event bus.
     */
    private final EventBus targetBus;

    /**
     * The name of the bounded context, to which the wrapped local bus belongs.
     */
    private final BoundedContextName boundedContextName;

    /**
     * The publisher hub, used publish the messages dispatched from the local buses to external
     * collaborators.
     */
    private final PublisherHub publisherHub;

    private BusAdapter(Builder builder) {
        this.targetBus = builder.targetBus;
        this.boundedContextName = builder.boundedContextName;
        this.publisherHub = builder.publisherHub;
    }

    /**
     * Wraps a given {@code ExternalMessage} into an {@link ExternalMessageEnvelope}
     * to allow its processing inside of the {@code IntegrationBus}.
     *
     * @param message
     *         an external message to wrap
     * @return an {@code ExternalMessageEnvelope}, containing the given message
     */
    ExternalMessageEnvelope toExternalEnvelope(ExternalMessage message) {
        Message originalMessage = unpack(message.getOriginalMessage());
        return ExternalMessageEnvelope.of(message, originalMessage);
    }

    /**
     * Creates a dispatcher suitable for the wrapped local bus, dispatching the messages of
     * the given class.
     *
     * <p>The created dispatcher is serving as a listener, notifying the {@code IntegrationBus}
     * of the messages, that are requested by the collaborators outside of this bounded context.
     *
     * @param messageClass
     *         the class of message to be dispatched by the created dispatcher
     * @return a dispatcher for the local bus
     */
    private EventDispatcher createDispatcher(Class<? extends Message> messageClass) {
        @SuppressWarnings("unchecked") // Logically checked.
        Class<? extends EventMessage> eventClass = (Class<? extends EventMessage>) messageClass;
        EventClass eventType = EventClass.from(eventClass);
        EventDispatcher result = new DomesticEventPublisher(
                boundedContextName, publisherHub, eventType
        );
        return result;
    }

    void register(Class<? extends Message> messageClass) {
        EventDispatcher dispatcher = createDispatcher(messageClass);
        targetBus.register(dispatcher);
    }

    void unregister(Class<? extends Message> messageClass) {
        EventDispatcher dispatcher = createDispatcher(messageClass);
        targetBus.unregister(dispatcher);
    }

    void dispatch(ExternalMessageEnvelope envelope) {
        Any message = envelope.outerObject().getOriginalMessage();
        if (!message.is(Event.class)) {
            throw messageUnsupported(message);
        } else {
            Event.Builder event = unpack(message, Event.class).toBuilder();
            event.getContextBuilder()
                 .setExternal(true);
            targetBus.post(event.build());
        }
    }

    private static IllegalArgumentException messageUnsupported(Any any) {
        throw newIllegalArgumentException(
                "The message of type `%s` isn't supported.", any.getTypeUrl()
        );
    }

    static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for bus adapters.
     */
    static final class Builder {

        private EventBus targetBus;
        private BoundedContextName boundedContextName;
        private PublisherHub publisherHub;

        Builder setPublisherHub(PublisherHub publisherHub) {
            this.publisherHub = checkNotNull(publisherHub);
            return this;
        }

        Builder setTargetBus(EventBus targetBus) {
            this.targetBus = checkNotNull(targetBus);
            return this;
        }

        Builder setBoundedContextName(BoundedContextName boundedContextName) {
            this.boundedContextName = checkNotNull(boundedContextName);
            return this;
        }

        public BusAdapter build() {
            checkNotNull(targetBus);
            checkNotNull(boundedContextName);
            checkNotNull(publisherHub);

            BusAdapter result = new BusAdapter(this);
            return result;
        }
    }
}
