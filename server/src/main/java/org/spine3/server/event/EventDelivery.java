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
package org.spine3.server.event;

import com.google.common.base.Function;
import org.spine3.Internal;
import org.spine3.base.Event;
import org.spine3.server.delivery.Delivery;
import org.spine3.type.EventClass;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Base functionality for the routines delivering the {@linkplain org.spine3.base.Event Events}
 * to event consumers, such as {@link EventDispatcher}s or {@link EventSubscriber}s.
 *
 * @param <C> the type of the consumer
 * @author Alex Tymchenko
 */
@Internal
abstract class EventDelivery<C> extends Delivery<Event, C> {

    private Function<EventClass, Set<C>> consumerProvider;

    /** {@inheritDoc} */
    EventDelivery(Executor delegate) {
        super(delegate);
    }

    /** {@inheritDoc} */
    EventDelivery() {
        super();
    }

    /** Used by the instance of {@link EventBus} to inject the knowledge about
     * up-to-date consumers for the event */
    void setConsumerProvider(Function<EventClass, Set<C>> consumerProvider) {
        this.consumerProvider = consumerProvider;
    }

    @Override
    protected final Collection<C> consumersFor(Event event) {
        final EventClass eventClass = EventClass.of(event);
        return consumerProvider.apply(eventClass);
    }
}
