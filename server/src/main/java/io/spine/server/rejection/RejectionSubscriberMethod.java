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
package io.spine.server.rejection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.RejectionContext;
import io.spine.core.Subscribe;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MethodPredicate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A wrapper for a rejection subscriber method.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 * @author Alexander Yevsyukov
 */
@Internal
public class RejectionSubscriberMethod extends RejectionHandlerMethod {

    /** The instance of the predicate to filter domestic rejection subscriber methods of a class. */
    private static final MethodPredicate DOMESTIC_SUBSCRIBERS = new FilterPredicate();

    /** The instance of the predicate to filter external rejection subscriber methods of a class. */
    private static final MethodPredicate EXTERNAL_SUBSCRIBERS = new FilterPredicate(true);


    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    @VisibleForTesting
    RejectionSubscriberMethod(Method method) {
        super(method);
    }

    /**
     * Invokes the wrapped handler method to handle {@code rejectionMessage},
     * {@code commandMessage} with the passed {@code context} of the {@code Command}.
     *
     * @param target
     *        the target object on which call the method
     * @param rejectionMessage
     *        the rejection message to handle
     * @param context
     *        the context of the rejection
     */
    @Override
    public Object invoke(Object target, Message rejectionMessage, RejectionContext context) {
        final Object result = doInvoke(target, rejectionMessage, context);
        return result;
    }

    /** Returns the factory for filtering and creating rejection subscriber methods. */
    public static HandlerMethod.Factory<RejectionSubscriberMethod> factory() {
        return Factory.getInstance();
    }

//
//    /**
//     * Invokes the subscriber method in the passed object.
//     */
//    public static void invokeFor(Object target,
//                                 Message rejectionMessage,
//                                 Message commandMessage,
//                                 RejectionContext context) {
//        checkNotNull(target);
//        checkNotNull(rejectionMessage);
//        checkNotNull(commandMessage);
//        checkNotNull(context);
//
//        final RejectionSubscriberMethod method =
//                getMethod(target.getClass(), rejectionMessage, commandMessage);
//        method.invoke(target, rejectionMessage, context);
//    }
//
//    /**
//     * Obtains the method for handling the rejection in the passed class.
//     *
//     * @throws IllegalStateException if the passed class does not have an rejection handling method
//     *                               for the class of the passed message
//     */
//    public static RejectionSubscriberMethod getMethod(Class<?> cls,
//                                                      Message rejectionMessage,
//                                                      Message commandMessage) {
//        checkNotNull(cls);
//        checkNotNull(rejectionMessage);
//        checkNotNull(commandMessage);
//
//        final Class<? extends Message> rejectionClass = rejectionMessage.getClass();
//        final MethodRegistry registry = MethodRegistry.getInstance();
//        final RejectionSubscriberMethod method = registry.get(cls,
//                                                              rejectionClass,
//                                                              factory());
//        if (method == null) {
//            throw missingRejectionHandler(cls, rejectionClass);
//        }
//        return method;
//    }
//
//    @CheckReturnValue
//    public static Set<RejectionClass> inspect(Class<?> cls) {
//        return inspectWith(cls, domesticSubscribers());
//    }
//
//    @CheckReturnValue
//    public static Set<RejectionClass> inspectExternal(Class<?> cls) {
//        return inspectWith(cls, externalSubscribers());
//    }

    static MethodPredicate domesticSubscribers() {
        return DOMESTIC_SUBSCRIBERS;
    }

    //TODO:2017-07-21:alex.tymchenko: cover external subscribers with tests as well.
    static MethodPredicate externalSubscribers() {
        return EXTERNAL_SUBSCRIBERS;
    }

    /**
     * The factory for filtering methods that match {@code RejectionSubscriberMethod} specification.
     */
    private static class Factory implements HandlerMethod.Factory<RejectionSubscriberMethod> {

        @Override
        public Class<RejectionSubscriberMethod> getMethodClass() {
            return RejectionSubscriberMethod.class;
        }

        @Override
        public RejectionSubscriberMethod create(Method method) {
            final RejectionSubscriberMethod result = new RejectionSubscriberMethod(method);
            return result;
        }

        @Override
        public Predicate<Method> getPredicate() {
            return Predicates.or(domesticSubscribers(), externalSubscribers());
        }

        @Override
        public void checkAccessModifier(Method method) {
            if (!Modifier.isPublic(method.getModifiers())) {
                warnOnWrongModifier("Rejection subscriber {} must be declared 'public'",
                                    method);
            }
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final Factory value = new Factory();
        }

        private static Factory getInstance() {
            return Factory.Singleton.INSTANCE.value;
        }
    }

    /**
     * The predicate class allowing to filter rejection subscriber methods.
     *
     * <p>Please see {@link Subscribe} annotation for more information.
     */
    private static class FilterPredicate extends RejectionFilterPredicate {

        private final boolean externalOnly;

        private FilterPredicate() {
            this(false);
        }

        private FilterPredicate(boolean externalOnly) {
            super(Subscribe.class);
            this.externalOnly = externalOnly;
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            final boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }

        @Override
        protected boolean verifyAnnotation(Method method) {
            return super.verifyAnnotation(method) && matchesExternal(externalOnly, method);
        }

        private boolean matchesExternal(boolean externalOnly, Method method) {
            final Annotation annotation = method.getAnnotation(getAnnotationClass());
            final Subscribe subscribeAnnotation = (Subscribe) annotation;
            final boolean result = externalOnly == subscribeAnnotation.external();
            return result;
        }
    }

}
