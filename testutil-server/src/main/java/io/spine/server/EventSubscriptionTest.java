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

package io.spine.server;

import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.Entity;
import io.spine.server.expected.AbstractExpected;

/**
 * The base class for testing the single event handling by a subscriber.
 *
 * @param <I> ID message of the event and the handling entity
 * @param <M> the type of the event message to test
 * @param <S> state message of the handling entity
 * @param <E> the type of the event subscriber being tested
 *
 * @author Vladyslav Lubenskyi
 */
public abstract class EventSubscriptionTest<I,
                                            M extends Message,
                                            S extends Message,
                                            E extends Entity<I, S>>
        extends MessageHandlerTest<I, M, S, E, EventSubscriptionTest.Expected<S>> {

    private final TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

    @Override
    @SuppressWarnings("CheckReturnValue")
    protected Expected<S> expectThat(E entity) {
        S initialState = entity.getState();
        dispatchTo(entity);
        return new Expected<>(initialState, entity.getState());
    }

    /**
     * Creates {@link Event} from the tested message.
     */
    protected final Event createEvent() {
        Message eventMessage = message();
        Event result = eventFactory.createEvent(eventMessage);
        return result;
    }

    public static class Expected<S extends Message> extends AbstractExpected<S, Expected<S>> {

        private Expected(S initialState, S state) {
            super(initialState, state);
        }

        @Override
        protected Expected<S> self() {
            return this;
        }
    }
}
