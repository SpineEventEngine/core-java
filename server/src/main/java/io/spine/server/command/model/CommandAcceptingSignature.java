/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import io.spine.base.CommandMessage;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandContext;
import io.spine.server.model.AllowedParams;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MethodParams;
import io.spine.server.model.MethodSignature;
import io.spine.server.model.ParameterSpec;
import io.spine.server.model.TypeMatcher;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

import java.lang.annotation.Annotation;
import java.util.Optional;

import static io.spine.server.model.TypeMatcher.classImplementing;
import static io.spine.server.model.TypeMatcher.exactly;

/**
 * The signature of a method, that accepts {@code Command} envelopes as parameter values.
 *
 * @param <H> the type of {@link HandlerMethod} which signature this is
 */
abstract class CommandAcceptingSignature
        <H extends HandlerMethod<?, CommandClass, CommandEnvelope, ?>>
        extends MethodSignature<H, CommandEnvelope> {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // to save on allocations.
    private static final Optional<Class<? extends Throwable>>
            ALLOWED_THROWABLE = Optional.of(ThrowableMessage.class);

    CommandAcceptingSignature(Class<? extends Annotation> annotation) {
        super(annotation);
    }

    @Override
    public AllowedParams<CommandEnvelope> params() {
        return CommandAcceptingMethodParams.ALLOWED;
    }

    /**
     * Returns {@code ThrowableMessage.class} wrapped into {@code Optional}.
     *
     * <p>The methods accepting commands may reject the command by throwing {@linkplain
     * ThrowableMessage command rejections} which are based on {@code ThrowableMessage}.
     */
    @Override
    protected Optional<Class<? extends Throwable>> allowedThrowable() {
        return ALLOWED_THROWABLE;
    }

    /**
     * Tells that a command-accepting method never returns an ignored result.
     */
    @Override
    public boolean mayReturnIgnored() {
        return false;
    }

    /**
     * Allowed combinations of parameters in the methods, that accept {@code Command}s.
     */
    @Immutable
    private enum CommandAcceptingMethodParams implements ParameterSpec<CommandEnvelope> {

        MESSAGE(classImplementing(CommandMessage.class)) {

            @Override
            public Object[] extractArguments(CommandEnvelope envelope) {
                return new Object[]{envelope.message()};
            }
        },

        MESSAGE_AND_CONTEXT(classImplementing(CommandMessage.class),
                            exactly(CommandContext.class)) {
            @Override
            public Object[] extractArguments(CommandEnvelope cmd) {
                return new Object[]{cmd.message(), cmd.context()};
            }
        };

        private static final AllowedParams<CommandEnvelope> ALLOWED = new AllowedParams<>(values());

        private final ImmutableList<TypeMatcher> criteria;

        CommandAcceptingMethodParams(TypeMatcher... criteria) {
            this.criteria = ImmutableList.copyOf(criteria);
        }

        @Override
        public boolean matches(MethodParams params) {
            return params.match(criteria);
        }
    }
}
