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

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.command.Commander;
import io.spine.server.command.model.CommandingMethod.Result;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.MethodSignature;

import java.lang.reflect.Method;

import static io.spine.server.model.MethodAccessChecker.forMethod;
import static io.spine.server.model.MethodSignatures.consistsOfSingle;
import static io.spine.server.model.MethodSignatures.consistsOfTwo;

/**
 * A method which <em>may</em> generate one or more command messages in response to an event.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandReactionMethod
        extends AbstractHandlerMethod<Commander, EventClass, EventEnvelope, Result>
        implements CommandingMethod<EventClass, EventEnvelope, Result> {

    private CommandReactionMethod(Method method, MethodSignature<EventEnvelope> signature) {
        super(method, signature);
    }

    @Override
    protected Result toResult(Commander target, Object rawMethodOutput) {
        Result result = new Result(rawMethodOutput, true);
        return result;
    }

    static MethodFactory<CommandReactionMethod, ?> factory() {
        return Factory.INSTANCE;
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(rawMessageClass());
    }

    /**
     * Obtains {@code CommandReactionMethod}s from a class.
     */
    private static final class Factory
            extends CommandingMethod.Factory<CommandReactionMethod, CommandReactionSignature> {

        private static final Factory INSTANCE = new Factory();

        private Factory() {
            super();
        }

        @Override
        public Class<CommandReactionMethod> getMethodClass() {
            return CommandReactionMethod.class;
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPackagePrivate(
                    "Commanding event reaction `{}` must be declared package-private`"
            );
        }

        @Override
        protected CommandReactionMethod doCreate(Method method,
                                                 CommandReactionSignature signature) {
            return new CommandReactionMethod(method, signature);
        }

        @Override
        protected Class<CommandReactionSignature> getSignatureClass() {
            return CommandReactionSignature.class;
        }
    }

    @Immutable
    private enum CommandReactionSignature implements MethodSignature<EventEnvelope> {

        MESSAGE {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfSingle(methodParams, Message.class);
            }

            @Override
            public Object[] extractArguments(EventEnvelope envelope) {
                return new Object[]{envelope.getMessage()};
            }
        },

        MESSAGE_AND_CONTEXT {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfTwo(methodParams, Message.class, EventContext.class);
            }

            @Override
            public Object[] extractArguments(EventEnvelope envelope) {
                return new Object[]{envelope, envelope.getEventContext()};
            }
        }
    }
}
