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
import io.spine.core.React;
import io.spine.server.event.EventReactor;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodPredicate;
import io.spine.server.model.ReactorMethodResult;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static io.spine.server.model.MethodAccessChecker.forMethod;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A wrapper for a method which {@linkplain React reacts} on events.
 *
 * @author Alexander Yevsyukov
 * @see React
 */
public final class EventReactorMethod
        extends EventHandlerMethod<EventReactor, ReactorMethodResult> {

    private EventReactorMethod(Method method) {
        super(method);
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.of(rawMessageClass());
    }

    @Override
    public HandlerKey key() {
        return HandlerKey.of(getMessageClass());
    }

    @Override
    protected ReactorMethodResult toResult(EventReactor target, Object rawMethodOutput) {
        return new ReactorMethodResult(target, rawMethodOutput);
    }

    static EventReactorMethod from(Method method) {
        return new EventReactorMethod(method);
    }

    public static AbstractHandlerMethod.Factory<EventReactorMethod> factory() {
        return Factory.INSTANCE;
    }

    /**
     * The factory for creating {@link EventReactorMethod event reactor} methods.
     */
    private static class Factory extends AbstractHandlerMethod.Factory<EventReactorMethod> {

        private static final Factory INSTANCE = new Factory();

        @Override
        public Class<EventReactorMethod> getMethodClass() {
            return EventReactorMethod.class;
        }

        @Override
        public Predicate<Method> getPredicate() {
            return Filter.INSTANCE;
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPackagePrivate("Reactive handler method {} should be package-private.");
        }

        @Override
        protected EventReactorMethod doCreate(Method method) {
            return from(method);
        }
    }

    /**
     * The predicate that filters event reactor methods.
     */
    private static class Filter extends EventMethodPredicate {

        private static final MethodPredicate INSTANCE = new Filter();

        private Filter() {
            super(React.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            boolean returnsMessageOrIterable = returnsMessageOrIterable(method);
            if (returnsMessageOrIterable) {
                checkOutputMessageType(method);
                return true;
            }
            return false;
        }

        /**
         * Ensures that the method does not return the same class of message as one that passed
         * as the first method parameter.
         *
         * <p>This protection is needed to prevent repeated events passed to Event Store.
         *
         * @throws IllegalStateException if the type of the first parameter is the same as
         *                               the return value
         */
        private static void checkOutputMessageType(Method method) {
            Class<?> returnType = method.getReturnType();

            // The method returns List. We're OK.
            if (Iterable.class.isAssignableFrom(returnType)) {
                return;
            }

            // The returned value must not be of the same type as the passed message param.
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length < 1) {
                /* The number of parameters is checked by `verifyParams()`.
                   We check the number of parameters here to avoid accidental exception in case
                   this method is called before `verifyParams()`. */
                return;
            }
            Class<?> firstParamType = paramTypes[0];
            if (firstParamType.equals(returnType)) {
                throw newIllegalStateException(
                        "React method cannot return the same event message {}",
                        firstParamType.getName());
            }
        }
    }
}
