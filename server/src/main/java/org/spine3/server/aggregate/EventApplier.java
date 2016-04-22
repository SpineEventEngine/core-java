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

package org.spine3.server.aggregate;

import com.google.common.base.Predicate;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import org.spine3.server.reflect.HandlerMethod;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper for event applier method.
 *
 * @author Alexander Yevsyukov
 */
/* package */ class EventApplier extends HandlerMethod<Empty> {

    /**
     * The instance of the predicate to filter event applier methods of an aggregate class.
     */
    /* package */ static final Predicate<Method> PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    /* package */ EventApplier(Method method) {
        super(method);
    }

    @Override
    protected <R> R invoke(Object aggregate, Message message) throws InvocationTargetException {
        // Make this method visible to Aggregate class.
        return super.invoke(aggregate, message);
    }

    /**
     * Verifiers modifiers in the methods in the passed map to be 'private'.
     *
     * <p>Logs warning for the methods with a non-private modifier.
     *
     * @param methods the map of methods to check
     * @see HandlerMethod#log()
     */
    /* package */ static void checkModifiers(Iterable<Method> methods) {
        for (Method method : methods) {
            final boolean isPrivate = Modifier.isPrivate(method.getModifiers());
            if (!isPrivate) {
                warnOnWrongModifier("Event applier method {} must be declared 'private'.", method);
            }
        }
    }

    public static HandlerMethod.Factory<EventApplier> factory() {
        return Factory.instance();
    }

    /**
     * The factory for filtering methods that match {@code EventApplier} specification.
     */
    private static class Factory implements HandlerMethod.Factory<EventApplier> {

        @Override
        public Class<EventApplier> getMethodClass() {
            return EventApplier.class;
        }

        @Override
        public EventApplier create(Method method) {
            return new EventApplier(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return PREDICATE;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings({"NonSerializableFieldInSerializableClass", "UnnecessarilyQualifiedInnerClassAccess"})
            private final EventApplier.Factory value = new EventApplier.Factory();
        }

        private static Factory instance() {
            return Singleton.INSTANCE.value;
        }
    }

    /**
     * The predicate for filtering event applier methods.
     */
    private static class FilterPredicate implements Predicate<Method> {

        private static final int EVENT_PARAM_INDEX = 0;

        private static final int NUMBER_OF_PARAMS = 1;

        /**
         * Checks if a method is an event applier.
         *
         * @param method to check
         * @return {@code true} if the method is an event applier, {@code false} otherwise
         */
        public static boolean isEventApplier(Method method) {
            final boolean isAnnotated = method.isAnnotationPresent(Apply.class);
            if (!isAnnotated) {
                return false;
            }

            final Class<?>[] parameterTypes = method.getParameterTypes();
            final boolean paramCountValid = parameterTypes.length == NUMBER_OF_PARAMS;
            if (!paramCountValid) {
                return false;
            }

            final boolean firstParamIsMessage = Message.class.isAssignableFrom(parameterTypes[EVENT_PARAM_INDEX]);
            final boolean returnsNothing = Void.TYPE.equals(method.getReturnType());

            return firstParamIsMessage && returnsNothing;
        }

        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isEventApplier(method);
        }
    }
}
