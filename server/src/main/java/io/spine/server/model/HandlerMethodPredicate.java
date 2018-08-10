/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.protobuf.Message;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * The predicate for filtering message handling methods.
 *
 * @param <C> the type of message context or {@link com.google.protobuf.Empty Empty} if
 *            a context parameter is never used
 * @author Alexander Yevsyukov
 */
public abstract class HandlerMethodPredicate<C extends Message> extends MethodPredicate {

    private final Class<? extends Annotation> annotationClass;
    private final Class<C> contextClass;

    protected HandlerMethodPredicate(Class<? extends Annotation> annotationClass,
                                     Class<C> contextClass) {
        super();
        this.annotationClass = annotationClass;
        this.contextClass = contextClass;
    }

    /**
     * Returns the context parameter class.
     */
    protected Class<C> getContextClass() {
        return contextClass;
    }

    protected Class<? extends Annotation> getAnnotationClass() {
        return annotationClass;
    }

    @Override
    protected boolean verifyAnnotation(Method method) {
        boolean isAnnotated = method.isAnnotationPresent(getAnnotationClass());
        return isAnnotated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean verifyParams(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        int paramCount = paramTypes.length;
        boolean isParamCountCorrect = (paramCount == 1) || (paramCount == 2);
        if (!isParamCountCorrect) {
            return false;
        }
        boolean isFirstParamMsg = Message.class.isAssignableFrom(paramTypes[0]);
        if (paramCount == 1) {
            return isFirstParamMsg;
        } else {
            Class<? extends Message> contextClass = getContextClass();
            boolean paramsCorrect = isFirstParamMsg && contextClass.equals(paramTypes[1]);
            return paramsCorrect;
        }
    }

    /**
     * Returns {@code true} if a method returns an instance of the class assignable from
     * {@link Message}, or {@link Iterable}.
     *
     * @param method       the method to check
     * @param messageClass the class of messages expected in the method result
     */
    protected static boolean returnsMessageOrIterable(
            Method method,
            @SuppressWarnings("unused") // will be used after message marker interfaces
                                        // are implemented
            Class<? extends Message> messageClass
    ) {
        Class<?> returnType = method.getReturnType();
        boolean isMessage = Message.class.isAssignableFrom(returnType);
        if (isMessage) {
            return true;
        }
        boolean isList = Iterable.class.isAssignableFrom(returnType);
        return isList;
    }
}
