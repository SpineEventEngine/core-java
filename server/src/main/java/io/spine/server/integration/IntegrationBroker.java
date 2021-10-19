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
import io.spine.server.event.EventBus;
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
 * Dispatches {@linkplain ExternalMessage external messages} from and to the Bounded Context
 * in which this bus operates.
 *
 * <p>An {@code IntegrationBroker} is available as a part of single {@code BoundedContext}.
 * In a multi-component environment messages may travel across components from one Bounded Context
 * to another.
 *
 * <p>An {@code IntegrationBroker} is always based upon {@linkplain TransportFactory transport}
 * that delivers the messages from and to it. For several Bounded Contexts to communicate,
 * their brokers have to share the transport. Typically, that would be a single message queue.
 *
 * <p>The messages from external components received by an {@code IntegrationBroker} via
 * the transport are propagated into the Bounded Context via the domestic {@code EventBus}.
 *
 * <p>{@code IntegrationBroker} is also responsible for publishing the messages born within
 * the current Bounded Context to external collaborators. To do that properly, the broker listens
 * to a special document message called {@linkplain RequestForExternalMessages} that describes
 * the needs of other parties.
 *
 * <p><b>Sample Usage.</b>
 *
 * <p>Bounded Context "Projects" has an external event handler method in a projection as follows:
 * <pre>
 * {@code
 * public class ProjectListView extends Projection ...  {
 *
 *     {@literal @}Subscribe(external = true)
 *      public void on(UserDeleted event) {
 *          // Remove the projects that belong to this user.
 *          // ...
 *      }
 *  }
 *  }
 * </pre>
 *
 * <p>Upon a registration of the corresponding repository, the broker associated with the "Projects"
 * context sends out a {@code RequestForExternalMessages} saying that the of {@code UserDeleted}
 * event is needed.
 *
 * <p>Let's say the second Context is "Users". Its broker will receive
 * the {@code RequestForExternalMessages} request sent by "Projects". To handle it properly, it will
 * create a bridge between "Users"'s Event Bus (which may eventually be transmitting
 * a {@code UserDeleted} event) and the external message
 * {@linkplain ServerEnvironment#transportFactory() transport}.
 *
 * <p>Once {@code UserDeleted} is emitted locally in "Users" context, it will be received
 * by this bridge (as well as other local dispatchers) and published to the external transport.
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
    private static final ChannelId MESSAGES_SOURCE_JOINED_CHANNEL_ID = channelIdFor(
            TypeUrl.of(MessagesSourceJoined.class)
    );
    private static final ChannelId NEEDS_EXCHANGE_CHANNEL_ID = channelIdFor(
            TypeUrl.of(RequestForExternalMessages.class)
    );

    private final SubscriberHub subscriberHub;
    private final PublisherHub publisherHub;

    private @MonotonicNonNull BoundedContextName contextName;
    private @MonotonicNonNull BusAdapter busAdapter;

    private @MonotonicNonNull ExternalNeedsObserver externalNeedsObserver;
    private @MonotonicNonNull InternalNeedsBroadcast internalNeedsBroadcast;

    public IntegrationBroker() {
        TransportFactory transportFactory = ServerEnvironment
                .instance()
                .transportFactory();

        this.subscriberHub = new SubscriberHub(transportFactory);
        this.publisherHub = new PublisherHub(transportFactory);
    }

    private static ChannelId toChannelId(EventClass cls) {
        TypeUrl targetType = cls.typeUrl();
        return channelIdFor(targetType);
    }

    @Override
    public void registerWith(BoundedContext context) {
        checkNotRegistered();

        BoundedContextName name = context.name();
        EventBus eventBus = context.eventBus();

        this.contextName = name;
        this.busAdapter = new BusAdapter(this, eventBus);

        this.externalNeedsObserver = new ExternalNeedsObserver(name, busAdapter);
        Publisher localNeedPublisher = publisherHub.get(NEEDS_EXCHANGE_CHANNEL_ID);
        this.internalNeedsBroadcast = new InternalNeedsBroadcast(name, localNeedPublisher);

        this.subscriberHub.get(NEEDS_EXCHANGE_CHANNEL_ID)
                          .addObserver(externalNeedsObserver);

        publishMessagesSourceJoined();
        subscribeForMessagesSourceJoined();
    }

    @Override
    public boolean isRegistered() {
        return contextName != null;
    }

    private void publishMessagesSourceJoined() {
        MessagesSourceJoined messagesSourceJoined = MessagesSourceJoined.newBuilder().buildPartial();
        ExternalMessage externalMessage = ExternalMessages.of(messagesSourceJoined, contextName);

        publisherHub.get(MESSAGES_SOURCE_JOINED_CHANNEL_ID)
                    .publish(pack(newUuid()), externalMessage);
    }
    
    private void subscribeForMessagesSourceJoined() {
        StreamObserver<ExternalMessage> sourceJoinedObserver = new AbstractChannelObserver(
                contextName,
                MessagesSourceJoined.class
        ) {
            @Override
            protected void handle(ExternalMessage message) {
                internalNeedsBroadcast.send();
            }
        };

        subscriberHub.get(MESSAGES_SOURCE_JOINED_CHANNEL_ID)
                .addObserver(sourceJoinedObserver);
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
        busAdapter.dispatch(event, ackObserver);
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
        internalNeedsBroadcast.onTypesChanged(needs);
    }

    /**
     * Notifies other Bounded Contexts of the application about the types requested by this Context.
     *
     * <p>The {@code IntegrationBroker} sends a {@link RequestForExternalMessages}. The request
     * triggers other Contexts to send their requests. As the result, all the Contexts know about
     * the needs of all the Contexts.
     */
    void notifyOthers() {
        internalNeedsBroadcast.send();
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
