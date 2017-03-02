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

package org.spine3.server.reflect;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.EventClass;
import org.spine3.base.EventContext;
import org.spine3.server.event.Subscribe;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * A wrapper for an event subscriber method.
 *
 * @author Alexander Yevsyukov
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
    public static void invokeSubscriber(Object target, Message eventMessage, EventContext context) {
        checkNotNull(target);
        checkNotNull(eventMessage);
        checkNotNull(context);

        try {
            final EventSubscriberMethod method = forMessage(target.getClass(),
                                                            eventMessage);
            method.invoke(target, eventMessage, context);
        } catch (InvocationTargetException e) {
            throw wrappedCause(e);
        }
    }

    /**
     * Obtains the method for handling the event in the passed class.
     *
     * @throws IllegalStateException if the passed class does not have an event handling method
     *                               for the class of the passed message
     */
    public static EventSubscriberMethod forMessage(Class<?> cls, Message eventMessage) {
        checkNotNull(cls);
        checkNotNull(eventMessage);

        final Class<? extends Message> eventClass = eventMessage.getClass();
        final MethodRegistry registry = MethodRegistry.getInstance();
        final EventSubscriberMethod method = registry.get(cls,
                                                          eventClass,
                                                          factory());
        if (method == null) {
            throw missingEventHandler(cls, eventClass);
        }
        return method;
    }

    static EventSubscriberMethod from(Method method) {
        return new EventSubscriberMethod(method);
    }

    private static IllegalStateException missingEventHandler(Class<?> cls,
                                                             Class<? extends Message> eventClass) {
        final String msg = format(
                "Missing event handler for event class %s in the stream projection class %s",
                eventClass, cls
        );
        return new IllegalStateException(msg);
    }

    @CheckReturnValue
    public static ImmutableSet<EventClass> getEventClasses(Class<?> cls) {
        checkNotNull(cls);

        final ImmutableSet<EventClass> result =
                EventClass.setOf(Classes.getHandledMessageClasses(cls, predicate()));
        return result;
    }

    /** Returns the factory for filtering and creating event subscriber methods. */
    private static HandlerMethod.Factory<EventSubscriberMethod> factory() {
        return Factory.instance();
    }

    static MethodPredicate predicate() {
        return PREDICATE;
    }


    /** The factory for filtering methods that match {@code EventHandlerMethod} specification. */
    private static class Factory implements HandlerMethod.Factory<EventSubscriberMethod> {

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

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final EventSubscriberMethod.Factory value = new EventSubscriberMethod.Factory();
        }

        private static Factory instance() {
            return Singleton.INSTANCE.value;
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
