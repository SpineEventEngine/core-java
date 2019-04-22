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

package io.spine.testing.server;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.entity.Entity;
import io.spine.testing.server.expected.EventSubscriberExpected;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The base class for testing the single event handling by a subscriber.
 *
 * @param <I> ID message of the event and the handling entity
 * @param <M> the type of the event message to test
 * @param <S> the type of the state message of the handling entity
 * @param <E> the type of the event subscriber being tested
 */
@CheckReturnValue
public abstract class EventSubscriptionTest<I,
                                            M extends EventMessage,
                                            S extends Message,
                                            E extends Entity<I, S>>
        extends MessageHandlerTest<I, M, S, E, EventSubscriberExpected<S>> {

    private final TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

    protected EventSubscriptionTest(I entityId, M eventMessage) {
        super(entityId, eventMessage);
    }

    @Override
    @SuppressWarnings("CheckReturnValue")
    protected EventSubscriberExpected<S> expectThat(E entity) {
        S initialState = entity.state();
        dispatchTo(entity);
        return new EventSubscriberExpected<>(initialState, entity.state());
    }

    /**
     * Creates {@link Event} from the tested message.
     */
    protected final Event createEvent() {
        EventMessage eventMessage = message();
        checkNotNull(eventMessage);
        Event result = eventFactory.createEvent(eventMessage);
        return result;
    }
}
