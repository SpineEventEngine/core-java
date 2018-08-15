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
import io.spine.core.EventClass;
import io.spine.server.event.EventReactor;
import io.spine.server.event.React;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.ReactorMethodResult;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.of;
import static io.spine.server.model.MethodAccessChecker.forMethod;

/**
 * A wrapper for a method which {@linkplain React reacts} on events.
 *
 * @author Alexander Yevsyukov
 * @see React
 */
public final class EventReactorMethod
        extends EventHandlerMethod<EventReactor, ReactorMethodResult> {

    private EventReactorMethod(Method method, EventAcceptingMethodParams signature) {
        super(method, signature);
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(rawMessageClass());
    }

    @Override
    protected ReactorMethodResult toResult(EventReactor target, @Nullable Object rawMethodOutput) {
        checkNotNull(rawMethodOutput,
                     "Event reactor method %s returned null.",
                     getRawMethod());
        return new ReactorMethodResult(target, rawMethodOutput);
    }

    public static MethodFactory<EventReactorMethod, ?> factory() {
        return Factory.INSTANCE;
    }

    /**
     * The factory for creating {@link EventReactorMethod event reactor} methods.
     */
    private static class Factory extends EventHandlerMethod.Factory<EventReactorMethod> {

        private static final Factory INSTANCE = new Factory();

        private Factory() {
            super(React.class, of(Message.class, Iterable.class));
        }

        @Override
        public Class<EventReactorMethod> getMethodClass() {
            return EventReactorMethod.class;
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPackagePrivate("Reactive handler method {} should be package-private.");
        }

        @Override
        protected EventReactorMethod doCreate(Method method, EventAcceptingMethodParams signature) {
            return new EventReactorMethod(method, signature);
        }
    }

}
