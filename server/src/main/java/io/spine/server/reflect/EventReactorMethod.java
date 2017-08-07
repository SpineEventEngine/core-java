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
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.Events;
import io.spine.core.React;

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
public final class EventReactorMethod extends HandlerMethod<EventContext> {

    private static final MethodPredicate PREDICATE = new FilterPredicate();

    private EventReactorMethod(Method method) {
        super(method);
    }

    /**
     * Invokes reacting method for the passed event.
     *
     * @param target  the object which method to be invoked
     * @param event   the event message
     * @param context the event context
     * @return a list of produced event messages or an empty list if a target object did not
     * modified its state because of the passed event
     */
    public static List<? extends Message> invokeFor(Object target,
                                                    Message event,
                                                    EventContext context) {
        checkNotNull(target);
        checkNotNull(event);
        checkNotNull(context);
        final Message eventMessage = Events.ensureMessage(event);
        final EventReactorMethod method = getMethod(target.getClass(), eventMessage);
        return method.invoke(target, eventMessage, context);
    }

    private static EventReactorMethod getMethod(Class<?> cls, Message eventMessage) {
        final Class<? extends Message> eventClass = eventMessage.getClass();
        final HandlerMethod.Factory<EventReactorMethod> factory = factory();
        final MethodRegistry registry = MethodRegistry.getInstance();
        final EventReactorMethod method = registry.get(cls, eventClass, factory);
        if (method == null) {
            throw newIllegalStateException("The class %s does not react to events of class %s.",
                                           cls.getName(), eventClass.getName());
        }
        return method;
    }

    /**
     * {@inheritDoc}
     *
     * @return the list of event messages (or an empty list if the reactor method returns nothing)
     */
    @Override
    public <R> R invoke(Object target, Message message, EventContext context) {
        final Object handlingResult = super.invoke(target, message, context);
        final List<? extends Message> eventMessages = toList(handlingResult);
        // The list of event messages is the return type expected.
        @SuppressWarnings("unchecked") final R result = (R) eventMessages;
        return result;
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

    private static HandlerMethod.Factory<EventReactorMethod> factory() {
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
