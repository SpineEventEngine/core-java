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
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.CommandContext;
import io.spine.core.RejectionClass;
import io.spine.core.Subscribe;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.unsupported;

/**
 * A wrapper for a rejection subscriber method.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 * @author Alexander Yevsyukov
 */
@Internal
public class RejectionSubscriberMethod extends RejectionHandlerMethod {

    /** The instance of the predicate to filter rejection subscriber methods of a class. */
    private static final MethodPredicate PREDICATE = new FilterPredicate();

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
     * Invokes the subscriber method in the passed object.
     */
    public static void invokeFor(Object target,
                                 Message rejectionMessage,
                                 Message commandMessage,
                                 CommandContext context) {
        checkNotNull(target);
        checkNotNull(rejectionMessage);
        checkNotNull(commandMessage);
        checkNotNull(context);

        final RejectionSubscriberMethod method =
                getMethod(target.getClass(), rejectionMessage, commandMessage);
        method.invoke(target, rejectionMessage, commandMessage, context);
    }

    /**
     * Obtains the method for handling the rejection in the passed class.
     *
     * @throws IllegalStateException if the passed class does not have an rejection handling method
     *                               for the class of the passed message
     */
    public static RejectionSubscriberMethod getMethod(Class<?> cls,
                                                      Message rejectionMessage,
                                                      Message commandMessage) {
        checkNotNull(cls);
        checkNotNull(rejectionMessage);
        checkNotNull(commandMessage);

        final Class<? extends Message> rejectionClass = rejectionMessage.getClass();
        final MethodRegistry registry = MethodRegistry.getInstance();
        final RejectionSubscriberMethod method = registry.get(cls,
                                                              rejectionClass,
                                                              factory());
        if (method == null) {
            throw missingRejectionHandler(cls, rejectionClass);
        }
        return method;
    }

    @CheckReturnValue
    public static Set<RejectionClass> inspect(Class<?> cls) {
        return inspectWith(cls, predicate());
    }

    /**
     * Invokes the wrapped handler method to handle {@code rejectionMessage},
     * {@code commandMessage} with the passed {@code context} of the {@code Command}.
     *
     * @param target           the target object on which call the method
     * @param rejectionMessage the rejection message to handle
     * @param commandMessage   the command message
     * @param context          the context of the command
     */
    public void invoke(Object target,
                       Message rejectionMessage,
                       Message commandMessage,
                       CommandContext context) {
        doInvoke(target, rejectionMessage, commandMessage, context);
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     * Call {@link #invoke(Object, Message, Message, CommandContext)} instead.
     *
     * @return nothing ever
     * @throws IllegalStateException always
     */
    @Override
    public <R> R invoke(Object target, Message message, CommandContext context) {
        throw unsupported("Call an overloaded" +
                                  " `invoke(Object, Message, Message, CommandContext)` instead.");
    }

    /** Returns the factory for filtering and creating rejection subscriber methods. */
    private static HandlerMethod.Factory<RejectionSubscriberMethod> factory() {
        return RejectionSubscriberMethod.Factory.getInstance();
    }

    static MethodPredicate predicate() {
        return PREDICATE;
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
            return predicate();
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

        private FilterPredicate() {
            super(Subscribe.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            final boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }
    }

}
