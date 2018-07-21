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

package io.spine.server.command;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.Rejection;
import io.spine.server.entity.Entity;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.HandlerMethodFailedException;
import io.spine.server.model.HandlerMethodPredicate;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodExceptionChecker;
import io.spine.server.model.MethodPredicate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.google.common.base.Throwables.getRootCause;

/**
 * The wrapper for a command handler method.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public final class CommandHandlerMethod extends HandlerMethod<CommandClass, CommandContext> {

    /** The instance of the predicate to filter command handler methods of a class. */
    private static final MethodPredicate PREDICATE = new FilterPredicate();

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    private CommandHandlerMethod(Method method) {
        super(method);
    }

    @Override
    public CommandClass getMessageClass() {
        return CommandClass.of(rawMessageClass());
    }

    @Override
    public HandlerKey key() {
        return HandlerKey.of(getMessageClass());
    }

    static CommandHandlerMethod from(Method method) {
        return new CommandHandlerMethod(method);
    }

    static MethodPredicate predicate() {
        return PREDICATE;
    }

    public static HandlerMethod.Factory<CommandHandlerMethod> factory() {
        return Factory.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the list of event messages
     */
    @Override
    public List<? extends Message> invoke(Object target, Message message, CommandContext context) {
        Object handlingResult = super.invoke(target, message, context);
        List<? extends Message> events = toList(handlingResult);
        return events;
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@linkplain ThrowableMessage#initProducer(Any) Initializes} producer ID if the exception
     * was caused by a thrown rejection.
     */
    @Override
    protected HandlerMethodFailedException whyFailed(Object target,
                                                     Message message,
                                                     CommandContext context,
                                                     Exception cause) {
        HandlerMethodFailedException exception =
                super.whyFailed(target, message, context, cause);

        Throwable rootCause = getRootCause(exception);
        if (rootCause instanceof ThrowableMessage) {
            ThrowableMessage thrownMessage = (ThrowableMessage) rootCause;

            Optional<Any> producerId = idOf(target);
            producerId.ifPresent(thrownMessage::initProducer);
        }

        return exception;
    }

    /**
     * Obtains ID of the passed object by attempting to cast it to {@link Entity} or
     * {@link CommandHandler}.
     *
     * @return packed ID or empty optional if the object is of type for which we cannot get ID
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    private static Optional<Any> idOf(Object target) {
        Any producerId;
        if (target instanceof Entity) {
            producerId = Identifier.pack(((Entity) target).getId());
        } else if (target instanceof CommandHandler) {
            producerId = Identifier.pack(((CommandHandler) target).getId());
        } else {
            return Optional.empty();
        }
        return Optional.of(producerId);
    }

    /**
     * The factory of {@link CommandHandlerMethod}s.
     */
    private static class Factory extends HandlerMethod.Factory<CommandHandlerMethod> {

        private static final Factory INSTANCE = new Factory();

        private static Factory getInstance() {
            return INSTANCE;
        }

        @Override
        public Class<CommandHandlerMethod> getMethodClass() {
            return CommandHandlerMethod.class;
        }

        @Override
        public Predicate<Method> getPredicate() {
            return predicate();
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = MethodAccessChecker.forMethod(method);
            checker.checkPackagePrivate("Command handler method {} should be package-private.");
        }

        /**
         * {@inheritDoc}
         *
         * <p>For the {@link CommandHandlerMethod}, the {@link ThrowableMessage} checked exception
         * type is allowed, because the mechanism of {@linkplain Rejection
         * command rejections} is based on this type.
         */
        @Override
        protected void checkThrownExceptions(Method method) {
            MethodExceptionChecker checker = MethodExceptionChecker.forMethod(method);
            checker.checkThrowsNoExceptionsBut(RuntimeException.class, ThrowableMessage.class);
        }

        @Override
        protected CommandHandlerMethod createFromMethod(Method method) {
            return from(method);
        }
    }

    /**
     * The predicate that filters command handling methods.
     *
     * <p>See {@link Assign} annotation for more info about such methods.
     */
    private static class FilterPredicate extends HandlerMethodPredicate<CommandContext> {

        private FilterPredicate() {
            super(Assign.class, CommandContext.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            boolean result = returnsMessageOrIterable(method);
            return result;
        }
    }
}
