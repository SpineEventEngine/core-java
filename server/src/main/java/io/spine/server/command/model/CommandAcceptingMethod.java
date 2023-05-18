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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import io.spine.base.CommandMessage;
import io.spine.base.ThrowableMessage;
import io.spine.server.EventProducer;
import io.spine.server.dispatch.Success;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.ParameterSpec;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.type.MessageClass;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * An abstract base for methods that accept a command message and optionally its context.
 *
 * @param <T>
 *         the type of the target object
 * @param <R>
 *         the type of the produced message classes
 */
@Immutable
public abstract class CommandAcceptingMethod<T extends EventProducer,
                                             R extends MessageClass<?>>
        extends AbstractHandlerMethod<T, CommandMessage, CommandClass, CommandEnvelope, R> {

    CommandAcceptingMethod(Method method, ParameterSpec<CommandEnvelope> params) {
        super(method, params);
    }

    @Override
    public final CommandClass messageClass() {
        return CommandClass.from(rawMessageClass());
    }

    /**
     * Obtains the classes of rejections thrown by this method, or empty set
     * if no rejections are thrown.
     */
    public ImmutableSet<EventClass> rejections() {
        Class<?>[] exceptionTypes = rawMethod().getExceptionTypes();
        @SuppressWarnings("unchecked") // The cast is safe as we filter before.
        ImmutableSet<EventClass> result =
                Arrays.stream(exceptionTypes)
                      .filter(ThrowableMessage.class::isAssignableFrom)
                      .map(c -> (Class<ThrowableMessage>) c)
                      .map(EventClass::fromThrowable)
                      .collect(toImmutableSet());
        return result;
    }

    @Override
    protected final Optional<Success>
    handleRejection(ThrowableMessage throwableMessage, T target, CommandEnvelope origin) {
        throwableMessage.initProducer(target.producerId());
        RejectionEnvelope envelope = RejectionEnvelope.from(origin, throwableMessage);
        Success success = Success
                .newBuilder()
                .setRejection(envelope.outerObject())
                .vBuild();
        return Optional.of(success);
    }
}
