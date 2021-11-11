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

import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.event.EventDispatcher;
import io.spine.server.transport.ChannelId;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.Subscriber;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.transport.MessageChannel.channelIdFor;

/**
 * Sends and receives the {@code external} domain events.
 *
 * <p>Publishes the domain events of the parent Bounded Context to those Bounded Contexts which
 * requested them for their {@code external} subscribers.
 *
 * <p>Receives the domain events from other Bounded Contexts and posts them to the domestic
 * bus treating them as {@code external}.
 */
class EventsExchange extends AbstractExchange {

    private final BusAdapter bus;

    /**
     * Creates a new exchange which uses the passed link.
     */
    EventsExchange(TransportLink link, BusAdapter bus) {
        super(link);
        this.bus = bus;
    }

    /**
     * Publishes the given event via this exchange.
     *
     * <p>Does nothing for events, which by chance, were passed into this method, being originated
     * from other Bounded Contexts.
     *
     * @param event
     *         event to publish
     */
    @Internal
    void publish(EventEnvelope event) {
        ChannelId channelId = toChannelId(event.messageClass());
        boolean wantedByOthers = !subscriptionChannels().contains(channelId);
        if (wantedByOthers) {
            Event outerObject = event.outerObject();
            ExternalMessage msg = ExternalMessages.of(outerObject, context());
            Publisher publisher = publisher(channelId);
            publisher.publish(AnyPacker.pack(event.id()), msg);
        }
    }

    /**
     * Registers a local dispatcher which would like to receive the {@code external} events
     * through this exchange.
     *
     * @param dispatcher
     *         the dispatcher to register
     */
    void register(EventDispatcher dispatcher) {
        Iterable<EventClass> receivedTypes = dispatcher.externalEventClasses();
        for (EventClass cls : receivedTypes) {
            ChannelId channelId = toChannelId(cls);
            Subscriber subscriber = subscriber(channelId);
            IncomingEventObserver observer = observerFor(cls);
            subscriber.addObserver(observer);
        }
    }

    /**
     * Unregisters a local dispatcher in case it should no longer receive its
     * {@code external} events via this exchange.
     *
     * @param dispatcher
     *         the dispatcher to unregister
     */
    public void unregister(EventDispatcher dispatcher) {
        Iterable<EventClass> externalEvents = dispatcher.externalEventClasses();
        for (EventClass cls : externalEvents) {
            ChannelId channelId = toChannelId(cls);
            Subscriber subscriber = subscriber(channelId);
            IncomingEventObserver observer = observerFor(cls);
            subscriber.removeObserver(observer);
        }
    }

    /**
     * Creates a new observer for a particular type of events.
     */
    private IncomingEventObserver observerFor(EventClass eventType) {
        IncomingEventObserver observer = new IncomingEventObserver(context(), eventType, bus);
        return observer;
    }

    /**
     * Creates an ID of the channel that will transmit the events of the given class.
     */
    private static ChannelId toChannelId(EventClass cls) {
        checkNotNull(cls);
        TypeUrl targetType = cls.typeUrl();
        return channelIdFor(targetType);
    }
}
