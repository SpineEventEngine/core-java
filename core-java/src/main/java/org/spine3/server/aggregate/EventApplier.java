/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Message;
import org.spine3.error.AccessLevelException;
import org.spine3.internal.MessageHandlerMethod;
import org.spine3.util.Methods;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper for event applier method.
 *
 * @author Alexander Yevsyukov
 */
class EventApplier extends MessageHandlerMethod<AggregateRoot, Void> {

    private static final int EVENT_PARAM_INDEX = 0;

    static final Predicate<Method> isEventApplierPredicate = new Predicate<Method>() {
        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isEventApplier(method);
        }
    };

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    protected EventApplier(AggregateRoot target, Method method) {
        super(target, method);
    }

    @Override
    protected <R> R invoke(Message message) throws InvocationTargetException {
        return super.invoke(message);
    }

    /**
     * Checks if a method is an event applier.
     *
     * @param method to check
     * @return {@code true} if the method is an event applier, {@code false} otherwise
     */
    @SuppressWarnings("LocalVariableNamingConvention") // -- we want longer names here for clarity.
    public static boolean isEventApplier(Method method) {
        final Class<?>[] parameterTypes = method.getParameterTypes();

        boolean isAnnotated = method.isAnnotationPresent(Apply.class);
        final boolean hasOneParam = parameterTypes.length == 1;
        final boolean firstParamIsMessage = Message.class.isAssignableFrom(parameterTypes[EVENT_PARAM_INDEX]);
        final boolean returnsNothing = Void.TYPE.equals(method.getReturnType());

        //noinspection OverlyComplexBooleanExpression
        return isAnnotated
                    && hasOneParam
                    && firstParamIsMessage
                    && returnsNothing;
    }

    @Override
    protected void checkModifier() {
        boolean methodIsPrivate = isPrivate();

        if (!methodIsPrivate) {
            throw new AccessLevelException(String.format(
                    "Event applier method %s must be declared 'private'.",
                     Methods.getFullMethodName(getTarget(), getMethod())));
        }
    }

}
