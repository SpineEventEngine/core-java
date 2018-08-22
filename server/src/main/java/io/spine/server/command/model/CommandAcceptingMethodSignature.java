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
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.declare.AccessModifier;
import io.spine.server.model.declare.MethodSignature;
import io.spine.server.model.declare.ParameterSpec;

import java.lang.annotation.Annotation;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.server.model.declare.MethodParams.consistsOfSingle;
import static io.spine.server.model.declare.MethodParams.consistsOfTwo;

/**
 * The signature of a method, that accepts {@code Command} envelopes as parameter values.
 *
 * @author Alex Tymchenko
 */
abstract class CommandAcceptingMethodSignature<H extends HandlerMethod<?, CommandClass, CommandEnvelope, ?>>
        extends MethodSignature<H, CommandEnvelope> {

    CommandAcceptingMethodSignature(Class<? extends Annotation> annotation) {
        super(annotation);
    }

    @Override
    public Class<CommandAcceptingMethodParams> getParamSpecClass() {
        return CommandAcceptingMethodParams.class;
    }

    @Override
    protected ImmutableSet<AccessModifier> getAllowedModifiers() {
        return of(AccessModifier.PACKAGE_PRIVATE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The methods accepting commands may reject the command by throwing {@linkplain
     * ThrowableMessage command rejections} which are based on {@code ThrowableMessage}.
     */
    @Override
    protected ImmutableSet<Class<? extends Throwable>> getAllowedExceptions() {
        return of(ThrowableMessage.class);
    }

    @Override
    protected ImmutableSet<Class<?>> getValidReturnTypes() {
        return of(Message.class, Iterable.class);
    }

    @Immutable
    public enum CommandAcceptingMethodParams implements ParameterSpec<CommandEnvelope> {

        MESSAGE {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfSingle(methodParams, Message.class);
            }

            @Override
            public Object[] extractArguments(CommandEnvelope envelope) {
                return new Object[]{envelope.getMessage()};
            }
        },

        MESSAGE_AND_CONTEXT {
            @Override
            public boolean matches(Class<?>[] methodParams) {
                return consistsOfTwo(methodParams, Message.class, CommandContext.class);
            }

            @Override
            public Object[] extractArguments(CommandEnvelope envelope) {
                return new Object[]{envelope.getMessage(), envelope.getCommandContext()};
            }
        }
    }
}
