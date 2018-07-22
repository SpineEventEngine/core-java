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
package io.spine.server.rejection;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.Commands;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionContext;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.HandlerMethodPredicate;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Rejections.isRejection;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.unsupported;

/**
 * A base class for methods, that handle rejections.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 * @author Alexander Yevsyukov
 */
@Internal
abstract class RejectionHandlerMethod extends AbstractHandlerMethod<RejectionClass, RejectionContext> {

    /** Determines the number of parameters and their types. */
    private final Kind kind;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method handler method
     */
    RejectionHandlerMethod(Method method) {
        super(method);
        this.kind = getKind(method);
    }

    @Override
    public RejectionClass getMessageClass() {
        return RejectionClass.of(rawMessageClass());
    }

    @Override
    public HandlerKey key() {
        if (kind == Kind.COMMAND_AWARE || kind == Kind.COMMAND_MESSAGE_AWARE) {
            @SuppressWarnings("unchecked") // RejectionFilterPredicate ensures that
            Class<? extends Message> rawCommandClass =
                    (Class<? extends Message>) getMethod().getParameterTypes()[1];
            return HandlerKey.of(getMessageClass(), CommandClass.of(rawCommandClass));
        } else {
            return HandlerKey.of(getMessageClass());
        }
    }

    private static Kind getKind(Method method) {
        Class[] paramTypes = method.getParameterTypes();
        int paramCount = paramTypes.length;
        switch (paramCount) {
            case 1:
                return Kind.REJECTION_MESSAGE_AWARE;
            case 2:
                Class<?> secondParamType = paramTypes[1];
                if (secondParamType.equals(CommandContext.class)) {
                    return Kind.COMMAND_CONTEXT_AWARE;
                }
                if (secondParamType.equals(RejectionContext.class)) {
                    return Kind.REJECTION_CONTEXT_AWARE;
                }
                return  Kind.COMMAND_MESSAGE_AWARE;
            case 3:
                return Kind.COMMAND_AWARE;
            default:
                throw newIllegalArgumentException(
                        "Invalid Rejection handler method parameter count: %s.", paramCount);
        }
    }

    /**
     * Invokes the wrapped handler method to handle {@code rejectionMessage},
     * {@code commandMessage} with the passed {@code context} of the {@code Command}.
     *
     * <p>Unlike the {@linkplain #invoke(Object, Message, Message) overloaded alternative method},
     * this one may return some value.
     *
     * @param  target       the target object on which call the method
     * @param  rejectionMsg the rejection message to handle
     * @param  context      the context of the rejection
     * @return the result of the invocation
     */
    @SuppressWarnings("OverlyLongMethod")
    Object doInvoke(Object target, Message rejectionMsg, RejectionContext context) {
        checkNotNull(target);
        checkNotNull(rejectionMsg);
        checkNotNull(context);
        Command command = context.getCommand();
        CommandContext commandContext = command.getContext();
        try {
            Object output;
            Method method = getMethod();
            Message commandMessage;
            switch (kind) {
                case REJECTION_MESSAGE_AWARE:
                    output = method.invoke(target, rejectionMsg);
                    break;
                case REJECTION_CONTEXT_AWARE:
                    output = method.invoke(target, rejectionMsg, context);
                    break;
                case COMMAND_CONTEXT_AWARE:
                    output = method.invoke(target, rejectionMsg, commandContext);
                    break;
                case COMMAND_MESSAGE_AWARE:
                    commandMessage = Commands.getMessage(command);
                    output = method.invoke(target, rejectionMsg, commandMessage);
                    break;
                case COMMAND_AWARE:
                    commandMessage = Commands.getMessage(command);
                    output = method.invoke(target, rejectionMsg, commandMessage, commandContext);
                    break;
                default:
                    throw unsupported("Unsupported method kind encountered %s", kind.name());
            }
            return output;
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw whyFailed(target, rejectionMsg, context, e);
        }
    }

    /**
     * The kind of the method signature, which is determined by the number of parameters and
     * their types.
     *
     * <p>Depending on a descendant, the signatures should be used with a particular
     * handler method annotation.
     */
    protected enum Kind {

        /**
         * A rejection handler method which receives a rejection message
         * as a single parameter.
         *
         * <p>The signature of such a method is as follows, if used in {@link RejectionSubscriber}:
         * <pre>
         * {@literal @}Subscribe
         * public void on(RejectionMessage rejection);
         * </pre>
         *
         * Or if it is an {@code Entity} reactor method, it is declared as:
         * <pre>
         * {@literal @}React
         * public void on(RejectionMessage rejection);
         * </pre>
         *
         * where {@code RejectionMessage} is a specific generated rejection message class.
         */
        REJECTION_MESSAGE_AWARE,

