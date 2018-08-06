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
package io.spine.server.model;

import com.google.errorprone.annotations.Immutable;
import io.spine.annotation.Internal;
import io.spine.core.Subscribe;
import io.spine.server.event.React;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A meta-attribute of the {@code Method}, telling whether this method handles the objects,
 * produced outside of the current bounded context.
 *
 * @see io.spine.core.Subscribe#external()
 * @see React#external()
 * @author Alex Tymchenko
 */
@Immutable
@Internal
public enum ExternalAttribute implements MethodAttribute<Boolean> {

    /** An attribute value for the methods, designed to handle external objects only. */
    EXTERNAL(true),

    /** An attribute value for the methods, designed to handle domestic objects only. */
    DOMESTIC(false);

    private final boolean value;

    ExternalAttribute(boolean value) {
        this.value = value;
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")   // "duplicates" have different semantic.
    @Override
    public String getName() {
        return "external";
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    /**
     * Obtains the value of this attribute for a given method.
     *
     * @param method the method to inspect
     * @return the value of {@code ExternalAttribute} for the given {@code Method}
     */
    public static ExternalAttribute of(Method method) {
        checkNotNull(method);
        React reactAnnotation = method.getAnnotation(React.class);
        boolean isExternal = (reactAnnotation != null && reactAnnotation.external());
        if(!isExternal) {
            Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
            isExternal = (subscribeAnnotation != null && subscribeAnnotation.external());
        }

        return isExternal ? EXTERNAL : DOMESTIC;
    }
}
