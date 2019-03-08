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

package io.spine.testing.server.expected;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;

import java.util.List;
import java.util.function.Consumer;

/**
 * Assertions for an event reactor invocation results.
 */
public class EventReactorExpected<S extends Message>
        extends MessageProducingExpected<S, EventReactorExpected<S>> {

    public EventReactorExpected(List<? extends Message> events,
                                S initialState,
                                S state,
                                List<Message> interceptedCommands) {
        super(events, initialState, state, interceptedCommands);
    }

    @Override
    protected EventReactorExpected<S> self() {
        return this;
    }

    @CanIgnoreReturnValue
    public <M extends Message>
    EventReactorExpected<S> producesEvent(Class<M> eventClass, Consumer<M> validator) {
        return producesMessage(eventClass, validator);
    }

    @CanIgnoreReturnValue
    public EventReactorExpected<S> producesEvents(Class<?>... eventClasses) {
        return producesMessages(eventClasses);
    }
}
