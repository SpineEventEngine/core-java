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
import io.spine.core.BoundedContextName;
import io.spine.server.bus.Bus;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.transport.PublisherHub;
import io.spine.server.type.MessageEnvelope;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract base for adapters of local buses (such as {@code EventBus})
 * to {@code IntegrationBus}.
 *
 * <p>Provides an API for routines, performed by the {@code IntegrationBus} lifecycle.
 *
 * @param <E> the type of envelopes which are handled by the local bus, which is being adapted
 * @param <D> the type of dispatchers suitable for the local bus, which is being adapted
 * @author Alex Tymchenko
 */
abstract class BusAdapter<E extends MessageEnvelope<?, ?, ?>,
                          D extends MessageDispatcher<?, E, ?>> {

    /**
     * The wrapped local bus.
     */
    private final Bus<?, E, ?, D> targetBus;

    /**
     * The name of the bounded context, to which the wrapped local bus belongs.
     */
    private final BoundedContextName boundedContextName;

    /**
     * The publisher hub, used publish the messages dispatched from the local buses to external
     * collaborators.
     */
    private final PublisherHub publisherHub;

    BusAdapter(AbstractBuilder<?, E, D> builder) {
        this.targetBus = builder.targetBus;
        this.publisherHub = builder.publisherHub;
        this.boundedContextName = builder.boundedContextName;
    }

    /**
     * Wraps a given {@code ExternalMessage} into an {@link ExternalMessageEnvelope}
     * to allow its processing inside of the {@code IntegrationBus}.
     *
     * @param message an external message to wrap
     * @return an {@code ExternalMessageEnvelope}, containing the given message
     */
    abstract ExternalMessageEnvelope toExternalEnvelope(ExternalMessage message);

    /**
     * Wraps the given message into an {@link ExternalMessageEnvelope}, marking it as the one
     * received from the collaborators outside of the current bounded context.
     *
     * @param message the message to wrap and mark external
     * @return an {@code ExternalMessageEnvelope}, containing the given message marked external
     */
    abstract ExternalMessageEnvelope markExternal(ExternalMessage message);

    /**
     * Tells whether a message of a given message class is eligible for processing with this
     * bus adapter.
     *
     * @param messageClass a {@code Class} of message
     * @return {@code true} if this bus adapter is able to accept messages of this type,
     * {@code false} otherwise
     */
    abstract boolean accepts(Class<? extends Message> messageClass);

    /**
     * Creates a dispatcher suitable for the wrapped local bus, dispatching the messages of
     * the given class.
     *
     * <p>The created dispatcher is serving as a listener, notifying the {@code IntegrationBus}
     * of the messages, that are requested by the collaborators outside of this bounded context.
     *
     * @param messageCls the class of message to be dispatched by the created dispatcher
     * @return a dispatcher for the local bus
     */
    abstract D createDispatcher(Class<? extends Message> messageCls);

    void register(Class<? extends Message> messageClass) {
        D dispatcher = createDispatcher(messageClass);
        targetBus.register(dispatcher);
    }

    void unregister(Class<? extends Message> messageClass) {
        D dispatcher = createDispatcher(messageClass);
        targetBus.unregister(dispatcher);
    }

    PublisherHub getPublisherHub() {
        return publisherHub;
    }

    BoundedContextName getBoundedContextName() {
        return boundedContextName;
    }

    /**
     * A builder for bus adapters.
     *
     * @param <B> type of the builder
     * @param <E> type of the message envelope
     * @param <D> type of message dispatcher
     */
    abstract static class AbstractBuilder<B extends AbstractBuilder<B, E, D>,
                                          E extends MessageEnvelope<?, ?, ?>,
                                          D extends MessageDispatcher<?, E, ?>> {

        private final Bus<?, E, ?, D> targetBus;
        private final BoundedContextName boundedContextName;

        private PublisherHub publisherHub;

        AbstractBuilder(Bus<?, E, ?, D> targetBus, BoundedContextName boundedContextName) {
            this.targetBus = checkNotNull(targetBus);
            this.boundedContextName = boundedContextName;
        }

        public B setPublisherHub(PublisherHub publisherHub) {
            this.publisherHub = checkNotNull(publisherHub);
            return self();
        }

        protected abstract BusAdapter<E, D> doBuild();

        protected abstract B self();

        public BusAdapter<E, D> build() {
            checkNotNull(publisherHub, "PublisherHub must be set");

            BusAdapter<E, D> result = doBuild();
            return result;
        }
    }
}
