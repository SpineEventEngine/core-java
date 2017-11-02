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
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
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
import io.spine.server.integration.memory.InMemoryRouteHolder;
import io.spine.server.integration.memory.InMemoryTransportFactory;
import io.spine.server.integration.route.ChannelRoute;
import io.spine.server.integration.route.DynamicRouter;
import io.spine.server.integration.route.Route;
import io.spine.server.integration.route.RouteHolder;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.type.TypeUrl;
import io.spine.validate.Validate;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.Identifier.pack;
import static io.spine.server.bus.Buses.acknowledge;
import static io.spine.server.integration.Channels.newId;
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
 * @author Dmitry Ganzha
 */
public class IntegrationBus extends MulticastBus<ExternalMessage,
                                                 ExternalMessageEnvelope,
                                                 ExternalMessageClass,
                                                 ExternalMessageDispatcher<?>> {

    private final Iterable<BusAdapter<?, ?>> localBusAdapters;
    private final BoundedContextName boundedContextName;
    private final SubscriberHub subscriberHub;
    private final ConfigurationChangeObserver configurationChangeObserver;
    private final DynamicRouter router;

    private IntegrationBus(Builder builder) {
        super(builder.getDelivery());
        this.boundedContextName = builder.boundedContextName;
        this.subscriberHub = new SubscriberHub(builder.transportFactory);
        this.router = new DynamicRouter(new PublisherHub(builder.transportFactory), builder.getRouteHolder());
        this.localBusAdapters = createAdapters(builder, router);
        configurationChangeObserver = observeConfigurationChanges();
        final ChannelId channelId = newId(
                ExternalMessageClass.of(RequestForExternalMessages.class));
        subscriberHub.get(channelId)
                     .addObserver(configurationChangeObserver);
    }

    /**
     * Creates an observer to react upon {@linkplain RequestForExternalMessages external request}
     * message arrival.
     */
    private ConfigurationChangeObserver observeConfigurationChanges() {
        return new ConfigurationChangeObserver(
                boundedContextName,
                new Function<Class<? extends Message>, BusAdapter<?, ?>>() {
                    @Override
                    public BusAdapter<?, ?> apply(@Nullable Class<? extends Message> message) {
                        checkNotNull(message);
                        return adapterFor(message);
                    }
                });
    }

    private static ImmutableSet<BusAdapter<?, ?>> createAdapters(Builder builder,
                                                                 DynamicRouter router) {
        return ImmutableSet.<BusAdapter<?, ?>>of(
                EventBusAdapter.builderWith(builder.eventBus, builder.boundedContextName)
                               .setRouter(router)
                               .build(),
                RejectionBusAdapter.builderWith(builder.rejectionBus, builder.boundedContextName)
                                   .setRouter(router)
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
    protected DeadMessageTap<ExternalMessageEnvelope> getDeadMessageHandler() {
        return DeadExternalMessageTap.INSTANCE;
    }

    @Override
    protected EnvelopeValidator<ExternalMessageEnvelope> getValidator() {
        return ExternalMessageValidator.INSTANCE;
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

        // Remember the channels, that we have been subscribed before.
        // The channel ID corresponds to the type of messages,
        // that are being served through the channel.
        final Set<ChannelId> requestedBefore = subscriberHub.keys();

        // Subscribe to incoming messages of requested types.
        subscribeToIncoming(dispatcher);

        final Set<ChannelId> currentlyRequested = subscriberHub.keys();
        if (!currentlyRequested.equals(requestedBefore)) {

            // Notify others that the requested message types have been changed.
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

        // Remember the channels, that we have been subscribed before.
        // The channel ID corresponds to the type of messages,
        // that are being served through the channel.
        final Set<ChannelId> requestedBefore = subscriberHub.keys();

        // Unsubscribe from incoming messages of requested types.
        unsubscribeFromIncoming(dispatcher);

        final Set<ChannelId> currentlyRequested = subscriberHub.keys();
        if (!currentlyRequested.equals(requestedBefore)) {

            // Notify others that the requested message types have been changed.
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
     * @param  currentlyRequested the set of channel IDs, which correspond to the currently
     *                            requested types of messages.
     */
    private void notifyOfNeeds(Iterable<ChannelId> currentlyRequested) {
        final RequestForExternalMessages.Builder resultBuilder =
                RequestForExternalMessages.newBuilder();
        for (ChannelId channelId : currentlyRequested) {
            final ExternalMessageType type = toExternalMessageType(channelId);
            resultBuilder.addRequestedMessageTypes(type);
        }
        final RequestForExternalMessages result = resultBuilder.build();
        final ExternalMessage externalMessage = ExternalMessages.of(result, boundedContextName);
        final ChannelId channelId = newId(ExternalMessageClass.of(result.getClass()));

        router.route(externalMessage);
    }

    private static ExternalMessageType toExternalMessageType(ChannelId channelId) {
        final TypeUrl typeUrl = TypeUrl.parse(channelId.getMessageTypeUrl());
        final ExternalMessageClass messageClass = ExternalMessageClass.of(typeUrl.getJavaClass());
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
            final ChannelId channelId = newId(imClass);
            final Subscriber subscriber = subscriberHub.get(channelId);
            subscriber.addObserver(new ExternalMessageObserver(boundedContextName,
                                                               imClass.value(),
                                                               integrationBus));
        }
    }

    private void unsubscribeFromIncoming(ExternalMessageDispatcher<?> dispatcher) {
        final IntegrationBus integrationBus = this;
        final Iterable<ExternalMessageClass> transformed = dispatcher.getMessageClasses();
        for (final ExternalMessageClass imClass : transformed) {
            final ChannelId channelId = newId(imClass);
            final Subscriber subscriber = subscriberHub.get(channelId);
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
        notifyOfNeeds(ImmutableSet.<ChannelId>of());

        subscriberHub.close();
        router.close();
    }

    @Override
    public String toString() {
        return "Integration Bus of BoundedContext Name = " + boundedContextName.getValue();
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
        private DomesticDelivery delivery;
        private BoundedContextName boundedContextName;
        private TransportFactory transportFactory;
        private RouteHolder<ChannelId> routeHolder;

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

        public Optional<BoundedContextName> getBoundedContextName() {
            final BoundedContextName value = Validate.isDefault(this.boundedContextName)
                                             ? null
                                             : this.boundedContextName;
            return Optional.fromNullable(value);
        }

        public Builder setRejectionBus(RejectionBus rejectionBus) {
            this.rejectionBus = checkNotNull(rejectionBus);
            return self();
        }

        public Builder setBoundedContextName(BoundedContextName boundedContextName) {
            this.boundedContextName = checkNotNull(boundedContextName);
            return self();
        }

        public Builder setTransportFactory(TransportFactory transportFactory) {
            this.transportFactory = checkNotNull(transportFactory);
            return self();
        }

        public Optional<TransportFactory> getTransportFactory() {
            return Optional.fromNullable(transportFactory);
        }

        public RouteHolder<ChannelId> getRouteHolder() {
            if(routeHolder == null) {
                Set<Route<ChannelId>> routes = Sets.newConcurrentHashSet();
                routeHolder = new InMemoryRouteHolder(routes);
            }
            return routeHolder;
        }

        public Builder setRouteHolder(RouteHolder<ChannelId> routeHolder) {
            this.routeHolder = checkNotNull(routeHolder);
            return self();
        }

        private DomesticDelivery getDelivery() {
            return delivery;
        }

        @Override
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

            this.delivery = new DomesticDelivery();

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
