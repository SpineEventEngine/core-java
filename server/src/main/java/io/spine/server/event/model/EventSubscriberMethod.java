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

import com.google.protobuf.Empty;
import io.spine.core.EventClass;
import io.spine.core.Subscribe;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.MethodResult;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.server.model.MethodAccessChecker.forMethod;

/**
 * A wrapper for an event subscriber method.
 *
 * @author Alexander Yevsyukov
 * @see Subscribe
 */
public final class EventSubscriberMethod
        extends EventHandlerMethod<Object, MethodResult<Empty>> {

    /** Creates a new instance. */
    private EventSubscriberMethod(Method method, EventAcceptor acceptor) {
        super(method, acceptor);
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(rawMessageClass());
    }

    public static EventSubscriberMethod from(Method method, EventAcceptor acceptor) {
        return new EventSubscriberMethod(method, acceptor);
    }

    /**
     * Returns the factory for filtering and creating event subscriber methods.
     */
    public static MethodFactory<EventSubscriberMethod, ?> factory() {
        return Factory.INSTANCE;
    }

    @Override
    protected MethodResult<Empty> toResult(Object target, @Nullable Object rawMethodOutput) {
        return MethodResult.empty();
    }

    /**
     * The factory for creating {@linkplain EventSubscriberMethod event subscriber} methods.
     */
    private static class Factory extends EventHandlerMethod.Factory<EventSubscriberMethod> {

        private static final Factory INSTANCE = new Factory();

        private Factory() {
            super(Subscribe.class, of(void.class));
        }

        @Override
        public Class<EventSubscriberMethod> getMethodClass() {
            return EventSubscriberMethod.class;
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPublic("Event subscriber `{}` must be declared `public`");
        }

        @Override
        protected EventSubscriberMethod doCreate(Method method, EventAcceptor acceptor) {
            return from(method, acceptor);
        }
    }
}
