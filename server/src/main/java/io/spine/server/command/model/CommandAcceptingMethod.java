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
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodResult;

import java.lang.reflect.Method;

/**
 * An abstract base for methods that accept a command message and optionally its context.
 *
 * @param <R> the type of the result object returned by the method
 * @author Alexander Yevsyukov
 */
@Immutable
public abstract class CommandAcceptingMethod<T, R extends MethodResult>
        extends AbstractHandlerMethod<T, CommandClass, CommandEnvelope, R> {

    CommandAcceptingMethod(Method method) {
        super(method);
    }

    @Override
    public CommandClass getMessageClass() {
        return CommandClass.of(rawMessageClass());
    }
}
