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

import io.spine.base.CommandMessage;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.server.command.CommandReceiver;
import io.spine.server.command.model.CommandingMethod.Result;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodExceptionChecker;
import io.spine.server.model.MethodFactory;

import java.lang.reflect.Method;

/**
 * A method that produces one or more command messages in response to an incoming command.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandSubstituteMethod
        extends CommandAcceptingMethod<CommandReceiver, Result>
        implements CommandingMethod<CommandReceiver, CommandClass, CommandContext, Result> {

    private CommandSubstituteMethod(Method method) {
        super(method);
    }

    @Override
    protected Result toResult(CommandReceiver target, Object rawMethodOutput) {
        Result result = new Result(rawMethodOutput, false);
        return result;
    }

    static MethodFactory<CommandSubstituteMethod> factory() {
        return Factory.INSTANCE;
    }

    private static final class Factory
            extends MethodFactory<CommandSubstituteMethod> {

        private static final Factory INSTANCE = new Factory();

        private Factory() {
            super(CommandSubstituteMethod.class, new Filter());
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = MethodAccessChecker.forMethod(method);
            checker.checkPackagePrivate(
                    "Command substitution method {} should be package-private."
            );
        }

        @Override
        protected void checkThrownExceptions(Method method) {
            MethodExceptionChecker checker = MethodExceptionChecker.forMethod(method);
            checker.checkThrowsNoExceptionsBut(ThrowableMessage.class);
        }

        @Override
        protected CommandSubstituteMethod doCreate(Method method) {
            return new CommandSubstituteMethod(method);
        }
    }

    /**
     * Filters command substitution methods.
     */
    private static final class Filter extends AbstractPredicate<CommandContext> {

        private Filter() {
            super(CommandContext.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            boolean result = returnsMessageOrIterable(method, CommandMessage.class);
            return result;
        }
    }
}
