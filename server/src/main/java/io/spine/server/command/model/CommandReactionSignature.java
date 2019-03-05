/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.server.command.Command;
import io.spine.server.model.declare.MethodParams;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.type.EventEnvelope;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.spine.server.model.declare.MethodParams.consistsOfSingle;
import static io.spine.server.model.declare.MethodParams.consistsOfTwo;

/**
 * A signature of {@link CommandReactionMethod}.
 */
public class CommandReactionSignature
        extends MethodSignature<CommandReactionMethod, EventEnvelope> {

    CommandReactionSignature() {
        super(Command.class);
    }

    @Override
    public ImmutableSet<? extends ParameterSpec<EventEnvelope>> getParamSpecs() {
        return ImmutableSet.copyOf(CommandReactionParams.values());
    }

    @Override
    protected ImmutableSet<Class<?>> getValidReturnTypes() {
        return ImmutableSet.of(CommandMessage.class, Iterable.class, Optional.class);
    }

    @Override
    public CommandReactionMethod doCreate(Method method,
                                          ParameterSpec<EventEnvelope> parameterSpec) {
        return new CommandReactionMethod(method, parameterSpec);
    }

    /**
     * {@inheritDoc}
     * <p>
     * @implNote This method distinguishes {@linkplain Command Commander} methods one from another,
     * as they use the same annotation, but have different parameter list. It skips the methods
     * which first parameter {@linkplain MethodParams#isFirstParamCommand(Method) is }
     * a {@code Command} message.
     */
    @Override
    protected boolean skipMethod(Method method) {
        boolean parentResult = !super.skipMethod(method);

        if(parentResult) {
            return MethodParams.isFirstParamCommand(method);
        }
        return true;
    }

    /**
     * Allowed combinations of parameters for {@linkplain CommandReactionMethod Command reaction}
     * methods.
     */
    @Immutable
    private enum CommandReactionParams implements ParameterSpec<EventEnvelope> {

        MESSAGE {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfSingle(methodParams, EventMessage.class);
            }

            @Override
            public Object[] extractArguments(EventEnvelope event) {
                return new Object[]{event.message()};
            }
        },

        EVENT_AND_EVENT_CONTEXT {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfTwo(methodParams, EventMessage.class, EventContext.class);
            }

            @Override
            public Object[] extractArguments(EventEnvelope event) {
                return new Object[]{event.message(), event.context()};
            }
        },

        REJECTION_AND_COMMAND_CONTEXT {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfTwo(methodParams, RejectionMessage.class, CommandContext.class);
            }

            @Override
            public Object[] extractArguments(EventEnvelope event) {
                CommandContext originContext = event.context()
                                                    .getCommandContext();
                return new Object[]{event.message(), originContext};
            }
        }
    }
}
