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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

    /**
     * Verifiers modifiers in the methods in the passed map to be 'public'.
     *
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
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final EventHandlerMethod.Factory value = new EventHandlerMethod.Factory(); // use the FQN
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
    private static class FilterPredicate extends HandlerMethod.FilterPredicate {

        @Override
        protected boolean isAnnotatedCorrectly(Method method) {
            final boolean isAnnotated = method.isAnnotationPresent(Subscribe.class);
            return isAnnotated;
        }

        @Override
        protected boolean isReturnTypeCorrect(Method method) {
            final boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }

        @Override
        protected Class<? extends Message> getContextClass() {
            return EventContext.class;
        }
    }
}
