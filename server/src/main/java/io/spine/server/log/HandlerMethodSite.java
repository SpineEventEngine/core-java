/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.log;

import com.google.common.base.Objects;
import com.google.common.flogger.LogSite;
import com.google.errorprone.annotations.Immutable;
import io.spine.code.java.ClassName;
import io.spine.code.java.SimpleClassName;
import io.spine.server.model.HandlerMethod;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * A {@link LogSite} of a signal handler method.
 *
 * <p>The site provides the name of the class and the name of the method with its parameter types.
 * The site never provides a line number or a file name.
 */
@Immutable
final class HandlerMethodSite extends LogSite {

    @SuppressWarnings("Immutable")
    private final Method method;

    HandlerMethodSite(HandlerMethod<?, ?, ?, ?> method) {
        super();
        checkNotNull(method);
        this.method = method.rawMethod();
    }

    @Override
    public String getClassName() {
        return method.getDeclaringClass()
                     .getName();
    }

    @Override
    public String getMethodName() {
        String params = Stream.of(method.getParameterTypes())
                              .map(Class::getSimpleName)
                              .collect(joining(", "));
        String methodName = format("%s(%s)", method.getName(), params);
        return methodName;
    }

    @Override
    public int getLineNumber() {
        return UNKNOWN_LINE;
    }

    @Override
    public String getFileName() {
        SimpleClassName className = ClassName.of(method.getDeclaringClass())
                                        .topLevelClass();
        return className.value() + ".java";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HandlerMethodSite)) {
            return false;
        }
        HandlerMethodSite site = (HandlerMethodSite) o;
        return Objects.equal(method, site.method);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(method);
    }
}
