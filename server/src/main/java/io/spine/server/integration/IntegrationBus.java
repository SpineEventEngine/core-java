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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.BoundedContextId;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.core.Rejections;
import io.spine.protobuf.AnyPacker;
import io.spine.server.bus.Bus;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageTap;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.bus.MulticastBus;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventSubscriber;
import io.spine.server.integration.TransportFactory.PublisherHub;
import io.spine.server.integration.TransportFactory.Subscriber;
import io.spine.server.integration.TransportFactory.SubscriberHub;
import io.spine.server.integration.local.LocalTransportFactory;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.type.KnownTypes;
import io.spine.type.TypeUrl;
import io.spine.validate.Validate;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.Identifier.newUuid;
import static io.spine.Identifier.pack;
import static io.spine.server.bus.Buses.acknowledge;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.validate.Validate.checkNotDefault;
import static java.lang.String.format;

/**
 * Dispatches {@linkplain ExternalMessage external messages} from and to the bounded context,
 * in which this bus operates.
 *
 * @author Alex Tymchenko
 */
public class IntegrationBus extends MulticastBus<ExternalMessage,
                                                 ExternalMessageEnvelope,
                                                 ExternalMessageClass,
                                                 ExternalMessageDispatcher<?>> {

    private final Iterable<BusAdapter<?, ?>> localBusAdapters;
    private final BoundedContextId boundedContextId;
    private final SubscriberHub subscriberHub;
    private final PublisherHub publisherHub;

    private IntegrationBus(Builder builder) {
        super(builder.getDelivery());
        this.boundedContextId = builder.boundedContextId;
        this.subscriberHub = new SubscriberHub(builder.transportFactory);
        this.publisherHub = new PublisherHub(builder.transportFactory);

        this.localBusAdapters = createAdapters(builder, publisherHub);

        /*
         * Reacts upon {@code RequestedMessageTypes} message arrival.
         */
        final ConfigurationChangeObserver observer = new ConfigurationChangeObserver(
                boundedContextId,
                new Function<Class<? extends Message>, BusAdapter<?, ?>>() {
                    @Override
                    public BusAdapter<?, ?> apply(@Nullable Class<? extends Message> message) {
                        checkNotNull(message);
                        return adapterFor(message);
                    }
                });
        subscriberHub.get(ExternalMessageClass.of(RequestForExternalMessages.class))
                     .addObserver(observer);
    }

    private static ImmutableSet<BusAdapter<?, ?>> createAdapters(Builder builder,
                                                                 PublisherHub publisherHub) {
        return ImmutableSet.<BusAdapter<?, ?>>of(
                EventBusAdapter.builderWith(builder.eventBus, builder.boundedContextId)
                               .setPublisherHub(publisherHub)
                               .build(),
                RejectionBusAdapter.builderWith(builder.rejectionBus, builder.boundedContextId)
                                   .setPublisherHub(publisherHub)
                                   .build()
        );
    }

    /** Creates a new builder for this bus. */
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
        return IncomingMessageValidator.INSTANCE;
    }

    @Override
    protected Deque<BusFilter<ExternalMessageEnvelope>> createFilterChain() {
        return newLinkedList();
    }

    @Override
    protected ExternalMessageEnvelope toEnvelope(ExternalMessage message) {
        final BusAdapter<?, ?> adapter = adapterFor(message);
        final ExternalMessageEnvelope result = adapter.toExternalEnvelope(message);
        return result;
    }

    private static IllegalArgumentException messageUnsupported(Class<? extends Message> msgClass) {
        throw newIllegalArgumentException("The message of %s type isn't supported", msgClass);
    }

    @Override
    protected Ack doPost(ExternalMessageEnvelope envelope) {
        final ExternalMessageEnvelope markedEnvelope = markExternal(envelope);
        final int dispatchersCalled = callDispatchers(markedEnvelope);

        final Any packedId = pack(markedEnvelope.getId());
        checkState(dispatchersCalled != 0,
                   format("External message %s has no local dispatchers.",
                          markedEnvelope.getMessage()));
        final Ack result = acknowledge(packedId);
        return result;
    }

    private  ExternalMessageEnvelope markExternal(ExternalMessageEnvelope envelope) {
        final ExternalMessage externalMessage = envelope.getOuterObject();
        final BusAdapter<?, ?> adapter = adapterFor(externalMessage);
        return adapter.markExternal(externalMessage);
    }

    private BusAdapter<?, ?> adapterFor(ExternalMessage message) {
        final Message unpackedOriginal = AnyPacker.unpack(message.getOriginalMessage());
        return adapterFor(unpackedOriginal.getClass());
    }

    @Override
    protected void store(Iterable<ExternalMessage> messages) {
        // we don't store the incoming messages yet.
    }

    /**
     * Registers a local dispatcher, which is subscribed to {@code external} messages.
     *
     * @param dispatcher the dispatcher to register
     */
    @Override
    public void register(ExternalMessageDispatcher<?> dispatcher) {
        super.register(dispatcher);

        // Remember the message types, that we have been subscribed before.
        final Set<ExternalMessageClass> requestedBefore = subscriberHub.keys();

        // Subscribe to incoming messages of requested types.
        subscribeToIncoming(dispatcher);

        final Set<ExternalMessageClass> currentlyRequested = subscriberHub.keys();
        if (!currentlyRequested.equals(requestedBefore)) {

            // Notify others that the requested message set has been changed.
            notifyOfNeeds(currentlyRequested);
        }
    }

    /**
     * Unregisters a local dispatcher, which should no longer be subscribed 
     * to {@code external} messages.
     *
     * @param dispatcher the dispatcher to unregister
     */
    @Override
    public void unregister(ExternalMessageDispatcher<?> dispatcher) {
        super.unregister(dispatcher);

        // Remember the message types, that we have been subscribed before.
        final Set<ExternalMessageClass> requestedBefore = subscriberHub.keys();

        // Subscribe to incoming messages of requested types.
        unsubscribeFromIncoming(dispatcher);

        final Set<ExternalMessageClass> currentlyRequested = subscriberHub.keys();
        if (!currentlyRequested.equals(requestedBefore)) {
            notifyOfNeeds(currentlyRequested);
        }
    }

    /**
     * Notifies other parts of the application that this integration bus instance now requests
     * for a different set of message types than previously.
     *
     * <p>Sends out an instance of {@linkplain RequestForExternalMessages
     * request for external messages} for that purpose.
     *
     * @param  currentlyRequested
     *         the set of message types that are now requested by this instance of
     *         integration bus
     */
    private void notifyOfNeeds(Iterable<ExternalMessageClass> currentlyRequested) {
        final RequestForExternalMessages.Builder resultBuilder =
                RequestForExternalMessages.newBuilder();
        for (ExternalMessageClass messageClass : currentlyRequested) {
            final ExternalMessageType type = toExternalMessageType(messageClass);
            resultBuilder.addRequestedMessageTypes(type);
        }
        final RequestForExternalMessages result = resultBuilder.build();
        final ExternalMessage externalMessage = ExternalMessages.of(result,
                                                                    boundedContextId);
        final ExternalMessageClass channelId = ExternalMessageClass.of(result.getClass());
        publisherHub.get(channelId)
                    .publish(pack(newUuid()), externalMessage);
    }

    private static ExternalMessageType toExternalMessageType(ExternalMessageClass messageClass) {
        final TypeUrl typeUrl = KnownTypes.getTypeUrl(messageClass.getClassName());
        final boolean isRejection = Rejections.isRejection(messageClass.value());
        final String wrapperTypeUrl = isRejection ? TypeUrl.of(Rejection.class).value()
                                                  : TypeUrl.of(Event.class).value();
        return ExternalMessageType.newBuilder()
                                                            .setMessageTypeUrl(typeUrl.value())
                                                            .setWrapperTypeUrl(wrapperTypeUrl)
                                                            .build();
    }

    /**
     * Registers the passed event subscriber as an external event dispatcher
     * by taking only external subscriptions into account.
     *
     * @param eventSubscriber the subscriber to register.
     */
    public void register(final EventSubscriber eventSubscriber) {
        final ExternalEventSubscriber wrapped = new ExternalEventSubscriber(eventSubscriber);
        register(wrapped);
    }

    /**
     * Registers the passed rejection subscriber as an external rejection dispatcher
     * by taking only external subscriptions into account.
     *
     * @param rejectionSubscriber the subscriber to register.
     */
    public void register(final RejectionSubscriber rejectionSubscriber) {
        final ExternalRejectionSubscriber wrapped =
                new ExternalRejectionSubscriber(rejectionSubscriber);
        register(wrapped);
    }

    /**
     * Unregisters the passed event subscriber as an external event dispatcher
     * by taking only external subscriptions into account.
     *
     * @param eventSubscriber the subscriber to register.
     */
    public void unregister(final EventSubscriber eventSubscriber) {
        final ExternalEventSubscriber wrapped = new ExternalEventSubscriber(eventSubscriber);
        unregister(wrapped);
    }

    private void subscribeToIncoming(ExternalMessageDispatcher<?> dispatcher) {
        final IntegrationBus integrationBus = this;
        final Iterable<ExternalMessageClass> transformed = dispatcher.getMessageClasses();
        for (final ExternalMessageClass imClass : transformed) {
            final Subscriber subscriber = subscriberHub.get(imClass);
            subscriber.addObserver(new IncomingMessageObserver(boundedContextId, 
                                                               imClass.value(), 
                                                               integrationBus));
        }
    }
    
    private void unsubscribeFromIncoming(ExternalMessageDispatcher<?> dispatcher) {
        final IntegrationBus integrationBus = this;
        final Iterable<ExternalMessageClass> transformed = dispatcher.getMessageClasses();
        for (final ExternalMessageClass imClass : transformed) {
            final Subscriber subscriber = subscriberHub.get(imClass);
            subscriber.removeObserver(new IncomingMessageObserver(boundedContextId,
                                                                  imClass.value(),
                                                                  integrationBus));
        }
        subscriberHub.releaseStale();
    }

    @Override
    public String toString() {
        return "Integration bus of BoundedContext ID = " + boundedContextId.getValue();
    }

    private BusAdapter<?, ?> adapterFor(Class<? extends Message> messageClass) {
        for (BusAdapter<?, ?> localAdapter : localBusAdapters) {
            if(localAdapter.accepts(messageClass)) {
                return localAdapter;
            }
        }
        throw messageUnsupported(messageClass);
    }

    /**
     * A {@code Builder} for {@code IntegrationBus} instances.
     */
    public static class Builder
            extends Bus.AbstractBuilder<ExternalMessageEnvelope, ExternalMessage, Builder> {

        /**
         * Buses that act inside the bounded context, e.g. {@code EventBus}, and which allow
         * dispatching their events to other bounded contexts.
         *
         * <p>{@code CommandBus} does <em>not</em> allow such a dispatching, as commands cannot be
         * sent to another bounded context for a postponed handling.
         */

        private EventBus eventBus;
        private RejectionBus rejectionBus;
        private LocalDelivery delivery;
        private BoundedContextId boundedContextId;
        private TransportFactory transportFactory;

        public Optional<EventBus> getEventBus() {
            return Optional.fromNullable(eventBus);
        }

        public Builder setEventBus(EventBus eventBus) {
            this.eventBus = checkNotNull(eventBus);
            return self();
        }

        public Optional<RejectionBus> getRejectionBus() {
            return Optional.fromNullable(rejectionBus);
        }

        public Optional<BoundedContextId> getBoundedContextId() {
            final BoundedContextId value = Validate.isDefault(this.boundedContextId)
                                           ? null
                                           : this.boundedContextId;
            return Optional.fromNullable(value);
        }

        public Builder setRejectionBus(RejectionBus rejectionBus) {
            this.rejectionBus = checkNotNull(rejectionBus);
            return self();
        }

        public Builder setBoundedContextId(BoundedContextId boundedContextId) {
            this.boundedContextId = checkNotNull(boundedContextId);
            return self();
        }

        public Builder setTransportFactory(TransportFactory transportFactory) {
            this.transportFactory = checkNotNull(transportFactory);
            return self();
        }

        public Optional<TransportFactory> getTransportFactory() {
            return Optional.fromNullable(transportFactory);
        }

        private LocalDelivery getDelivery() {
            return delivery;
        }

        @Override
        public IntegrationBus build() {

            checkState(eventBus != null,
                       "`eventBus` must be set for integration bus.");
            checkState(rejectionBus != null,
                       "`rejectionBus` must be set for integration bus.");
            checkNotDefault(boundedContextId,
                            "`boundedContextId` must be set for integration bus.");

            if (transportFactory == null) {
                transportFactory = initTransportFactory();
            }

            this.delivery = new LocalDelivery();

            return new IntegrationBus(this);
        }

        private static TransportFactory initTransportFactory() {
            return LocalTransportFactory.newInstance();
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
