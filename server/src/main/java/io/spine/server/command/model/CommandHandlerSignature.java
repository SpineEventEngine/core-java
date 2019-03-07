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
import io.spine.base.EventMessage;
import io.spine.server.command.Assign;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.type.CommandEnvelope;

import java.lang.reflect.Method;

/**
 * The signature of {@code Command} handler method.
 */
public final class CommandHandlerSignature
        extends CommandAcceptingMethodSignature<CommandHandlerMethod> {

    public CommandHandlerSignature() {
        super(Assign.class);
    }

    @Override
    protected ImmutableSet<Class<?>> validReturnTypes() {
        return ImmutableSet.of(EventMessage.class, Iterable.class);
    }

    @Override
    public CommandHandlerMethod doCreate(Method method,
                                         ParameterSpec<CommandEnvelope> parameterSpec) {
        return new CommandHandlerMethod(method, parameterSpec);
    }
}
