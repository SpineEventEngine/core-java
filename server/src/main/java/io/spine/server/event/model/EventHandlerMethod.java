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

import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.MethodResult;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

/**
 * An abstract base for methods handling events.
 *
 * @author Dmytro Dashenkov
 */
public abstract class EventHandlerMethod<T, R extends MethodResult>
        extends AbstractHandlerMethod<T, EventClass, EventEnvelope, R> {

    private final EventAcceptor acceptor;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    protected EventHandlerMethod(Method method, EventAcceptor acceptor) {
        super(method, acceptor);
        this.acceptor = acceptor;
    }

    @Override
    public HandlerKey key() {
        HandlerKey key = acceptor.createKey(getRawMethod());
        return key;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote
     * Overridden to mark {@code rawMethodOutput} argument as nullable.
     */
    @Override
    protected abstract R toResult(T target, @Nullable Object rawMethodOutput);

    protected abstract static class Factory<H extends EventHandlerMethod>
            extends MethodFactory<H, EventAcceptor> {

        protected Factory(Class<? extends Annotation> annotation,
                          Set<Class<?>> types) {
            super(annotation, types);
        }

        @Override
        protected Optional<? extends EventAcceptor>
        findAcceptorForParameters(Class<?>[] parameterTypes) {
            // Satisfy Java compiler.
            // When returning `EventAcceptor.findFor(parameterTypes)` directly, it complains because
            // of an unchecked cast.
            return EventAcceptor.findFor(parameterTypes);
        }
    }
}
