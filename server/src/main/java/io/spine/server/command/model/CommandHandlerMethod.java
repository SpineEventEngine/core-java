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

package io.spine.server.command.model;

import com.google.common.base.Throwables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.RejectionEventContext;
import io.spine.server.EventProducer;
import io.spine.server.command.Assign;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.EventsResult;
import io.spine.server.model.HandlerMethodPredicate;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodExceptionChecker;
import io.spine.server.procman.ProcessManager;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getRootCause;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.command.Rejection.causedByRejection;

/**
 * The wrapper for a command handler method.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandHandlerMethod
        extends CommandAcceptingMethod<EventProducer, CommandHandlerMethod.Result> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    private CommandHandlerMethod(Method method) {
        super(method);
    }

    static CommandHandlerMethod from(Method method) {
        return new CommandHandlerMethod(method);
    }

    public static AbstractHandlerMethod.Factory<CommandHandlerMethod> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Transforms the passed raw method output into a list of event messages.
     */
    @Override
    protected Result toResult(EventProducer target, Object rawMethodOutput) {
        Result result = NormalCommandHandlerResult.of(rawMethodOutput, target);
        return result;
    }

    @CanIgnoreReturnValue
    @Override
    public Result invoke(EventProducer target, CommandEnvelope envelope) {
        try {
            return super.invoke(target, envelope);
        } catch (RuntimeException e) {
            return handleError(e, envelope.getMessage(), target);
        }
    }

    private static Result handleError(RuntimeException exception,
                                      Message commandMessage,
                                      EventProducer target) {
        boolean rejection = causedByRejection(exception);
        if (rejection) {
            ThrowableMessage throwableMessage = (ThrowableMessage) getRootCause(exception);
            RejectionEventContext context = rejectionContext(commandMessage, throwableMessage);
            return RejectionCommandHandlerResult.of(throwableMessage, context, target);
        } else {
            throw exception;
        }
    }

    private static RejectionEventContext rejectionContext(Message commandMessage,
                                                          ThrowableMessage throwableMessage) {
        String stacktrace = Throwables.getStackTraceAsString(throwableMessage);
        RejectionEventContext result = RejectionEventContext
                .newBuilder()
                .setCommandMessage(pack(commandMessage))
                .setStacktrace(stacktrace)
                .build();
        return result;
    }

    /**
     * The factory of {@link CommandHandlerMethod}s.
     */
    private static class Factory extends AbstractHandlerMethod.Factory<CommandHandlerMethod> {

        private static final Factory INSTANCE = new Factory();

        @Override
        public Class<CommandHandlerMethod> getMethodClass() {
            return CommandHandlerMethod.class;
        }

        @Override
        public Predicate<Method> getPredicate() {
            return Filter.INSTANCE;
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
         * type is allowed, because the mechanism of {@linkplain ThrowableMessage
         * command rejections} is based on this type.
         */
        @Override
        protected void checkThrownExceptions(Method method) {
            MethodExceptionChecker checker = MethodExceptionChecker.forMethod(method);
            checker.checkThrowsNoExceptionsBut(ThrowableMessage.class);
        }

        @Override
        protected CommandHandlerMethod doCreate(Method method) {
            return from(method);
        }
    }

    /**
     * The predicate that filters command handling methods.
     *
     * <p>See {@link Assign} annotation for more info about such methods.
     */
    private static class Filter extends HandlerMethodPredicate<CommandContext> {

        private static final Filter INSTANCE = new Filter();

        private Filter() {
            super(Assign.class, CommandContext.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            boolean result = returnsMessageOrIterable(method);
            return result;
        }
    }

    /**
     * The result of a command handler method execution.
     */
    public abstract static class Result extends EventsResult {

        Result(EventProducer producer, Object rawMethodResult) {
            super(producer, rawMethodResult);
            List<Message> eventMessages = toMessages(rawMethodResult);
            List<Message> filtered = filterEmpty(eventMessages);
            ensureNotEmptyIfNotProcessManager(filtered, rawMethodResult, producer);
            setMessages(filtered);
        }

        /**
         * Ensures that a command handler method produces one or more event messages.
         *
         * <p>The only allowed exception to this are {@link ProcessManager} instances returning
         * {@link Empty} from their command handler methods.
         *
         * @param eventMessages  the events produced as the result of the command handling
         * @param handlingResult the result of the command handler method invocation
         * @param target         the target on which the method was executed
         * @throws IllegalStateException if the command handling method did not produce any events
         */
        private static void ensureNotEmptyIfNotProcessManager(List<? extends Message> eventMessages,
                                                              Object handlingResult,
                                                              Object target) {

            //TODO:2018-07-25:dmytro.kuzmin: Prohibit returning `Empty` from `ProcessManager` in favor
            // of "Expected<...>" construction.
            // See https://github.com/SpineEventEngine/core-java/issues/790.
            boolean procmanReturnedEmpty =
                    handlingResult instanceof Empty && target instanceof ProcessManager;
            checkState(!eventMessages.isEmpty() || procmanReturnedEmpty,
                       "Command handling method did not produce events");
        }
    }
}
