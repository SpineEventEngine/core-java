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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.CommandContext;
import io.spine.core.RejectionClass;
import io.spine.core.Subscribe;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.lang.String.format;

/**
 * A wrapper for a rejection subscriber method.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
@Internal
public abstract class RejectionSubscriberMethod extends HandlerMethod<CommandContext> {

    /** The instance of the predicate to filter rejection subscriber methods of a class. */
    private static final MethodPredicate PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    RejectionSubscriberMethod(Method method) {
        super(method);
    }

    /**
     * Invokes the wrapped subscriber method to handle {@code rejectionMessage},
     * {@code commandMessage} with the passed {@code context} of the {@code Command}.
     *
     * <p>Unlike the {@linkplain #invoke(Object, Message, Message) overloaded alternative method},
     * this one does return any value, since the rejection subscriber methods are {@code void}
     * by design.
     *
     * @param target           the target object on which call the method
     * @param rejectionMessage the rejection message to handle
     * @param commandMessage   the command message
     * @param context          the context of the command
     * @throws InvocationTargetException if the wrapped method throws any {@link Throwable} that
     *                                   is not an {@link Error}.
     *                                   {@code Error} instances are propagated as-is.
     */
    public void invoke(Object target,
                       Message rejectionMessage,
                       Message commandMessage,
                       CommandContext context)
            throws InvocationTargetException {
        checkNotNull(rejectionMessage);
        checkNotNull(commandMessage);
        checkNotNull(context);
        try {
            doInvoke(target, rejectionMessage, context, commandMessage);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throwIfUnchecked(e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Invokes the underlying {@link Method} with the specified set of params.
     *
     * <p>Depending on the implementation, some parameters may be omitted.
     *
     * @param target           the invocation target
     * @param rejectionMessage the rejection message parameter of the handler method
     * @param context          the {@link CommandContext} parameter of the handler method
     * @param commandMessage   the command message parameter of the handler method
     * @throws IllegalArgumentException  if thrown by the handler method invocation
     * @throws IllegalAccessException    if thrown by the handler method invocation
     * @throws InvocationTargetException if thrown by the handler method invocation
     */
    protected abstract void doInvoke(Object target,
                                     Message rejectionMessage,
                                     CommandContext context,
                                     Message commandMessage) throws IllegalArgumentException,
                                                                    IllegalAccessException,
                                                                    InvocationTargetException;

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

        try {
            final RejectionSubscriberMethod method = getMethod(target.getClass(),
                                                               rejectionMessage, commandMessage);
            method.invoke(target, rejectionMessage, commandMessage, context);
        } catch (InvocationTargetException e) {
            log().error("Exception handling a rejection. " +
                                "Rejection message: {}, context: {}, cause: {}",
                        rejectionMessage, context, e.getCause());
        }
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

    private static IllegalStateException missingRejectionHandler(
            Class<?> cls, Class<? extends Message> rejectionClass) {
        final String msg = format(
                "Missing handler for rejection class %s in the class %s",
                rejectionClass, cls
        );
        return new IllegalStateException(msg);
    }

    @CheckReturnValue
    public static Set<RejectionClass> getRejectionClasses(Class<?> cls) {
        checkNotNull(cls);
        final Set<RejectionClass> result = RejectionClass.setOf(inspect(cls, predicate()));
        return result;
    }

    /** Returns the factory for filtering and creating rejection subscriber methods. */
    private static HandlerMethod.Factory<RejectionSubscriberMethod> factory() {
        return Factory.getInstance();
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
            final Class[] paramTypes = method.getParameterTypes();
            final MethodWrapper wrapper = MethodWrapper.forParams(paramTypes);
            final RejectionSubscriberMethod result = wrapper.apply(method);
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
            return Singleton.INSTANCE.value;
        }
    }

    /**
     * A {@link Function} wrapping the given {@link Method} into a corresponding
     * {@code FailureSubscriberMethod}.
     */
    private enum MethodWrapper implements Function<Method, RejectionSubscriberMethod> {

        /**
         * Wraps the given {@link Method} with an instance of
         * {@link RejectionMessageSubscriberMethod}.
         */
        FAILURE_MESSAGE_AWARE {
            @Override
            RejectionSubscriberMethod wrap(Method method) {
                return new RejectionMessageSubscriberMethod(method);
            }
        },

        /**
         * Wraps the given {@link Method} with an instance of
         * {@link CommandContextAwareRejectionSubscriberMethod}.
         */
        COMMAND_CONTEXT_AWARE {
            @Override
            RejectionSubscriberMethod wrap(Method method) {
                return new CommandContextAwareRejectionSubscriberMethod(method);
            }
        },

        /**
         * Wraps the given {@link Method} with an instance of
         * {@link CommandMessageAwareRejectionSubscriberMethod}.
         */
        COMMAND_MESSAGE_AWARE {
            @Override
            RejectionSubscriberMethod wrap(Method method) {
                return new CommandMessageAwareRejectionSubscriberMethod(method);
            }
        },

        /**
         * Wraps the given {@link Method} with an instance of
         * {@link CommandAwareRejectionSubscriberMethod}.
         */
        COMMAND_AWARE {
            @Override
            RejectionSubscriberMethod wrap(Method method) {
                return new CommandAwareRejectionSubscriberMethod(method);
            }
        };

        /**
         * Retrieves the wrapper implementation for the given set of parameters of a rejection
         * subscriber method
         *
         * @param paramTypes the parameter types of the rejection subscriber method
         * @return the corresponding instance of {@code MethodWrapper}
         */
        private static MethodWrapper forParams(Class[] paramTypes) {
            checkNotNull(paramTypes);
            final int paramCount = paramTypes.length;
            final MethodWrapper methodWrapper;
            switch (paramCount) {
                case 1:
                    methodWrapper = FAILURE_MESSAGE_AWARE;
                    break;
                case 2:
                    final Class<?> secondParamType = paramTypes[1];
                    methodWrapper = secondParamType == CommandContext.class
                                    ? COMMAND_CONTEXT_AWARE
                                    : COMMAND_MESSAGE_AWARE;
                    break;
                case 3:
                    methodWrapper = COMMAND_AWARE;
                    break;
                default:
                    throw new IllegalArgumentException(
                            format("Invalid Rejection handler method parameter count: %s.",
                                   paramCount));
            }
            return methodWrapper;
        }

        @Override
        public RejectionSubscriberMethod apply(@Nullable Method method) {
            checkNotNull(method);
            return wrap(method);
        }

        abstract RejectionSubscriberMethod wrap(Method method);
    }

    /**
     * The predicate class allowing to filter rejection subscriber methods.
     *
     * <p>Please see {@link Subscribe} annotation for more information.
     */
    private static class FilterPredicate extends HandlerMethodPredicate<CommandContext> {

        private FilterPredicate() {
            super(Subscribe.class, CommandContext.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            final boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }

        @Override
        protected boolean verifyParams(Method method) {
            final Class<?>[] paramTypes = method.getParameterTypes();
            final int paramCount = paramTypes.length;
            final boolean paramCountCorrect = paramCount >= 1 && paramCount <= 3;
            if (!paramCountCorrect) {
                return false;
            }

            final boolean firstParamCorrect = Message.class.isAssignableFrom(paramTypes[0]);
            if (!firstParamCorrect) {
                return false;
            }
            if (paramCount == 1) {
                return true;
            }

            final boolean secondParamCorrect = Message.class.isAssignableFrom(paramTypes[1]);
            if (!secondParamCorrect) {
                return false;
            }
            if (paramCount == 2) {
                return true;
            }

            final Class<? extends Message> contextClass = getContextClass();
            final boolean thirdParamCorrect = contextClass == paramTypes[2];
            return thirdParamCorrect;
        }
    }
}
