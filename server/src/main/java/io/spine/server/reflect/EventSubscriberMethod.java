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
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A wrapper for an event subscriber method.
 *
 * @author Alexander Yevsyukov
 * @see Subscribe
 */
public class EventSubscriberMethod extends HandlerMethod<EventContext> {

    /** The instance of the predicate to filter event subscriber methods of a class. */
    private static final MethodPredicate PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    private EventSubscriberMethod(Method method) {
        super(method);
    }

    /**
     * Invokes the subscriber method in the passed object.
     */
    public static void invokeFor(Object target, Message eventMessage, EventContext context) {
        checkNotNull(target);
        checkNotNull(eventMessage);
        checkNotNull(context);

        try {
            final EventSubscriberMethod method = getMethod(target.getClass(), eventMessage);
            method.invoke(target, eventMessage, context);
        } catch (RuntimeException e) {
            log().error("Exception handling event. Event message: {}, context: {}, cause: {}",
                        eventMessage, context, e.getCause());
        }
    }

    /**
     * Obtains the method for handling the event in the passed class.
     *
     * @throws IllegalStateException if the passed class does not have an event handling method
     *                               for the class of the passed message
     */
    public static EventSubscriberMethod getMethod(Class<?> cls, Message eventMessage) {
        checkNotNull(cls);
        checkNotNull(eventMessage);

        final Class<? extends Message> eventClass = eventMessage.getClass();
        final EventSubscriberMethod method = MethodRegistry.getInstance()
                                                           .get(cls, eventClass, factory());
        if (method == null) {
            throw newIllegalStateException("The class %s is not subscribed to events of %s.",
                                           cls.getName(), eventClass.getName());
        }
        return method;
    }

    static EventSubscriberMethod from(Method method) {
        return new EventSubscriberMethod(method);
    }

    @CheckReturnValue
    public static Set<EventClass> inspect(Class<?> cls) {
        checkNotNull(cls);
        final ImmutableSet<EventClass> result = EventClass.setOf(inspect(cls, predicate()));
        return result;
    }

    /** Returns the factory for filtering and creating event subscriber methods. */
    private static HandlerMethod.Factory<EventSubscriberMethod> factory() {
        return Factory.getInstance();
    }

    static MethodPredicate predicate() {
        return PREDICATE;
    }

    /**
     * The factory for creating {@linkplain EventSubscriberMethod event subscriber} methods.
     */
    private static class Factory implements HandlerMethod.Factory<EventSubscriberMethod> {

        private static final Factory INSTANCE = new Factory();

        private static Factory getInstance() {
            return INSTANCE;
        }

        @Override
        public Class<EventSubscriberMethod> getMethodClass() {
            return EventSubscriberMethod.class;
        }

        @Override
        public EventSubscriberMethod create(Method method) {
            return from(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return predicate();
        }

        @Override
        public void checkAccessModifier(Method method) {
            if (!Modifier.isPublic(method.getModifiers())) {
                warnOnWrongModifier("Event subscriber {} must be declared 'public'", method);
            }
        }
    }

    /**
     * The predicate class allowing to filter event subscriber methods.
     *
     * <p>Please see {@link Subscribe} annotation for more information.
     */
    private static class FilterPredicate extends HandlerMethodPredicate<EventContext> {

        private FilterPredicate() {
            super(Subscribe.class, EventContext.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            final boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }
    }
}
