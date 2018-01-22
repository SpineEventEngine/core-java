/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.event;

import com.google.common.base.Predicate;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MethodPredicate;

import java.lang.reflect.Method;
import java.util.List;

import static io.spine.server.model.HandlerMethods.ensureExternalMatch;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A wrapper for a method which {@linkplain React reacts} on events.
 *
 * @author Alexander Yevsyukov
 * @see React
 */
public final class EventReactorMethod extends HandlerMethod<EventContext> {

    private static final MethodPredicate PREDICATE = new FilterPredicate();

    private EventReactorMethod(Method method) {
        super(method);
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.of(rawMessageClass());
    }

    /**
     * {@inheritDoc}
     *
     * @return the list of event messages (or an empty list if the reactor method returns nothing)
     */
    @Override
    public List<? extends Message> invoke(Object target, Message message, EventContext context) {
        ensureExternalMatch(this, context.getExternal());

        final Object handlingResult = super.invoke(target, message, context);
        final List<? extends Message> eventMessages = toList(handlingResult);
        return eventMessages;
    }

    static EventReactorMethod from(Method method) {
        return new EventReactorMethod(method);
    }

    static MethodPredicate predicate() {
        return PREDICATE;
    }

    public static HandlerMethod.Factory<EventReactorMethod> factory() {
        return Factory.getInstance();
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
    private static class FilterPredicate extends EventMethodPredicate {

        private FilterPredicate() {
            super(React.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            final boolean returnsMessageOrIterable = returnsMessageOrIterable(method);
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
            final Class<?> returnType = method.getReturnType();

            // The method returns List. We're OK.
            if (Iterable.class.isAssignableFrom(returnType)) {
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
            if (firstParamType.equals(returnType)) {
                throw newIllegalStateException(
                        "React method cannot return the same event message {}",
                        firstParamType.getName());
            }
        }
    }
}
