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
import org.spine3.base.Event;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.Subscribe;
import org.spine3.server.type.EventClass;
import org.spine3.server.integration.grpc.IntegrationEventSubscriberGrpc.IntegrationEventSubscriber;
import org.spine3.server.type.EventClass;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.Messages.fromAny;

/**
 * Allows to register {@link BoundedContext}s which contain integration event subscribers and
 * to deliver events to them.
 *
 * <p>An integration event is sent between different Bounded Contexts.
 *
 * @see Subscribe
 * @see EventBus
 */
public class IntegrationEventBus {

    private final Multimap<EventClass, BoundedContext> boundedContextMap = HashMultimap.create();

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
        final Collection<BoundedContext> contexts = boundedContextMap.get(eventClass);
        for (BoundedContext context : contexts) {
            context.notify(event);
        }
    }

    public void subscribe(Iterable<Class<? extends Message>> integrationEventClasses,
                          BoundedContext subscriber) {
        //TODO:2016-04-27:alexander.yevsyukov: Implement
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
