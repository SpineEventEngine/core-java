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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.command.Command;
import io.spine.server.model.declare.AccessModifier;
import io.spine.server.model.declare.MethodParams;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.model.declare.ParameterSpec;

import java.lang.reflect.Method;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.server.model.declare.MethodParams.consistsOfSingle;
import static io.spine.server.model.declare.MethodParams.consistsOfTwo;

/**
 * @author Alex Tymchenko
 */
public class CommandReactionSignature
        extends MethodSignature<CommandReactionMethod, EventEnvelope> {

    CommandReactionSignature() {
        super(Command.class);
    }

    @Override
    public Class<CommandReactionParams> getParamSpecClass() {
        return CommandReactionParams.class;
    }

    @Override
    protected ImmutableSet<AccessModifier> getAllowedModifiers() {
        return of(AccessModifier.PACKAGE_PRIVATE);
    }

    @Override
    protected ImmutableSet<Class<?>> getValidReturnTypes() {
        return of(Message.class, Iterable.class);
    }

    @Override
    public CommandReactionMethod doCreate(Method method,
                                          ParameterSpec<EventEnvelope> parameterSpec) {
        return new CommandReactionMethod(method, parameterSpec);
    }

    @Override
    protected boolean shouldInspect(Method method) {
        boolean parentResult = super.shouldInspect(method);

        if(parentResult) {
            Optional<CommandReactionParams> paramMatch =
                    MethodParams.findMatching(method, getParamSpecClass());
            return paramMatch.isPresent();
        }
        return false;
    }

    @Immutable
    private enum CommandReactionParams implements ParameterSpec<EventEnvelope> {

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

        MESSAGE_AND_EVENT_CONTEXT {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfTwo(methodParams, Message.class, EventContext.class);
            }

            @Override
            public Object[] extractArguments(EventEnvelope envelope) {
                return new Object[]{envelope.getMessage(), envelope.getEventContext()};
            }
        },

        MESSAGE_AND_COMMAND_CONTEXT {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfTwo(methodParams, Message.class, CommandContext.class);
            }

            @Override
            public Object[] extractArguments(EventEnvelope envelope) {
                return new Object[]{envelope, envelope.getEventContext()};
            }
        }
    }
}
