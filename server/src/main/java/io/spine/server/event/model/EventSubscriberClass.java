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

package io.spine.server.event.model;

import com.google.common.collect.ImmutableSet;
import io.spine.core.EventClass;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.model.ModelClass;
import io.spine.type.MessageClass;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides type information on an {@link AbstractEventSubscriber} class.
 *
 * @param <S> the type of event subscribers
 * @author Alexander Yevsyukov
 */
public final class EventSubscriberClass<S extends AbstractEventSubscriber> extends ModelClass<S>
    implements EventReceiverClass, SubscribingClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<EventClass, EventSubscriberMethod> eventSubscriptions;
    private final ImmutableSet<EventClass> domesticSubscriptions;
    private final ImmutableSet<EventClass> externalSubscriptions;

    private EventSubscriberClass(Class<? extends S> cls) {
        super(cls);
        this.eventSubscriptions = MessageHandlerMap.create(cls, new EventSubscriberSignature());
        this.domesticSubscriptions =
                    eventSubscriptions.getMessageClasses(HandlerMethod::isDomestic);
        this.externalSubscriptions =
                    eventSubscriptions.getMessageClasses(HandlerMethod::isExternal);
    }

    /**
     * Creates new instance for the passed raw class.
     */
    public static <S extends AbstractEventSubscriber>
    EventSubscriberClass<S> asEventSubscriberClass(Class<S> cls) {
        checkNotNull(cls);
        EventSubscriberClass<S> result = (EventSubscriberClass<S>)
                get(cls, EventSubscriberClass.class, () -> new EventSubscriberClass<>(cls));
        return result;
    }

    @Override
    public Set<EventClass> getEventClasses() {
        return domesticSubscriptions;
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        return externalSubscriptions;
    }

    @Override
    public EventSubscriberMethod getSubscriber(EventClass eventClass, MessageClass originClass) {
        return eventSubscriptions.getMethod(eventClass, originClass);
    }
}
