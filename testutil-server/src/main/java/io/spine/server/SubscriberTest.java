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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.Entity;

import java.util.List;

/**
 * A base class for testing the single event handling by a subscriber.
 *
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("TestOnlyProblems")
public abstract class SubscriberTest<M extends Message,
                                     I,
                                     S extends Message,
                                     E extends Entity<I, S>>
        extends MessageHandlerTest<M, I, S, E, SubscriberTest.Expected<S>> {

    private final TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

    @Override
    @CanIgnoreReturnValue
    protected Expected<S> expectThat(E entity) {
        final S initialState = entity.getState();
        dispatchTo(entity);
        return new Expected<>(initialState, entity.getState());
    }

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
        Expected<S> self() {
            return this;
        }
    }
}
