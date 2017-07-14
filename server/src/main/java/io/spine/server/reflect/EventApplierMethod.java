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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A wrapper for event applier method.
 *
 * @author Alexander Yevsyukov
 */
public class EventApplierMethod extends HandlerMethod<Empty> {

    /** The instance of the predicate to filter event applier methods of an aggregate class. */
    private static final MethodPredicate PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    private EventApplierMethod(Method method) {
        super(method);
    }

    static EventApplierMethod from(Method method) {
        return new EventApplierMethod(method);
    }

    public static EventApplierMethod getMethod(Class<? extends Aggregate> cls,
                                               Message eventMessage) {
        checkNotNull(cls);
        checkNotNull(eventMessage);

        final EventApplierMethod method =
                MethodRegistry.getInstance()
                              .get(cls, eventMessage.getClass(), factory());
        if (method == null) {
            throw newIllegalStateException(
                    "Missing event applier for event class %s in aggregate class %s.",
                    eventMessage.getClass().getName(),
                    cls.getName());
        }
        return method;
    }

    @VisibleForTesting
    static MethodPredicate predicate() {
        return PREDICATE;
    }

    public static HandlerMethod.Factory<EventApplierMethod> factory() {
        return Factory.getInstance();
    }

    /**
     * Invokes the applier method.
     *
     * <p>The method {@linkplain HandlerMethod#invoke(Object, Message, Message) delegates}
     * the invocation passing {@linkplain Empty#getDefaultInstance() empty message}
     * as the context parameter because event appliers do not have a context parameter.
     * Such redirection is correct because {@linkplain #getParamCount()} the number of parameters}
     * is set to one during instance construction.
     */
    public <R> R invoke(Aggregate aggregate, Message message) {
        // Make this method visible to Aggregate class.
        return invoke(aggregate, message, Empty.getDefaultInstance());
    }

    /** The factory for filtering methods that match {@code EventApplier} specification. */
    private static class Factory implements HandlerMethod.Factory<EventApplierMethod> {

        private static final Factory INSTANCE = new Factory();

        private static Factory getInstance() {
            return INSTANCE;
        }

        @Override
        public Class<EventApplierMethod> getMethodClass() {
            return EventApplierMethod.class;
        }

        @Override
        public EventApplierMethod create(Method method) {
            return from(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return predicate();
        }

        @Override
        public void checkAccessModifier(Method method) {
            if (!Modifier.isPrivate(method.getModifiers())) {
                warnOnWrongModifier("Event applier method {} must be declared 'private'.", method);
            }
        }
    }

    /**
     * The predicate for filtering event applier methods.
     */
    private static class FilterPredicate extends HandlerMethodPredicate<Empty> {

        private static final int NUMBER_OF_PARAMS = 1;
        private static final int EVENT_PARAM_INDEX = 0;

        private FilterPredicate() {
            super(Apply.class, Empty.class);
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod") // because we override the checking.
        @Override
        protected boolean verifyParams(Method method) {
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
        protected boolean verifyReturnType(Method method) {
            final boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }
    }
}
