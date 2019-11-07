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

import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.DispatchKey;
import io.spine.server.model.MethodParams;
import io.spine.server.model.ParameterSpec;
import io.spine.server.type.CommandClass;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An abstract base for methods handling events.
 *
 * @param <T>
 *         the type of the target object
 * @param <R>
 *         the type of the produced message classes
 */
public abstract class EventHandlerMethod<T, R extends MessageClass<?>>
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
    protected EventAcceptingMethodParams parameterSpec() {
        return (EventAcceptingMethodParams) super.parameterSpec();
    }

    @Override
    public DispatchKey key() {
        EventClass eventClass = messageClass();
        if (parameterSpec().acceptsCommand()) {
            Class<?>[] parameters = rawMethod().getParameterTypes();
            Class<? extends CommandMessage> commandMessageClass =
                    castClass(parameters[1], CommandMessage.class);
            CommandClass commandClass = CommandClass.from(commandMessageClass);
            return new DispatchKey(eventClass.value(), null, commandClass.value());
        } else {
            return super.key();
        }
    }

    @Override
    public MethodParams params() {
        if (parameterSpec().acceptsCommand()) {
            MethodParams result = MethodParams.of(rawMethod());
            return result;
        }
        return super.params();
    }

    private static <T extends Message> Class<T> castClass(Class<?> raw, Class<T> targetBound) {
        checkArgument(targetBound.isAssignableFrom(raw));
        @SuppressWarnings("unchecked") // Checked above.
        Class<T> result = (Class<T>) raw;
        return result;
    }

    /**
     * Ensures that the passed event matches the {@code external} attribute
     * of the method annotation.
     *
     * <p>If the method annotated to accept {@code external} events, the event passed to the handler
     * method must be produced {@linkplain io.spine.core.EventContext#getExternal() outside} of
     * the Bounded Context to which the handling entity belongs.
     *
     * <p>And vice versa, if the event handling method is designed for domestic events,
     * it does not accept external events.
     *
     * @see io.spine.core.Subscribe#external()
     * @see io.spine.server.event.React#external()
     */
    @Override
    protected void checkAttributesMatch(EventEnvelope event) {
        boolean external = event.context()
                                .getExternal();
        ensureExternalMatch(external);
    }

}