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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.protobuf.Message;
import io.spine.core.BoundedContextName;
import io.spine.protobuf.AnyPacker;
import io.spine.server.bus.BusBuilder;
import io.spine.server.bus.DeadMessageHandler;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.bus.MulticastBus;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.event.EventBus;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.server.transport.PublisherHub;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.SubscriberHub;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.validate.Validate;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;
import static io.spine.server.integration.IntegrationChannels.fromId;
import static io.spine.server.integration.IntegrationChannels.toId;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.validate.Validate.checkNotDefault;
import static java.lang.String.format;

/**
 * Dispatches {@linkplain ExternalMessage external messages} from and to the bounded context,
 * in which this bus operates.
 *
 * <p>This bus is available as a part of single {@code BoundedContext}.
 * In a multi-component environment messages may travel across components from one bounded context
 * to another.
 *
 * <p>{@code IntegrationBus} is always based upon some {@linkplain TransportFactory transport},
 * that delivers the messages from and to it. For several bounded contexts to communicate,
 * their integration buses have to share the transport. Typically that would be a single
 * messaging broker.
 *
 * <p>The messages from external components received by the {@code IntegrationBus} instance
 * via the transport are propagated into the bounded context. They are dispatched
 * to the subscribers and reactors marked with {@code external = true} on per-message-type basis.
 *
 * {@code IntegrationBus} is also responsible for publishing the messages,
 * born within the current `BoundedContext`, to external collaborators. To do that properly,
 * the bus listens to a special document message called {@linkplain RequestForExternalMessages},
 * that describes the needs of other parties.
 *
 * <h3>An example.</h3>
 *
 * <p>Bounded context "Projects" has the external event handler method in the projection as follows:
 * <pre>
 * public class ProjectListView extends Projection ...  {
 *
 *      {@literal @}Subscribe(external = true)
 *      public void on(UserDeleted event) {
 *          // Remove the projects that belong to this user.
 *          // ...
 *      }
 *
 *      // ...
 *  }
 * </pre>
 *
 * <p>Upon a registration of the corresponding repository the  integration bus of
 * "Projects" context sends out a {@code RequestForExternalMessages} saying that an {@code Event}
 * of {@code UserDeleted} type is needed from other parties.
 *
 * <p>Let's say the second bounded context is "Users". Its integration bus will receive
 * the {@code RequestForExternalMessages} request sent out by "Projects". To handle it properly
 * it will create a bridge between "Users"'s event bus (which may eventually be transmitting
 * a {@code UserDeleted} event) and an external transport.
 *
 * <p>Once {@code UserDeleted} is published locally in "Users" context, it will be received
 * by this bridge (as well as other local dispatchers), and published to the external transport.
 *
 * <p>The integration bus of "Projects" context will receive the {@code UserDeleted}
 * external message. The event will be dispatched to the external event handler of
 * {@code ProjectListView} projection.
 *
 * @author Alex Tymchenko
 */
