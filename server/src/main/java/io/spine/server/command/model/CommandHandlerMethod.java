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

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandEnvelope;
import io.spine.server.EventProducer;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.model.EventsResult;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodExceptionChecker;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.MethodSignature;
import io.spine.server.procman.ProcessManager;

import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * The wrapper for a command handler method.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandHandlerMethod
        extends CommandAcceptingMethod<CommandHandler, CommandHandlerMethod.Result> {

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method   command handler method
     * @param signature the {@link MethodSignature} describing the method signature
     */
    private CommandHandlerMethod(Method method,
                                 MethodSignature<CommandEnvelope> signature) {
        super(method, signature);
    }

    static CommandHandlerMethod from(Method method,
                                     MethodSignature<CommandEnvelope> signature) {
        return new CommandHandlerMethod(method, signature);
    }

    public static MethodFactory<CommandHandlerMethod, ?> factory() {
        return Factory.INSTANCE;
    }

    /**
     * Transforms the passed raw method output into a list of event messages.
     */
    @Override
    protected Result toResult(CommandHandler target, Object rawMethodOutput) {
        return new Result(target, rawMethodOutput);
    }

    /**
     * The factory of {@link CommandHandlerMethod}s.
     */
    private static final class Factory extends CommandAcceptingMethod.Factory<CommandHandlerMethod> {

        private static final Factory INSTANCE = new Factory();

        private Factory() {
            super(Assign.class);
        }

        @Override
        public Class<CommandHandlerMethod> getMethodClass() {
            return CommandHandlerMethod.class;
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
        protected CommandHandlerMethod doCreate(Method method,
                                                CommandAcceptingSignature signature) {
            return from(method, signature);
        }
    }

    /**
     * The result of a command handler method execution.
     */
    public static final class Result extends EventsResult {

        private Result(EventProducer producer, Object rawMethodResult) {
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
