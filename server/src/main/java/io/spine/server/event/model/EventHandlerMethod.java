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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Dmytro Dashenkov
 */
abstract class EventHandlerMethod<T, R extends MethodResult>
        extends AbstractHandlerMethod<T, EventClass, EventEnvelope, R> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    protected EventHandlerMethod(Method method) {
        super(method);
    }

    /**
     * {@inheritDoc}
     *
     * @return the list of event messages (or an empty list if the reactor method returns nothing)
     */
    @CanIgnoreReturnValue
    @Override
    public R invoke(T target, EventEnvelope event) {
        EventContext context = event.getEventContext();
        ensureExternalMatch(context.getExternal());

        Object rawResult;
        try {
            rawResult = EventAcceptor.accept(target, getRawMethod(), event);
        } catch (InvocationTargetException e) {
            throw whyFailed(target, event.getMessage(), context, e);
        }
        R result = toResult(target, rawResult);
        return result;
    }
}
