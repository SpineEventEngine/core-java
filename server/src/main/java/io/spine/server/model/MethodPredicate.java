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

import com.google.common.base.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A filter for methods by annotation, method parameters, and return type.
 *
 * @author Alexander Yevsyukov
 */
public abstract class MethodPredicate implements Predicate<Method> {

    @Override
    public boolean apply(@Nullable Method method) {
        checkNotNull(method);
        boolean result = verifyAnnotation(method)
                         && verifyParams(method)
                         && verifyReturnType(method);
        return result;
    }

    /**
     * Returns {@code true} if the method is annotated correctly, {@code false} otherwise.
     */
    protected abstract boolean verifyAnnotation(Method method);

    /**
     * Returns {@code true} if the method return type is correct, {@code false} otherwise.
     */
    protected abstract boolean verifyReturnType(Method method);

    /**
     * Returns {@code true} if the method parameters are correct, {@code false} otherwise.
     */
    protected abstract boolean verifyParams(Method method);
}
