/*
 * Copyright 2022, TeamDev. All rights reserved.
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
package io.spine.server.model;

import com.google.common.base.Joiner;

import java.util.Collection;

import static java.lang.String.format;

/**
 * Indicates that more than one handling method for the same message class are present
 * in the declaring class.
 */
public final class DuplicateHandlerMethodError extends ModelError {

    private static final Joiner METHOD_JOINER = Joiner.on(", ");

    private static final long serialVersionUID = 0L;

    DuplicateHandlerMethodError(Class<?> declaringClass,
                                DispatchKey key,
                                String firstMethodName,
                                String secondMethodName) {
        super(format("The `%s` class defines more than one method with parameters `%s`." +
                             " Methods encountered: `%s`, `%s`.",
                     declaringClass.getName(), key,
                     firstMethodName, secondMethodName));
    }

    DuplicateHandlerMethodError(Collection<? extends HandlerMethod<?, ?, ?, ?>> handlers) {
        super(format("Handler methods %s are clashing.%n" +
                             "Only one of them should handle this message type.",
                     METHOD_JOINER.join(handlers)));
    }
}
