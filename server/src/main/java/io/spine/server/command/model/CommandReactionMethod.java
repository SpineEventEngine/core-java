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

import io.spine.base.EventMessage;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.server.command.model.CommandingMethod.Result;
import io.spine.server.event.EventReceiver;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodFactory;

import java.lang.reflect.Method;

import static io.spine.server.model.MethodAccessChecker.forMethod;

/**
 * A method which <em>may</em> generate one or more command messages in response to an event.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandReactionMethod
        extends AbstractHandlerMethod<EventReceiver, EventClass, EventContext, Result>
        implements CommandingMethod<EventReceiver, EventClass, EventContext, Result> {

    private CommandReactionMethod(Method method) {
        super(method);
    }

    @Override
    protected Result toResult(EventReceiver target, Object rawMethodOutput) {
        Result result = new Result(rawMethodOutput, true);
        return result;
    }

    static MethodFactory<CommandReactionMethod> factory() {
        return Factory.INSTANCE;
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(rawMessageClass());
    }

    /**
     * Obtains {@code CommandReactionMethod}s from a class.
     */
    private static final class Factory extends MethodFactory<CommandReactionMethod> {

        private static final Factory INSTANCE = new Factory();

        private Factory() {
            super(CommandReactionMethod.class, new Filter());
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPublic("Commanding event reaction `{}` must be declared `public`");
        }

        @Override
        protected CommandReactionMethod doCreate(Method method) {
            CommandReactionMethod result = new CommandReactionMethod(method);
            return result;
        }
    }

    /**
     * Recognizes methods that accept events and generate command messages.
     */
    private static final class Filter extends AbstractPredicate<EventContext> {

        private Filter() {
            super(EventContext.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            boolean result = returnsMessageIterableOrOptional(method, EventMessage.class);
            return result;
        }
    }
}
