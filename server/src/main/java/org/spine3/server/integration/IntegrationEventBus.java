/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Event;
import org.spine3.base.Response;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.Subscribe;
import org.spine3.server.type.EventClass;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.server.integration.IntegrationEventSubscriberGrpc.IntegrationEventSubscriber;
import static org.spine3.validate.Validate.checkNotEmptyOrBlank;

/**
 * Allows to register integration event subscribers and deliver events to them.
 *
 * <p>An integration event is sent between loosely coupled parts of a system.
 * Typically such parts would be implemented as {@link BoundedContext}s.
 *
 * @see Subscribe
 * @see EventBus
 */
public class IntegrationEventBus {

    private final Multimap<EventClass, IntegrationEventSubscriber> subscribersMap = HashMultimap.create();

    private final StreamObserver<Response> observer = new StreamObserver<Response>() {
        @Override
        public void onNext(Response response) {
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onCompleted() {
        }
    };

    /**
     * Returns an event bus instance.
     */
    public static IntegrationEventBus getInstance() {
        return instance();
    }

    /**
     * Dispatches an integration event to subscribers.
     *
     * @param event an event to post
     */
    public void post(Event event) {
        final Message msg = fromAny(event.getMessage());
        final EventClass eventClass = EventClass.of(msg);
        checkNotEmptyOrBlank(event.getContext().getSource(), "integration event source");
        final Collection<IntegrationEventSubscriber> subscribers = subscribersMap.get(eventClass);
        for (IntegrationEventSubscriber subscriber : subscribers) {
            subscriber.notify(event, observer);
        }
    }

    /**
     * Subscribes the passed object to receive integration events of the specified classes.
     *
     * @param subscriber a subscriber to register
     * @param eventClasses classes of integration event messages handled by the subscriber
     */
    public void subscribe(IntegrationEventSubscriber subscriber, Iterable<Class<? extends Message>> eventClasses) {
        checkNotNull(subscriber);
        for (Class<? extends Message> eventClass : eventClasses) {
            subscribersMap.put(EventClass.of(eventClass), subscriber);
        }
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final IntegrationEventBus value = new IntegrationEventBus();
    }

    private static IntegrationEventBus instance() {
        return Singleton.INSTANCE.value;
    }
}
