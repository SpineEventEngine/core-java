/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.common.reflect.TypeToken;
import io.spine.base.EventMessage;
import io.spine.server.event.React;
import io.spine.server.model.ParameterSpec;
import io.spine.server.type.EventEnvelope;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * The signature of {@link EventReactorMethod}.
 */
final class EventReactorSignature extends EventAcceptingSignature<EventReactorMethod> {

    private static final ImmutableSet<TypeToken<?>>
            RETURN_TYPES = ImmutableSet.of(
                    TypeToken.of(EventMessage.class),
                    new TypeToken<Iterable<EventMessage>>() {},
                    new TypeToken<Optional<EventMessage>>() {}
                    );

    EventReactorSignature() {
        super(React.class);
    }

    @Override
    protected ImmutableSet<TypeToken<?>> returnTypes() {
        return RETURN_TYPES;
    }

    @Override
    public EventReactorMethod create(Method method, ParameterSpec<EventEnvelope> params) {
        return new EventReactorMethod(method, params);
    }

    /**
     * Tells that the method may state that a reaction isn't needed by returning
     * {@link io.spine.server.model Nothing Nothing}.
     */
    @Override
    public boolean mayReturnIgnored() {
        return true;
    }
}
