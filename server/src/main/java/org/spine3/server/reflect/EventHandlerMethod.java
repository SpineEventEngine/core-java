/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.server.event.Subscribe;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper for an event handling method.
 *
 * @author Alexander Yevsyukov
 */
public class EventHandlerMethod extends HandlerMethod<EventContext> {

    /**
     * The instance of the predicate to filter event handler methods of a class.
     */
    public static final Predicate<Method> PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    public EventHandlerMethod(Method method) {
        super(method);
    }

    /**
     * Scans for event handlers the passed target.
     *
     * @param target the target to scan
     * @return immutable map of event handling methods
     */
    @CheckReturnValue
    public static MethodMap<EventHandlerMethod> scan(Object target) {
        final MethodMap<EventHandlerMethod> result = MethodMap.create(target.getClass(), factory());
        return result;
    }

    @Override
    public <R> R invoke(Object target, Message message, EventContext context) throws InvocationTargetException {
        return super.invoke(target, message, context);
    }

    /**
     * Verifiers modifiers in the methods in the passed map to be 'public'.
     * <p/>
     * <p>Logs warning for the methods with a non-public modifier.
     *
     * @param methods the map of methods to check
     * @see HandlerMethod#log()
     */
    public static void checkModifiers(Iterable<Method> methods) {
        for (Method method : methods) {
            final boolean isPublic = Modifier.isPublic(method.getModifiers());
            if (!isPublic) {
                warnOnWrongModifier("Event handler {} must be declared 'public'", method);
            }
        }
    }

    /**
     * @return the factory for filtering and creating event handler methods
     */
    public static HandlerMethod.Factory<EventHandlerMethod> factory() {
        return Factory.instance();
    }

    /**
     * The factory for filtering methods that match {@code EventHandlerMethod} specification.
     */
    private static class Factory implements HandlerMethod.Factory<EventHandlerMethod> {

        @Override
        public Class<EventHandlerMethod> getMethodClass() {
            return EventHandlerMethod.class;
        }

        @Override
        public EventHandlerMethod create(Method method) {
            return new EventHandlerMethod(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return PREDICATE;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings({"NonSerializableFieldInSerializableClass", "UnnecessarilyQualifiedInnerClassAccess"})
            private final EventHandlerMethod.Factory value = new EventHandlerMethod.Factory();
        }

        private static Factory instance() {
            return Singleton.INSTANCE.value;
        }
    }

    /**
     * The predicate class allowing to filter event handling methods.
     *
     * <p>Please see {@link Subscribe} annotation for more information.
     */
    private static class FilterPredicate implements Predicate<Method> {

        private static final int MESSAGE_PARAM_INDEX = 0;
        private static final int EVENT_CONTEXT_PARAM_INDEX = 1;

        /**
         * Checks if the passed method is an event handler.
         */
        private static boolean isEventHandler(Method method) {
            if (!isAnnotatedCorrectly(method)) {
                return false;
            }
            if (!acceptsCorrectParams(method)) {
                return false;
            }
            final boolean returnsNothing = Void.TYPE.equals(method.getReturnType());
            return returnsNothing;
        }

        private static boolean acceptsCorrectParams(Method method) {
            final Class<?>[] paramTypes = method.getParameterTypes();
            final int paramCount = paramTypes.length;
            if (paramCount == 1) {
                final boolean isCorrect = Message.class.isAssignableFrom(paramTypes[MESSAGE_PARAM_INDEX]);
                return isCorrect;
            } else if (paramCount == 2) {
                final boolean paramsCorrect =
                        Message.class.isAssignableFrom(paramTypes[MESSAGE_PARAM_INDEX]) &&
                        EventContext.class.equals(paramTypes[EVENT_CONTEXT_PARAM_INDEX]);
                return paramsCorrect;
            } else {
                return false;
            }
        }

        private static boolean isAnnotatedCorrectly(Method method) {
            final boolean result = method.isAnnotationPresent(Subscribe.class);
            return result;
        }

        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isEventHandler(method);
        }
    }
}
