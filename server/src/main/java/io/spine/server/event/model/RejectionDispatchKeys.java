/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.event.model;

import io.spine.base.CommandMessage;
import io.spine.server.model.DispatchKey;
import io.spine.server.type.CommandClass;
import io.spine.server.type.EventClass;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper utility class that allows creating {@link DispatchKey}s for rejection handling methods.
 */
final class RejectionDispatchKeys {

    /** Prevents instantiation of this utility class. **/
    private RejectionDispatchKeys() {
    }

    /**
     * Creates a new {@code DispatchKey} for a rejection handling method that accepts commands as
     * a second parameter.
     *
     * @implSpec the developer is responsible for verifying that the method actually has a
     *         {@code CommandClass} type as a second parameter.
     */
    static DispatchKey of(EventClass eventClass, Method rawMethod) {
        checkNotNull(eventClass);
        checkNotNull(rawMethod);
        var parameters = rawMethod.getParameterTypes();
        var methodName = rawMethod.getName();
        checkArgument(parameters.length >= 2,
                      "The method `%s` should have at least 2 parameters, but has `%s`.",
                      methodName,
                      parameters.length);
        var secondParameter = parameters[1];
        checkArgument(CommandMessage.class.isAssignableFrom(secondParameter),
                      "The method `%s` should have the second parameter assignable from `CommandMessage`, but has `%s`.",
                      methodName,
                      secondParameter);
        @SuppressWarnings("unchecked") // checked above
        var commandMessageClass = (Class<? extends CommandMessage>) secondParameter;
        var commandClass = CommandClass.from(commandMessageClass);
        return new DispatchKey(eventClass.value(), commandClass.value());
    }
}
