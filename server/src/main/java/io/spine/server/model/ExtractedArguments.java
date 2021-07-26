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

package io.spine.server.model;

import com.google.errorprone.annotations.Immutable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public final class ExtractedArguments {

    @SuppressWarnings("Immutable") // This array is never changed or exposed.
    private final Object[] args;

    private ExtractedArguments(Object[] args) {
        this.args = args;
    }

    public static ExtractedArguments ofOne(Object arg) {
        checkNotNull(arg);
        return new ExtractedArguments(new Object[]{arg});
    }

    public static ExtractedArguments ofTwo(Object first, Object second) {
        checkNotNull(first);
        checkNotNull(second);
        return new ExtractedArguments(new Object[]{first, second});
    }

    public static ExtractedArguments ofTree(Object first, Object second, Object third) {
        checkNotNull(first);
        checkNotNull(second);
        checkNotNull(third);
        return new ExtractedArguments(new Object[]{first, second, third});
    }

    Object invokeMethod(Method method, Object receiver)
            throws InvocationTargetException, IllegalAccessException {
        return method.invoke(receiver, args);
    }
}
