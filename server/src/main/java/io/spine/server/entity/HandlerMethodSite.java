/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.entity;

import com.google.common.base.Objects;
import com.google.common.flogger.LogSite;
import com.google.errorprone.annotations.Immutable;
import io.spine.code.java.ClassName;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

@Immutable
final class HandlerMethodSite extends LogSite {

    private final String fileName;
    private final ClassName name;
    private final String methodName;

    HandlerMethodSite(Method method) {
        checkNotNull(method);
        this.name = ClassName.of(method.getDeclaringClass());
        this.fileName = name.topLevelClass().value();
        String params = Stream.of(method.getParameterTypes())
                              .map(Class::getSimpleName)
                              .collect(joining(", "));
        this.methodName = format("%s(%s)", method.getName(), params);
    }

    @Override
    public String getClassName() {
        return name.canonicalName();
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public int getLineNumber() {
        return UNKNOWN_LINE;
    }

    @Override
    public String getFileName() {
        return fileName;
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
        return Objects.equal(getFileName(), site.getFileName()) &&
                Objects.equal(name, site.name) &&
                Objects.equal(getMethodName(), site.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getFileName(), name, getMethodName());
    }
}
