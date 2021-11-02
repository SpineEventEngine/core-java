/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.ContextAware;
import io.spine.server.ServerEnvironment;
import io.spine.server.event.EventDispatcher;
import io.spine.server.transport.ChannelId;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.PublisherHub;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.SubscriberHub;
import io.spine.server.transport.TransportFactory;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.transport.MessageChannel.channelIdFor;

/**
 * Dispatches {@linkplain ExternalMessage external messages} from and to a Bounded Context with
 * which this broker is associated.
 *
 * <p>In a multi-component environment messages may travel across components from one
 * Bounded Context to another. Accordingly, an {@code IntegrationBroker} is available as a part
 * of a Bounded Context.
 *
 * <p>An {@code IntegrationBroker} is always based upon {@linkplain TransportFactory transport}
 * that delivers messages from and to it. For several Bounded Contexts to communicate, their brokers
 * have to share the transport. Typically, that would be a single message queue.
 *
 * <p>{@code IntegrationBroker}s communicate with each other in order to keep the whole list of
 * requested messages (needs) and their potential sources (Bounded Contexts) up-to-date.
 * They use two special messages for this:
 * <ul>
 *     <li>{@link ExternalMessagesSourceAvailable} is sent when a broker is registered
 *     withing a Bounded Context;
 *     <li>{@link RequestForExternalMessages} is sent
 *     <ul>
 *         <li>in response to {@link ExternalMessagesSourceAvailable} sent by other brokers;
 *         <li>when internal needs for external messages are changed.
 *     </ul>
 *</ul>
 *
 * <p><b>Receiving</b>
 *
 * <p>The messages from external components received by an {@code IntegrationBroker} via
 * the transport are propagated into the Bounded Context via the domestic {@code EventBus}.
 *
 * <p><b>Publishing</b>
 *
 * <p>The messages requested by other parties are published from the domestic
 * {@code EventBus} with the help of {@linkplain DomesticEventPublisher special dispatcher}.
 *
 * <p><b>Sample Usage</b>
 *
 * <p>Bounded Context "Projects" has a projection with an event handler that is subscribed to an
 * external event as follows:
 * <pre>
 * {@code
 * public class ProjectListView extends Projection<...>  {
 *
 *     {@literal @}Subscribe
 *      public void on(@External UserDeleted event) {
 *          // Remove the projects that belong to this user.
 *          // ...
 *      }
 *  }
 *  }
 * </pre>
 *
 * <p>Upon a registration of the corresponding repository for this projection in the context,
 * the broker associated with that context is informed that one more external event is needed.
 * It sends out an updated {@code RequestForExternalMessages} saying that {@code UserDeleted}
 * events are needed too.
 *
 * <p>Let's say the second Context is "Users". Its broker will receive
 * the {@code RequestForExternalMessages} sent by "Projects". To handle it, it will create a bridge
 * between "Users"'s Event Bus (which may eventually be transmitting a {@code UserDeleted} event)
 * and the {@linkplain ServerEnvironment#transportFactory() transport}.
 *
 * <p>Once {@code UserDeleted} is emitted locally in "Users" context, it will be received
 * by this bridge (as well as other local dispatchers) and published to the external transport by
 * its {@code IntegrationBroker}.
 *
 * <p>The integration broker on the "Projects" side will receive the {@code UserDeleted}
 * external message. The event will be dispatched to the external event handler of the projection.
 *
 * <p><b>Limitations</b>
 *
 * <p>An event type may be consumed by many Contexts but produced only by a single Context. This
 * means that each given event type belongs only to one Context. The ownership of an event type may
 * be changed between the versions of a system, but may not be changed while events are travelling
 * between the Contexts.
 */
@Internal
@SuppressWarnings("OverlyCoupledClass")
public final class IntegrationBroker implements ContextAware, AutoCloseable {

    private static final TypeUrl EVENT_TYPE_URL = TypeUrl.of(Event.class);
    private static final ChannelId MESSAGE_SOURCES_CHANNEL_ID = channelIdFor(
            TypeUrl.of(ExternalMessagesSourceAvailable.class)
    );
    private static final ChannelId NEEDS_EXCHANGE_CHANNEL_ID = channelIdFor(
            TypeUrl.of(RequestForExternalMessages.class)
    );

    private final SubscriberHub subscriberHub;
    private final PublisherHub publisherHub;

    private @MonotonicNonNull BoundedContextName contextName;
    private @MonotonicNonNull BusAdapter bus;

    private @MonotonicNonNull ExternalNeedsObserver externalNeedsObserver;
    private @MonotonicNonNull InternalNeedsBroadcast internalNeedsBroadcast;

    public IntegrationBroker() {
        TransportFactory transportFactory = ServerEnvironment
                .instance()
                .transportFactory();
        this.subscriberHub = new SubscriberHub(transportFactory);
        this.publisherHub = new PublisherHub(transportFactory);
    }

    @Override
    public void registerWith(BoundedContext context) {
        checkNotRegistered();

        this.contextName = context.name();
        this.bus = new BusAdapter(this, context.eventBus());

        setUpNeedsExchange();
        notifyOfRegistration();
        subscribeForFurtherRegistrations();
    }