        /**
         * A rejection handler method which receives a rejection message as the first parameter,
         * and {@link io.spine.core.RejectionContext RejectionContext} as the second.
         *
         * <p>The signature of such a method is as follows, if used in {@link RejectionSubscriber}:
         * <pre>
         * {@literal @}Subscribe
         * public void on(RejectionMessage rejection, RejectionContext context);
         * </pre>
         *
         * Or if it is an {@code Entity} reactor method, it is declared as:
         * <pre>
         * {@literal @}React
         * public void on(RejectionMessage rejection, RejectionContext context);
         * </pre>
         *
         * where {@code RejectionMessage} is a specific generated rejection message class.
         */
        REJECTION_CONTEXT_AWARE,

        /**
         * A rejection handler method aware of the {@link CommandContext}.
         *
         * <p>The signature of such a method is as follows, if used in {@link RejectionSubscriber}:
         * <pre>
         * {@literal @}Subscribe
         * public void on(RejectionMessage rejection, CommandContext context);
         * </pre>
         *
         * Or if it is an {@code Entity} reactor method, it is declared as:
         * <pre>
         * {@literal @}React
         * public void on(RejectionMessage rejection, CommandContext context);
         * </pre>
         *
         * where {@code RejectionMessage} is a specific generated rejection message class.
         */
        COMMAND_CONTEXT_AWARE,

        /**
         * A rejection handler method aware of the command message.
         *
         * <p>The signature of such a method is as follows, if used in {@link RejectionSubscriber}:
         * <pre>
         * {@literal @}Subscribe
         * public void on(RejectionMessage rejection, CommandMessage command);
         * </pre>
         *
         * Or if it is an {@code Entity} reactor method, it is declared as:
         * <pre>
         * {@literal @}React
         * public void on(RejectionMessage rejection, CommandMessage command);
         * </pre>
         *
         * where {@code RejectionMessage} is a specific generated rejection message class, and
         * {@code CommandMessage} is a specific generated command message class.
         */
        COMMAND_MESSAGE_AWARE,

        /**
         * A rejection handler method aware of both the command message and
         * the {@link CommandContext}.
         *
         * <p>The signature of such a method is as follows, if used in {@link RejectionSubscriber}:
         * <pre>
         * {@literal @}Subscribe
         * public void on(RejectionMessage rejection, CommandMessage command, CommandContext ctx);
         * </pre>
         *
         * Or if it is an {@code Entity} reactor method, it is declared as:
         * <pre>
         * {@literal @}React
         * public void on(RejectionMessage rejection, CommandMessage command, CommandContext ctx);
         * </pre>
         *
         * where {@code RejectionMessage} is a specific generated rejection message class, and
         * {@code CommandMessage} is a specific generated command message class.
         */
        COMMAND_AWARE
    }

    /**
     * The abstract base for predicates allowing to filter rejection handler methods.
     */
    protected abstract static class AbstractFilterPredicate
            extends HandlerMethodPredicate<CommandContext> {

        AbstractFilterPredicate(Class<? extends Annotation> annotationClass) {
            super(annotationClass, CommandContext.class);
        }

        @Override
        protected boolean verifyParams(Method method) {
            Class<?>[] paramTypes = method.getParameterTypes();
            int paramCount = paramTypes.length;
            boolean paramCountCorrect = paramCount >= 1 && paramCount <= 3;
            if (!paramCountCorrect) {
                return false;
            }

            boolean firstParamIsMessage = Message.class.isAssignableFrom(paramTypes[0]);
            @SuppressWarnings("unchecked")  // checked above.
            boolean firstParamCorrect =
                    firstParamIsMessage
                            && isRejection((Class<? extends Message>) paramTypes[0]);
            if (!firstParamCorrect) {
                return false;
            }
            if (paramCount == 1) {
                return true;
            }

            boolean secondParamCorrect = Message.class.isAssignableFrom(paramTypes[1]);
            if (!secondParamCorrect) {
                return false;
            }
            if (paramCount == 2) {
                return true;
            }

            Class<? extends Message> contextClass = getContextClass();
            boolean thirdParamCorrect = contextClass == paramTypes[2];
            return thirdParamCorrect;
        }
    }
}
