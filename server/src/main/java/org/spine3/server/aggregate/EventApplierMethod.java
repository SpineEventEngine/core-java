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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A wrapper for event applier method.
 *
 * @author Alexander Yevsyukov
 */
/* package */ class EventApplierMethod extends HandlerMethod<Empty> {

    /**
     * The instance of the predicate to filter event applier methods of an aggregate class.
     */
    /* package */ static final Predicate<Method> PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    /* package */ EventApplierMethod(Method method) {
        super(method);
    }

    /**
     * Invokes {@link HandlerMethod#invoke(Object, Message, Message)} passing the default message as the context parameter
     * as event appliers do not have this parameter.
     */
    protected <R> R invoke(Object aggregate, Message message) throws InvocationTargetException {
        // Make this method visible to Aggregate class.
        return super.invoke(aggregate, message, Empty.getDefaultInstance());
    }

    /**
     * This method is deprecated because event appliers do not have context parameter.
     *
     * <p>Please use {@link EventApplierMethod#invoke(Object, Message)} instead.
     */
    @Deprecated
    @Override
    public <R> R invoke(Object target, Message message, Empty context) throws InvocationTargetException {
        return super.invoke(target, message, Empty.getDefaultInstance());
    }

    public static HandlerMethod.Factory<EventApplierMethod> factory() {
        return Factory.instance();
    }

    /**
     * The factory for filtering methods that match {@code EventApplier} specification.
     */
    private static class Factory implements HandlerMethod.Factory<EventApplierMethod> {

        @Override
        public Class<EventApplierMethod> getMethodClass() {
            return EventApplierMethod.class;
        }

        @Override
        public EventApplierMethod create(Method method) {
            return new EventApplierMethod(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return PREDICATE;
        }

        @Override
        public void checkAccessModifier(Method method) {
            if (!Modifier.isPrivate(method.getModifiers())) {
                warnOnWrongModifier("Event applier method {} must be declared 'private'.", method);
            }
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings({"NonSerializableFieldInSerializableClass", "UnnecessarilyQualifiedInnerClassAccess"})
            private final EventApplierMethod.Factory value = new EventApplierMethod.Factory(); // use the FQN
        }

        private static Factory instance() {
            return Singleton.INSTANCE.value;
        }
    }

    /**
     * The predicate for filtering event applier methods.
     */
    private static class FilterPredicate extends HandlerMethod.FilterPredicate {

        private static final int NUMBER_OF_PARAMS = 1;
        private static final int EVENT_PARAM_INDEX = 0;

        @Override
        @SuppressWarnings("RefusedBequest")
        protected boolean acceptsCorrectParams(Method method) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final boolean paramCountIsValid = parameterTypes.length == NUMBER_OF_PARAMS;
            if (!paramCountIsValid) {
                return false;
            }
            final Class<?> paramType = parameterTypes[EVENT_PARAM_INDEX];
            final boolean paramIsMessage = Message.class.isAssignableFrom(paramType);
            return paramIsMessage;
        }

        @Override
        protected boolean isAnnotatedCorrectly(Method method) {
            final boolean isAnnotated = method.isAnnotationPresent(Apply.class);
            return isAnnotated;
        }

        @Override
        protected boolean isReturnTypeCorrect(Method method) {
            final boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }

        @Override
        protected Class<? extends Message> getContextClass() {
            return Empty.class;
        }
    }
}