    private void setUpNeedsExchange() {
        Publisher localNeedsPublisher = publisherHub.get(NEEDS_EXCHANGE_CHANNEL_ID);

        this.internalNeedsBroadcast = new InternalNeedsBroadcast(contextName, localNeedsPublisher);
        this.externalNeedsObserver = new ExternalNeedsObserver(contextName, bus);

        this.subscriberHub.get(NEEDS_EXCHANGE_CHANNEL_ID)
                          .addObserver(externalNeedsObserver);
    }

    private void notifyOfRegistration() {
        ExternalMessagesSourceAvailable notification =
                ExternalMessagesSourceAvailable
                        .newBuilder()
                        .vBuild();

        ExternalMessage externalMessage = ExternalMessages.of(notification, contextName);

        publisherHub.get(MESSAGE_SOURCES_CHANNEL_ID)
                    .publish(pack(newUuid()), externalMessage);
    }

    private void subscribeForFurtherRegistrations() {
        StreamObserver<ExternalMessage> newSourcesObserver =
                new ObserveNewRegistrations(contextName, internalNeedsBroadcast);
        subscriberHub.get(MESSAGE_SOURCES_CHANNEL_ID)
                     .addObserver(newSourcesObserver);
    }

    @Override
    public boolean isRegistered() {
        return contextName != null;
    }

    /**
     * Publishes the given event for other Bounded Contexts.
     *
     * <p>If this event belongs to another context, does nothing. More formally, if there is
     * a <b>subscriber</b> channel in this broker for events of such a type, those events are
     * NOT <b>published</b> from this Context.
     *
     * @param event
     *         the event to publish
     */
    void publish(EventEnvelope event) {
        EventClass eventClass = event.messageClass();
        ChannelId channelId = toChannelId(eventClass);
        boolean eventFromUpstream = subscriberHub.hasChannel(channelId);
        if (!eventFromUpstream) {
            Event outerObject = event.outerObject();
            ExternalMessage msg = ExternalMessages.of(outerObject, contextName);
            Publisher channel = publisherHub.get(channelId);
            channel.publish(AnyPacker.pack(event.id()), msg);
        }
    }

    /**
     * Dispatches the given event via the local {@code EventBus}.
     */
    void dispatchLocally(Event event) {
        dispatchLocally(event, noOpObserver());
    }

    /**
     * Dispatches the given event via the local {@code EventBus} and observes the acknowledgement.
     */
    void dispatchLocally(Event event, StreamObserver<Ack> ackObserver) {
        bus.dispatch(event, ackObserver);
    }

    /**
     * Registers a local dispatcher which is subscribed to {@code external} messages.
     *
     * @param dispatcher
     *         the dispatcher to register
     */
    public void register(EventDispatcher dispatcher) {
        Iterable<EventClass> receivedTypes = dispatcher.externalEventClasses();
        for (EventClass cls : receivedTypes) {
            ChannelId channelId = toChannelId(cls);
            Subscriber subscriber = subscriberHub.get(channelId);
            ExternalMessageObserver observer = observerFor(cls);
            subscriber.addObserver(observer);
            notifyTypesChanged();
        }
    }

    /**
     * Unregisters a local dispatcher which should no longer be subscribed
     * to {@code external} messages.
     *
     * @param dispatcher
     *         the dispatcher to unregister
     */
    public void unregister(EventDispatcher dispatcher) {
        Iterable<EventClass> transformed = dispatcher.externalEventClasses();
        for (EventClass cls : transformed) {
            ChannelId channelId = toChannelId(cls);
            Subscriber subscriber = subscriberHub.get(channelId);
            ExternalMessageObserver observer = observerFor(cls);
            subscriber.removeObserver(observer);
        }
        subscriberHub.closeStaleChannels();
    }

    private static ChannelId toChannelId(EventClass cls) {
        TypeUrl targetType = cls.typeUrl();
        return channelIdFor(targetType);
    }

    private ExternalMessageObserver observerFor(EventClass externalClass) {
        ExternalMessageObserver observer =
                new ExternalMessageObserver(contextName, externalClass.value(), this);
        return observer;
    }

    /**
     * Notifies other Bounded Contexts that this integration bus instance now requests a different
     * set of message types.
     *
     * <p>Sends out an instance of {@linkplain RequestForExternalMessages
     * request for external messages} for that purpose.
     */
    private void notifyTypesChanged() {
        ImmutableSet<ExternalMessageType> needs = subscriberHub
                .ids()
                .stream()
                .map(channelId -> ExternalMessageType
                        .newBuilder()
                        .setMessageTypeUrl(channelId.getTargetType())
                        .setWrapperTypeUrl(EVENT_TYPE_URL.value())
                        .buildPartial())
                .collect(toImmutableSet());
        internalNeedsBroadcast.onNeedsChange(needs);
    }

    /**
     * Removes all subscriptions and closes all the underlying transport channels.
     */
    @Override
    public void close() throws Exception {
        externalNeedsObserver.close();
        notifyTypesChanged();

        subscriberHub.close();
        publisherHub.close();
    }

    @Override
    public String toString() {
        return "IntegrationBroker of <" + contextName.getValue() + '>';
    }

}
