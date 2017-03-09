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
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.Subscribe;
import org.spine3.server.failure.FailureClass;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.lang.String.format;

/**
 * A wrapper for a failure subscriber method.
 *
 * @author Alex Tymchenko
 */
public class FailureSubscriberMethod extends HandlerMethod<CommandContext> {

    /** The instance of the predicate to filter failure subscriber methods of a class. */
    private static final MethodPredicate PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    private FailureSubscriberMethod(Method method) {
        super(method);
    }

    /**
     * Invokes the wrapped subscriber method to handle {@code failureMessage},
     * {@code commandMessage}  with the passed {@code context} of the {@code Command}.
     *
     * <p>Unlike the {@linkplain #invoke(Object, Message, Message) overloaded alternative method},
     * this one does return any value, since the failure subscriber methods are {@code void}
     * by design.
     *
     * @param target         the target object on which call the method
     * @param failureMessage the failure message to handle
     * @param commandMessage the command message
     * @param context        the context of the command
     * @throws InvocationTargetException if the wrapped method throws any {@link Throwable} that
     *                                   is not an {@link Error}.
     *                                   {@code Error} instances are propagated as-is.
     */
    public void invoke(Object target, Message failureMessage,
                       Message commandMessage, CommandContext context)
            throws InvocationTargetException {
        checkNotNull(failureMessage);
        checkNotNull(commandMessage);
        checkNotNull(context);
        try {
            final int paramCount = getParamCount();
            if (paramCount == 2) {

                getMethod().invoke(target, failureMessage, commandMessage);

            } else {
                getMethod().invoke(target, failureMessage, commandMessage, context);
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throwIfUnchecked(e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <R> R invoke(Object target, Message message, CommandContext context)
                                                            throws InvocationTargetException {
        throw new IllegalStateException("Failure handling method requires " +
                                        "at least two Message arguments. " +
                                        "See org.spine3.base.Subscribe for more details");
    }

    /**
     * Invokes the subscriber method in the passed object.
     */
    public static void invokeSubscriber(Object target, Message failureMessage,
                                        Message commandMessage, CommandContext context) {
        checkNotNull(target);
        checkNotNull(failureMessage);
        checkNotNull(commandMessage);
        checkNotNull(context);

        try {
            final FailureSubscriberMethod method = forMessage(target.getClass(),
                                                              failureMessage, commandMessage);
            method.invoke(target, failureMessage, commandMessage, context);
        } catch (InvocationTargetException e) {
            log().error("Exception handling failure. Failure message: {}, context: {}, cause: {}",
                        failureMessage, context, e.getCause());
        }
    }

    /**
     * Obtains the method for handling the failure in the passed class.
     *
     * @throws IllegalStateException if the passed class does not have an failure handling method
     *                               for the class of the passed message
     */
    public static FailureSubscriberMethod forMessage(Class<?> cls,
                                                     Message failureMessage,
                                                     Message commandMessage) {
        checkNotNull(cls);
        checkNotNull(failureMessage);
        checkNotNull(commandMessage);

        final Class<? extends Message> failureClass = failureMessage.getClass();
        final MethodRegistry registry = MethodRegistry.getInstance();
        final FailureSubscriberMethod method = registry.get(cls,
                                                            failureClass,
                                                            factory());
        if (method == null) {
            throw missingEventHandler(cls, failureClass);
        }
        return method;
    }

    static FailureSubscriberMethod from(Method method) {
        return new FailureSubscriberMethod(method);
    }

    private static IllegalStateException missingEventHandler(Class<?> cls,
                                                             Class<? extends Message> eventClass) {
        final String msg = format(
                "Missing failure handler for failure class %s in the class %s",
                eventClass, cls
        );
        return new IllegalStateException(msg);
    }

    @CheckReturnValue
    public static ImmutableSet<FailureClass> getFailureClasses(Class<?> cls) {
        checkNotNull(cls);

        final ImmutableSet<FailureClass> result =
                FailureClass.setOf(Classes.getHandledMessageClasses(cls, predicate()));
        return result;
    }

    /** Returns the factory for filtering and creating event subscriber methods. */
    private static HandlerMethod.Factory<FailureSubscriberMethod> factory() {
        return Factory.instance();
    }

    static MethodPredicate predicate() {
        return PREDICATE;
    }

    /** The factory for filtering methods that match {@code FailureSubscriberMethod} specification. */
    private static class Factory implements HandlerMethod.Factory<FailureSubscriberMethod> {

        @Override
        public Class<FailureSubscriberMethod> getMethodClass() {
            return FailureSubscriberMethod.class;
        }

        @Override
        public FailureSubscriberMethod create(Method method) {
            return from(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return predicate();
        }

        @Override
        public void checkAccessModifier(Method method) {
            if (!Modifier.isPublic(method.getModifiers())) {
                warnOnWrongModifier("Failure subscriber {} must be declared 'public'",
                                    method);
            }
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final FailureSubscriberMethod.Factory value =
                    new FailureSubscriberMethod.Factory();
        }

        private static Factory instance() {
            return Singleton.INSTANCE.value;
        }
    }

    /**
     * The predicate class allowing to filter failure subscriber methods.
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
            final boolean isParamCountCorrect = (paramCount == 2) || (paramCount == 3);
            if (!isParamCountCorrect) {
                return false;
            }
            final boolean isFirstParamMsg = Message.class.isAssignableFrom(paramTypes[0]);
            final boolean isSecondParamMsg = Message.class.isAssignableFrom(paramTypes[1]);
            if (paramCount == 2) {
                return isFirstParamMsg && isSecondParamMsg;
            } else {
                final Class<? extends Message> contextClass = getContextClass();
                final boolean paramsCorrect = isFirstParamMsg
                                              && isSecondParamMsg
                                              && contextClass.equals(paramTypes[2]);
                return paramsCorrect;
            }
        }
    }
}
