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
import org.spine3.base.EventContext;
import org.spine3.server.event.Subscribe;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A wrapper for an event subscriber method.
 *
 * @author Alexander Yevsyukov
 */
public class EventSubscriberMethod extends HandlerMethod<EventContext> {

    /** The instance of the predicate to filter event subscriber methods of a class. */
    public static final Predicate<Method> PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    public EventSubscriberMethod(Method method) {
        super(method);
    }

    /**
     * Scans for event subscribers the passed target.
     *
     * @param target the target to scan
     * @return immutable map of event subscriber methods
     */
    @CheckReturnValue
    public static MethodMap<EventSubscriberMethod> scan(Object target) {
        final MethodMap<EventSubscriberMethod> result = MethodMap.create(target.getClass(), factory());
        return result;
    }

    /** Returns the factory for filtering and creating event subscriber methods. */
    public static HandlerMethod.Factory<EventSubscriberMethod> factory() {
        return Factory.instance();
    }

    /** The factory for filtering methods that match {@code EventHandlerMethod} specification. */
    private static class Factory implements HandlerMethod.Factory<EventSubscriberMethod> {

        @Override
        public Class<EventSubscriberMethod> getMethodClass() {
            return EventSubscriberMethod.class;
        }

        @Override
        public EventSubscriberMethod create(Method method) {
            return new EventSubscriberMethod(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return PREDICATE;
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
        protected boolean isReturnTypeCorrect(Method method) {
            final boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }
    }
}
