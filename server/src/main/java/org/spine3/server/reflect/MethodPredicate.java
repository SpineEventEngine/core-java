/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.reflect;

import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * A filter for methods by annotation, method parameters, and return type.
 *
 * @author Alexander Yevsyukov
 */
public abstract class MethodPredicate implements Predicate<Method> {

    @Override
    public boolean apply(@Nullable Method method) {
        if (method == null) {
            return false;
        }
        final boolean result = isAnnotatedCorrectly(method)
                               && acceptsCorrectParams(method)
                               && isReturnTypeCorrect(method);
        return result;
    }

    /**
     * Returns {@code true} if the method is annotated correctly, {@code false} otherwise.
     */
    protected abstract boolean isAnnotatedCorrectly(Method method);

    /**
     * Returns {@code true} if the method return type is correct, {@code false} otherwise.
     */
    protected abstract boolean isReturnTypeCorrect(Method method);

    /**
     * Returns {@code true} if the method parameters are correct, {@code false} otherwise.
     */
    protected abstract boolean acceptsCorrectParams(Method method);
}
