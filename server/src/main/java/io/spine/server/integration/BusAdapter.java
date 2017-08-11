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

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.core.BoundedContextId;
import io.spine.core.ExternalMessageEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.server.bus.Bus;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.integration.TransportFactory.PublisherHub;
import io.spine.server.integration.TransportFactory.SubscriberHub;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alex Tymchenko
 */
abstract class BusAdapter<E extends MessageEnvelope<?, ?, ?>,
        D extends MessageDispatcher<?, E, ?>> {

    private final Bus<?, E, ?, D> targetBus;

    private final PublisherHub publisherHub;

    private final SubscriberHub subscriberHub;

    protected BusAdapter(AbstractBuilder<?, E, D> builder) {
        this.targetBus = builder.targetBus;
        this.publisherHub = builder.publisherHub;
        this.subscriberHub = builder.subscriberHub;
    }

    abstract ExternalMessageEnvelope toExternalEnvelope(Message message);

    abstract ExternalMessageEnvelope markExternal(Message message);

    abstract boolean accepts(Class<? extends Message> message);

    abstract D createDispatcher(Class<? extends Message> messageCls,
                                BoundedContextId boundedContextId);

    void register(Class<? extends Message> messageClass, BoundedContextId boundedContextId) {
        final D dispatcher = createDispatcher(messageClass, boundedContextId);
        targetBus.register(dispatcher);
    }

    void unregister(Class<? extends Message> messageClass, BoundedContextId boundedContextId) {
        final D dispatcher = createDispatcher(messageClass, boundedContextId);
        targetBus.unregister(dispatcher);
    }

    Bus<?, E, ?, D> getTargetBus() {
        return targetBus;
    }

    PublisherHub getPublisherHub() {
        return publisherHub;
    }

    abstract static class AbstractBuilder<B extends AbstractBuilder<B, E, D>,
                                          E extends MessageEnvelope<?, ?, ?>,
                                          D extends MessageDispatcher<?, E, ?>> {

        private final Bus<?, E, ?, D> targetBus;

        private PublisherHub publisherHub;

        private SubscriberHub subscriberHub;

        AbstractBuilder(Bus<?, E, ?, D> targetBus) {
            this.targetBus = checkNotNull(targetBus);
        }

        public Optional<PublisherHub> getPublisherHub() {
            return Optional.fromNullable(publisherHub);
        }

        public B setPublisherHub(PublisherHub publisherHub) {
            this.publisherHub = checkNotNull(publisherHub);
            return self();
        }

        public Optional<SubscriberHub> getSubscriberHub() {
            return Optional.fromNullable(subscriberHub);
        }

        public B setSubscriberHub(SubscriberHub subscriberHub) {
            this.subscriberHub = checkNotNull(subscriberHub);
            return self();
        }

        protected abstract BusAdapter<E, D> doBuild();

        protected abstract B self();

        public BusAdapter<E, D> build() {
            checkNotNull(publisherHub, "PublisherHub must be set");
            checkNotNull(subscriberHub, "SubscriberHub must be set");

            final BusAdapter<E, D> result = doBuild();
            return result;
        }
    }
}
