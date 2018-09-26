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

import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.MethodResult;
import io.spine.server.model.declare.ParameterSpec;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

/**
 * An abstract base for methods handling events.
 *
 * @author Dmytro Dashenkov
 */
public abstract class EventHandlerMethod<T, R extends MethodResult>
        extends AbstractHandlerMethod<T, EventMessage, EventClass, EventEnvelope, R> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    EventHandlerMethod(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
        super(method, parameterSpec);
    }

    @Override
    protected EventAcceptingMethodParams getParameterSpec() {
        return (EventAcceptingMethodParams) super.getParameterSpec();
    }

    @Override
    public HandlerKey key() {
        Class<?>[] types = getRawMethod().getParameterTypes();
        @SuppressWarnings("unchecked")
        Class<? extends EventMessage> eventMessageClass = (Class<? extends EventMessage>) types[0];
        EventClass eventClass = EventClass.from(eventMessageClass);
        if (!getParameterSpec().isAwareOfCommandType()) {
            return HandlerKey.of(eventClass);
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends CommandMessage> commandMessageClass =
                    (Class<? extends CommandMessage>) types[1];
            CommandClass commandClass = CommandClass.from(commandMessageClass);
            return HandlerKey.of(eventClass, commandClass);
        }
    }

    @Override
    protected void checkAttributesMatch(EventEnvelope envelope) {
        boolean external = envelope.getEventContext()
                                   .getExternal();
        ensureExternalMatch(external);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote
     * Overridden to mark {@code rawMethodOutput} argument as nullable.
     */
    @SuppressWarnings("UnnecessaryInheritDoc") // IDEA bug: `@apiNote` isn't seen as modification
    @Override
    protected abstract R toResult(T target, @Nullable Object rawMethodOutput);
}
