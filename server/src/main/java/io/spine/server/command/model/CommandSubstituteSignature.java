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
import io.spine.base.CommandMessage;
import io.spine.server.command.Command;
import io.spine.server.model.declare.MethodParams;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.type.CommandEnvelope;

import java.lang.reflect.Method;

/**
 * A signature of {@link io.spine.server.command.model.CommandSubstituteMethod
 * CommandSubstituteMethod}.
 */
public class CommandSubstituteSignature
        extends CommandAcceptingMethodSignature<CommandSubstituteMethod> {

    CommandSubstituteSignature() {
        super(Command.class);
    }

    @Override
    public CommandSubstituteMethod doCreate(Method method,
                                            ParameterSpec<CommandEnvelope> parameterSpec) {
        return new CommandSubstituteMethod(method, parameterSpec);
    }

    @Override
    protected ImmutableSet<Class<?>> validReturnTypes() {
        return ImmutableSet.of(CommandMessage.class, Iterable.class);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This method distinguishes {@linkplain Command Commander} methods one from
     * another, as they use the same annotation, but have different parameter list. It skips
     * the methods which first parameter {@linkplain MethodParams#isFirstParamCommand(Method)
     * is NOT} a {@code Command} message.
     */
    @SuppressWarnings("UnnecessaryInheritDoc") // IDEA bug.
    @Override
    protected boolean skipMethod(Method method) {
        boolean parentResult = !super.skipMethod(method);

        if (parentResult) {
            return !MethodParams.isFirstParamCommand(method);
        }
        return true;
    }
}
