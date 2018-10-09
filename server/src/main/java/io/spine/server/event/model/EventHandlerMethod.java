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

import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerId;
import io.spine.server.model.HandlerType;
import io.spine.server.model.MethodResult;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

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
    public HandlerId id() {
        HandlerType type = selectBySignature(
                (eventClass) -> HandlerType
                        .newBuilder()
                        .setMessageType(TypeUrl.of(eventClass)
                                               .value())
                        .build(),
                (eventClass, commandClass) -> HandlerType
                        .newBuilder()
                        .setMessageType(TypeUrl.of(eventClass)
                                               .value())
                        .setOriginType(TypeUrl.of(commandClass)
                                              .value())
                        .build());
        return HandlerId
                .newBuilder()
                .setType(type)
                .build();
    }

    private <U> U
    selectBySignature(Function<Class<EventMessage>, U> forCommandUnaware,
                      BiFunction<Class<EventMessage>, Class<CommandMessage>, U> forCommandAware) {
        Class<?>[] types = getRawMethod().getParameterTypes();
        Class<EventMessage> eventMessageClass = castClass(types[0], EventMessage.class);
        if (!getParameterSpec().isAwareOfCommandType()) {
            return forCommandUnaware.apply(eventMessageClass);
        } else {
            Class<CommandMessage> commandMessageClass = castClass(types[1], CommandMessage.class);
            return forCommandAware.apply(eventMessageClass, commandMessageClass);
        }
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
    protected void checkAttributesMatch(EventEnvelope envelope) {
        boolean external = envelope.getEventContext()
                                   .getExternal();
        ensureExternalMatch(external);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote Overridden to mark the {@code rawMethodOutput} parameter as {@code Nullable}.
     */
    @SuppressWarnings("UnnecessaryInheritDoc") // IDEA bug: `@apiNote` isn't seen as modification
    @Override
    protected abstract R toResult(T target, @Nullable Object rawMethodOutput);
}