@SuppressWarnings("OverlyCoupledClass")
public class IntegrationBus extends MulticastBus<ExternalMessage,
                                                 ExternalMessageEnvelope,
                                                 ExternalMessageClass,
                                                 ExternalMessageDispatcher<?>> {

    /**
     * An identification of the channel, serving to exchange {@linkplain RequestForExternalMessages
     * configuration messages} with other parties, such as instances of {@code IntegrationBus}
     * from other {@code BoundedContext}s.
     */
    private static final ChannelId CONFIG_EXCHANGE_CHANNEL_ID =
            toId(RequestForExternalMessages.class);

    private final Iterable<BusAdapter<?, ?>> localBusAdapters;
    private final BoundedContextName boundedContextName;
    private final SubscriberHub subscriberHub;
    private final PublisherHub publisherHub;
    private final ConfigurationChangeObserver configurationChangeObserver;

    @SuppressWarnings("OptionalGetWithoutIsPresent")
        // `TransportFactory` has already been initialized.
    private IntegrationBus(Builder builder) {
        super(builder);
        TransportFactory transportFactory = builder.getTransportFactory()
                                                   .get();
        this.boundedContextName = builder.boundedContextName;
        this.subscriberHub = new SubscriberHub(transportFactory);
        this.publisherHub = new PublisherHub(transportFactory);
        this.localBusAdapters = createAdapters(builder, publisherHub);
        configurationChangeObserver = observeConfigurationChanges();
        subscriberHub.get(CONFIG_EXCHANGE_CHANNEL_ID)
                     .addObserver(configurationChangeObserver);
    }

    /**
     * Creates an observer to react upon {@linkplain RequestForExternalMessages external request}
     * message arrival.
     */
    private ConfigurationChangeObserver observeConfigurationChanges() {
        return new ConfigurationChangeObserver(
                boundedContextName,
                message -> {
                    checkNotNull(message);
                    return adapterFor(message);
                });
    }

    private static
    ImmutableSet<BusAdapter<?, ?>> createAdapters(Builder builder, PublisherHub publisherHub) {
        return ImmutableSet.of(
                EventBusAdapter.builderWith(builder.eventBus, builder.boundedContextName)
                               .setPublisherHub(publisherHub)
                               .build(),
                RejectionBusAdapter.builderWith(builder.rejectionBus, builder.boundedContextName)
                                   .setPublisherHub(publisherHub)
                                   .build()
        );
    }

    /** Creates a new builder for this bus. */
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    protected DomesticDispatcherRegistry createRegistry() {
        return new DomesticDispatcherRegistry();
    }

    @Override
    protected DeadMessageHandler<ExternalMessageEnvelope> getDeadMessageHandler() {
        return DeadExternalMessageHandler.INSTANCE;
    }

    @Override
    protected EnvelopeValidator<ExternalMessageEnvelope> getValidator() {
        return ExternalMessageValidator.INSTANCE;
    }

    @Override
    protected ExternalMessageEnvelope toEnvelope(ExternalMessage message) {
        BusAdapter<?, ?> adapter = adapterFor(message);
        ExternalMessageEnvelope result = adapter.toExternalEnvelope(message);
        return result;
    }

    private static IllegalArgumentException messageUnsupported(Class<? extends Message> msgClass) {
        throw newIllegalArgumentException("The message of %s type isn't supported", msgClass);
    }

    @Override
    protected void dispatch(ExternalMessageEnvelope envelope) {
        ExternalMessageEnvelope markedEnvelope = markExternal(envelope);
        int dispatchersCalled = callDispatchers(markedEnvelope);

        checkState(dispatchersCalled != 0,
                   format("External message %s has no local dispatchers.",
                          markedEnvelope.getMessage()));
    }

    private ExternalMessageEnvelope markExternal(ExternalMessageEnvelope envelope) {
        ExternalMessage externalMessage = envelope.getOuterObject();
        BusAdapter<?, ?> adapter = adapterFor(externalMessage);
        return adapter.markExternal(externalMessage);
    }

    private BusAdapter<?, ?> adapterFor(ExternalMessage message) {
        Message unpackedOriginal = AnyPacker.unpack(message.getOriginalMessage());
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

        // Remember the channel IDs, that we have been subscribed before.
        Set<ChannelId> requestedBefore = subscriberHub.ids();

        // Subscribe to incoming messages of requested types.
        subscribeToIncoming(dispatcher);

        Set<ChannelId> currentlyRequested = subscriberHub.ids();
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

        // Remember the IDs of channels, that we have been subscribed before.
        Set<ChannelId> requestedBefore = subscriberHub.ids();

        // Unsubscribe from the types requested by this dispatcher.
        unsubscribeFromIncoming(dispatcher);

        Set<ChannelId> currentlyRequested = subscriberHub.ids();
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
    @SuppressWarnings("CheckReturnValue") // calling builder
    private void notifyOfNeeds(Iterable<ChannelId> currentlyRequested) {
        RequestForExternalMessages.Builder resultBuilder = RequestForExternalMessages.newBuilder();
        for (ChannelId channelId : currentlyRequested) {
            ExternalMessageType type = fromId(channelId);
            resultBuilder.addRequestedMessageTypes(type);
        }
        RequestForExternalMessages result = resultBuilder.build();
        ExternalMessage externalMessage = ExternalMessages.of(result, boundedContextName);
        publisherHub.get(CONFIG_EXCHANGE_CHANNEL_ID)
                    .publish(pack(newUuid()), externalMessage);
    }

    /**
     * Registers the passed event subscriber as an external event dispatcher
     * by taking only external subscriptions into account.
     *
     * @param eventSubscriber the subscriber to register.
     */
    public void register(AbstractEventSubscriber eventSubscriber) {
        ExternalEventSubscriber wrapped = new ExternalEventSubscriber(eventSubscriber);
        register(wrapped);
    }

    /**
     * Registers the passed rejection subscriber as an external rejection dispatcher
     * by taking only external subscriptions into account.
     *
     * @param rejectionSubscriber the subscriber to register.
     */
    public void register(RejectionSubscriber rejectionSubscriber) {
        ExternalRejectionSubscriber wrapped = new ExternalRejectionSubscriber(rejectionSubscriber);
        register(wrapped);
    }

    /**
     * Unregisters the passed event subscriber as an external event dispatcher
     * by taking only external subscriptions into account.
     *
     * @param eventSubscriber the subscriber to register.
     */
    public void unregister(AbstractEventSubscriber eventSubscriber) {
        ExternalEventSubscriber wrapped = new ExternalEventSubscriber(eventSubscriber);
        unregister(wrapped);
    }

    private void subscribeToIncoming(ExternalMessageDispatcher<?> dispatcher) {
        IntegrationBus integrationBus = this;
        Iterable<ExternalMessageClass> transformed = dispatcher.getMessageClasses();
        for (ExternalMessageClass imClass : transformed) {
            ChannelId channelId = toId(imClass);
            Subscriber subscriber = subscriberHub.get(channelId);
            subscriber.addObserver(new ExternalMessageObserver(boundedContextName,
                                                               imClass.value(),
                                                               integrationBus));
        }
    }

    private void unsubscribeFromIncoming(ExternalMessageDispatcher<?> dispatcher) {
        IntegrationBus integrationBus = this;
        Iterable<ExternalMessageClass> transformed = dispatcher.getMessageClasses();
        for (ExternalMessageClass imClass : transformed) {
            ChannelId channelId = toId(imClass);
            Subscriber subscriber = subscriberHub.get(channelId);
            subscriber.removeObserver(new ExternalMessageObserver(boundedContextName,
                                                                  imClass.value(),
                                                                  integrationBus));
        }
        subscriberHub.closeStaleChannels();
    }

    /**
     * Removes all subscriptions and closes all the underlying transport channels.
     */
    @Override
    public void close() throws Exception {
        super.close();

        configurationChangeObserver.close();
        // Declare that this instance has no needs.
        notifyOfNeeds(ImmutableSet.of());

        subscriberHub.close();
        publisherHub.close();
    }

    @Override
    public String toString() {
        return "Integration Bus of BoundedContext Name = " + boundedContextName.getValue();
    }

    private BusAdapter<?, ?> adapterFor(Class<? extends Message> messageClass) {
        for (BusAdapter<?, ?> localAdapter : localBusAdapters) {
            if (localAdapter.accepts(messageClass)) {
                return localAdapter;
            }
        }
        throw messageUnsupported(messageClass);
    }

    /**
     * A {@code Builder} for {@code IntegrationBus} instances.
     */
    @CanIgnoreReturnValue
    public static class Builder
            extends BusBuilder<ExternalMessageEnvelope, ExternalMessage, Builder> {

        /**
         * Buses that act inside the bounded context, e.g. {@code EventBus}, and which allow
         * dispatching their events to other bounded contexts.
         *
         * <p>{@code CommandBus} does <em>not</em> allow such a dispatching, as commands cannot be
         * sent to another bounded context for a postponed handling.
         */

        private EventBus eventBus;
        private RejectionBus rejectionBus;
        private BoundedContextName boundedContextName;
        private TransportFactory transportFactory;

        public Optional<EventBus> getEventBus() {
            return Optional.ofNullable(eventBus);
        }

        @CanIgnoreReturnValue
        public Builder setEventBus(EventBus eventBus) {
            this.eventBus = checkNotNull(eventBus);
            return self();
        }

        public Optional<RejectionBus> getRejectionBus() {
            return Optional.ofNullable(rejectionBus);
        }

        public Optional<BoundedContextName> getBoundedContextName() {
            BoundedContextName value = Validate.isDefault(this.boundedContextName)
                                       ? null
                                       : this.boundedContextName;
            return Optional.ofNullable(value);
        }

        @CanIgnoreReturnValue
        public Builder setRejectionBus(RejectionBus rejectionBus) {
            this.rejectionBus = checkNotNull(rejectionBus);
            return self();
        }

        @CanIgnoreReturnValue
        public Builder setBoundedContextName(BoundedContextName boundedContextName) {
            this.boundedContextName = checkNotNull(boundedContextName);
            return self();
        }

        @CanIgnoreReturnValue
        public Builder setTransportFactory(TransportFactory transportFactory) {
            this.transportFactory = checkNotNull(transportFactory);
            return self();
        }

        public Optional<TransportFactory> getTransportFactory() {
            return Optional.ofNullable(transportFactory);
        }

        @Override
        @CheckReturnValue
        public IntegrationBus build() {

            checkState(eventBus != null,
                       "`eventBus` must be set for IntegrationBus.");
            checkState(rejectionBus != null,
                       "`rejectionBus` must be set for IntegrationBus.");
            checkNotDefault(boundedContextName,
                            "`boundedContextName` must be set for IntegrationBus.");

            if (transportFactory == null) {
                transportFactory = initTransportFactory();
            }

            return new IntegrationBus(this);
        }

        private static TransportFactory initTransportFactory() {
            return InMemoryTransportFactory.newInstance();
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
