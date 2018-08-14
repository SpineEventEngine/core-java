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
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MessageAcceptor;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.MethodResult;
import io.spine.server.model.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.server.model.MethodSignatures.consistsOfSingle;
import static io.spine.server.model.MethodSignatures.consistsOfTwo;

/**
 * An abstract base for methods that accept a command message and optionally its context.
 *
 * @param <T> the type of the target object
 * @param <R> the type of the result object returned by the method
 * @author Alexander Yevsyukov
 */
@Immutable
public abstract class CommandAcceptingMethod<T, R extends MethodResult>
        extends AbstractHandlerMethod<T, CommandClass, CommandEnvelope, R> {

    CommandAcceptingMethod(Method method, MethodSignature<CommandEnvelope> signature) {
        super(method, signature);
    }

    @Override
    public CommandClass getMessageClass() {
        return CommandClass.from(rawMessageClass());
    }

    protected abstract static class Factory<H extends CommandAcceptingMethod>
            extends MethodFactory<H, CommandAcceptingSignature> {

        protected Factory(Class<? extends Annotation> annotation) {
            super(annotation, of(Message.class, Iterable.class));
        }

        @Override
        protected Class<CommandAcceptingSignature> getSignatureClass() {
            return CommandAcceptingSignature.class;
        }
    }

    enum CommandAcceptingSignature implements MethodSignature<CommandEnvelope> {

        MESSAGE {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfSingle(methodParams, Message.class);
            }

            @Override
            public Object[] extractArguments(CommandEnvelope envelope) {
                return new Object[] {envelope.getMessage()};
            }
        },

        MESSAGE_AND_CONTEXT {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfTwo(methodParams, Message.class, CommandContext.class);
            }

            @Override
            public Object[] extractArguments(CommandEnvelope envelope) {
                return new Object[] {envelope.getMessage(), envelope.getCommandContext()};
            }
        };
    }
}
