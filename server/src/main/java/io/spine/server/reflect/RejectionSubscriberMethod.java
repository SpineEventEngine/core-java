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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.unsupported;
import static java.lang.String.format;

/**
 * A wrapper for a rejection subscriber method.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 * @author Alexander Yevsyukov
 */
@Internal
public class RejectionSubscriberMethod extends HandlerMethod<CommandContext> {

    /** The instance of the predicate to filter rejection subscriber methods of a class. */
    private static final MethodPredicate PREDICATE = new FilterPredicate();

    /** Determines the number of parameters and their types. */
    private final Kind kind;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    @VisibleForTesting
    RejectionSubscriberMethod(Method method) {
        super(method);
        this.kind = getKind(method);
    }

    private static Kind getKind(Method method) {
        final Class[] paramTypes = method.getParameterTypes();
        final int paramCount = paramTypes.length;
        switch (paramCount) {
            case 1:
                return Kind.REJECTION_MESSAGE_AWARE;
            case 2:
                final Class<?> secondParamType = paramTypes[1];
                return  secondParamType == CommandContext.class
                        ? Kind.COMMAND_CONTEXT_AWARE
                        : Kind.COMMAND_MESSAGE_AWARE;
            case 3:
                return Kind.COMMAND_AWARE;
            default:
                throw newIllegalArgumentException(
                        "Invalid Rejection handler method parameter count: %s.", paramCount);
        }
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
     */
    public void invoke(Object target,
                       Message rejectionMessage,
                       Message commandMessage,
                       CommandContext context) {
        checkNotNull(rejectionMessage);
        checkNotNull(commandMessage);
        checkNotNull(context);
        try {
            final Method method = getMethod();
            switch (kind) {
                case REJECTION_MESSAGE_AWARE:
                    method.invoke(target, rejectionMessage);
                    break;
                case COMMAND_CONTEXT_AWARE:
                    method.invoke(target, rejectionMessage, context);
                    break;
                case COMMAND_MESSAGE_AWARE:
                    method.invoke(target, rejectionMessage, commandMessage);
                    break;
                case COMMAND_AWARE:
                    method.invoke(target, rejectionMessage, commandMessage, context);
                    break;
            }
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw whyFailed(target, rejectionMessage, context, e);
        }
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
        throw unsupported("Call invoke(Object, Message, Message, CommandContext) instead.");
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

        final RejectionSubscriberMethod method = getMethod(target.getClass(),
                                                           rejectionMessage, commandMessage);
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

    private static IllegalStateException missingRejectionHandler(
            Class<?> cls, Class<? extends Message> rejectionClass) {
        final String msg = format(
                "Missing handler for rejection class %s in the class %s",
                rejectionClass, cls
        );
        return new IllegalStateException(msg);
    }

    @CheckReturnValue
    public static Set<RejectionClass> inspect(Class<?> cls) {
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
            return Singleton.INSTANCE.value;
        }
    }

    /**
     * The kind of the method signature, which is determined by the number of parameters and
     * their types.
     */
    private enum Kind {

        /**
         * A wrapper of a rejection subscriber method which receives a rejection message
         * as a single parameter.
         *
         * <p>The signature of such a method is following:
         * <pre>
         * {@literal @}Subscribe
         * public void on(RejectionMessage rejection);
         * </pre>
         * where {@code RejectionMessage} is a specific generated rejection message class.
         */
        REJECTION_MESSAGE_AWARE,

        /**
         * A rejection subscriber method aware of the {@link CommandContext}.
         *
         * <p>The signature of such a method is following:
         * <pre>
         * {@literal @}Subscribe
         * public void on(RejectionMessage rejection, CommandContext context);
         * </pre>
         * where {@code RejectionMessage} is a specific generated rejection message class.
         */
        COMMAND_CONTEXT_AWARE,

        /**
         * A rejection subscriber method aware of the command message.
         *
         * <p>The signature of such a method is following:
         * <pre>
         * {@literal @}Subscribe
         * public void on(RejectionMessage rejection, CommandMessage command);
         * </pre>
         * where {@code RejectionMessage} is a specific generated rejection message class, and
         * {@code CommandMessage} is a specific generated command message class.
         */
        COMMAND_MESSAGE_AWARE,

        /**
         * A rejection subscriber method aware of both the command message and
         * the {@link CommandContext}.
         *
         * <p>The signature of such a method is following:
         * <pre>
         * {@literal @}Subscribe
         * public void on(RejectionMessage rejection, CommandMessage command, CommandContext context);
         * </pre>
         * where {@code RejectionMessage} is a specific generated rejection message class, and
         * {@code CommandMessage} is a specific generated command message class.
         */
        COMMAND_AWARE
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
