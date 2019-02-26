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

import com.google.errorprone.annotations.Immutable;
import io.spine.base.CommandMessage;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodResult;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;

/**
 * An abstract base for methods that accept a command message and optionally its context.
 *
 * @param <T>
 *         the type of the target object
 * @param <P>
 *         the type of the produced message classes
 * @param <R>
 *         the type of the result object returned by the method
 */
@Immutable
public abstract class CommandAcceptingMethod<T,
                                             P extends MessageClass<?>,
                                             R extends MethodResult<?>>
        extends AbstractHandlerMethod<T, CommandMessage, CommandClass, CommandEnvelope, P, R> {

    CommandAcceptingMethod(Method method, ParameterSpec<CommandEnvelope> params) {
        super(method, params);
    }

    @Override
    public CommandClass getMessageClass() {
        return CommandClass.from(rawMessageClass());
    }
}
