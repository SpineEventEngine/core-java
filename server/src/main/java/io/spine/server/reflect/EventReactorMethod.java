/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.reflect;

import com.google.common.base.Predicate;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.server.aggregate.React;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A wrapper for a method which {@linkplain React reacts} on events.
 *
 * @author Alexander Yevsyukov
 * @see React
 */
public class EventReactorMethod extends HandlerMethod<EventContext> {

    private static final MethodPredicate PREDICATE = new FilterPredicate();

    private EventReactorMethod(Method method) {
        super(method);
    }

    static EventReactorMethod from(Method method) {
        return new EventReactorMethod(method);
    }

    static MethodPredicate predicate() {
        return PREDICATE;
    }

    /**
     * Obtains classes of events on which instances of the passed class {@linkplain React react}.
     *
     * @param cls the class of objects that react on events
     * @return immutable set of classes of events, or empty set of none reacting methods are found
     * in the passed class
     */
    @CheckReturnValue
    public static Set<EventClass> inspect(Class<?> cls) {
        checkNotNull(cls);
        final Set<EventClass> result = EventClass.setOf(inspect(cls, predicate()));
        return result;
    }

    /**
     * The factory for creating {@link EventReactorMethod event reactor} methods.
     */
    private static class Factory implements HandlerMethod.Factory<EventReactorMethod> {

        private static final Factory INSTANCE = new Factory();

        private static Factory getInstance() {
            return INSTANCE;
        }

        @Override
        public Class<EventReactorMethod> getMethodClass() {
            return EventReactorMethod.class;
        }

        @Override
        public EventReactorMethod create(Method method) {
            return from(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return predicate();
        }

        @Override
        public void checkAccessModifier(Method method) {
            if (!isPackagePrivate(method)) {
                warnOnWrongModifier("Reactive handler method {} should be package-private.",
                                    method);
            }
        }
    }

    /**
     * The predicate that filters event reactor methods.
     */
    private static class FilterPredicate extends HandlerMethodPredicate<EventContext> {

        private FilterPredicate() {
            super(React.class, EventContext.class);
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
            // The method returns List. We're OK.
            if (List.class.isAssignableFrom(method.getReturnType())) {
                return;
            }

            // The returned value must not be of the same type as the passed message param.
            final Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length < 1) {
                /* The number of parameters is checked by `verifyParams()`.
                   We check the number of parameters here to avoid accidental exception in case
                   this method is called before `verifyParams()`. */
                return;
            }
            final Class<?> firstParamType = paramTypes[0];
            final Class<?> returnType = method.getReturnType();
            if (firstParamType.equals(returnType)) {
                throw newIllegalStateException(
                        "React method cannot return the same event message {}",
                        firstParamType.getName());
            }
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            final boolean returnsMessageOrList = returnsMessageOrList(method);
            if (returnsMessageOrList) {
                checkOutputMessageType(method);
                return true;
            }
            return false;
        }
    }
}
