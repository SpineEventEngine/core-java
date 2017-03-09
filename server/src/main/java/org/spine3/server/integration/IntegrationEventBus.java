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

package org.spine3.server.integration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.EventClass;
import org.spine3.base.Response;
import org.spine3.server.event.EventBus;
import org.spine3.server.outbus.Subscribe;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.server.integration.grpc.IntegrationEventSubscriberGrpc.IntegrationEventSubscriberImplBase;

/**
 * Allows to register {@link IntegrationEvent} subscribers and deliver events to them.
 *
 * <p>An integration event is sent between loosely coupled parts of a system.
 * Typically such parts would be implemented as
 * {@link org.spine3.server.BoundedContext BoundedContext}s.
 *
 * @see Subscribe
 * @see EventBus
 */
public class IntegrationEventBus {

    private final Multimap<EventClass, IntegrationEventSubscriberImplBase> subscribersMap =
            HashMultimap.create();

    private StreamObserver<Response> responseObserver = new DefaultResponseObserver();

    /** Returns an event bus instance. */
    public static IntegrationEventBus getInstance() {
        return instance();
    }

    /** Sets a response observer used in
     * {@link IntegrationEventSubscriberImplBase#notify(IntegrationEvent, StreamObserver)}. */
    public void setResponseObserver(StreamObserver<Response> responseObserver) {
        this.responseObserver = responseObserver;
    }

    @VisibleForTesting
    StreamObserver<Response> getResponseObserver() {
        return responseObserver;
    }

    /**
     * Dispatches an integration event to subscribers.
     *
     * @param event an event to post
     */
    public void post(IntegrationEvent event) {
        final Message msg = unpack(event.getMessage());
        final EventClass eventClass = EventClass.of(msg);
        final Collection<IntegrationEventSubscriberImplBase> subscribers =
                subscribersMap.get(eventClass);
        checkArgument(!subscribers.isEmpty(),
                      "No integration event subscribers found for event " +
                                msg.getClass().getName());
        for (IntegrationEventSubscriberImplBase subscriber : subscribers) {
            subscriber.notify(event, responseObserver);
        }
    }

    /**
     * Subscribes the passed object to receive {@link IntegrationEvent}s of the specified classes.
     *
     * @param subscriber a subscriber to register (typically it is a
     *                  {@link org.spine3.server.BoundedContext BoundedContext})
     * @param eventClasses classes of integration event messages handled by the subscriber
     */
    public void subscribe(IntegrationEventSubscriberImplBase subscriber,
                          Iterable<Class<? extends Message>> eventClasses) {
        checkNotNull(subscriber);
        for (Class<? extends Message> eventClass : eventClasses) {
            subscribersMap.put(EventClass.of(eventClass), subscriber);
        }
    }

    /**
     * Subscribes the passed object to receive integration events of the specified classes.
     *
     * @param subscriber a subscriber to register
     * @param eventClasses classes of integration event messages handled by the subscriber
     */
    @SafeVarargs
    public final void subscribe(IntegrationEventSubscriberImplBase subscriber,
                                Class<? extends Message>... eventClasses) {
        subscribe(subscriber, FluentIterable.from(eventClasses));
    }

    private static class DefaultResponseObserver implements StreamObserver<Response> {
        @Override
        public void onNext(Response response) {
            log().debug("Notified integration event subscriber, response:", response);
        }

        @Override
        public void onError(Throwable throwable) {
            log().error("Error while notifying integration event subscriber:", throwable);
        }

        @Override
        public void onCompleted() {
            // Do nothing.
        }
    }

    private static IntegrationEventBus instance() {
        return BusSingleton.INSTANCE.value;
    }

    private enum BusSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final IntegrationEventBus value = new IntegrationEventBus();
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(IntegrationEventBus.class);
    }
}
