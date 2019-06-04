/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.model.ModelClass;
import io.spine.server.type.EmptyClass;
import io.spine.server.type.EventClass;
import io.spine.type.MessageClass;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides type information on an {@link AbstractEventSubscriber} class.
 *
 * @param <S>
 *         the type of event subscribers
 */
public final class EventSubscriberClass<S extends AbstractEventSubscriber> extends ModelClass<S>
    implements EventReceiverClass, SubscribingClass {

    private static final long serialVersionUID = 0L;

    private final EventReceivingClassDelegate<S, EmptyClass, SubscriberMethod> delegate;

    private EventSubscriberClass(Class<S> cls) {
        super(cls);
        this.delegate = new EventReceivingClassDelegate<>(cls, new SubscriberSignature());
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
    public Set<EventClass> domesticEvents() {
        return delegate.domesticEvents();
    }

    @Override
    public Set<EventClass> externalEvents() {
        return delegate.externalEvents();
    }

    @Override
    public
    Collection<SubscriberMethod> subscribersOf(EventClass eventClass, MessageClass originClass) {
        return delegate.handlersOf(eventClass, originClass);
    }
}
