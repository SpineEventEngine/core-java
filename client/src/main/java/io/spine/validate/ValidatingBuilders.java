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
package io.spine.validate;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * Utility class for working with {@linkplain ValidatingBuilder validating builders}.
 *
 * @author Alex Tymchenko
 */
public class ValidatingBuilders {

    private ValidatingBuilders() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates an instance of {@code ValidatingBuilder} by its type.
     *
     * @param builderClass the type of the {@code ValidatingBuilder} to instantiate
     * @param <B>          the generic type of returned value
     * @return the new instance of the builder
     */
    @SuppressWarnings("OverlyBroadCatchBlock")   // OK, as the exception handling is the same.
    public static <B extends ValidatingBuilder<?, ?>> B
    newInstance(Class<B> builderClass) {
        checkNotNull(builderClass);

        try {
            final Method newBuilderMethod =
                    ValidatingBuilder.TypeInfo.getNewBuilderMethod(
                            builderClass);
            final Object raw = newBuilderMethod.invoke(null);

            // By convention, `newBuilder()` always returns instances of `B`.
            @SuppressWarnings("unchecked")
            final B builder = (B) raw;
            return builder;
        } catch (Exception e) {
            throw illegalStateWithCauseOf(e);
        }
    }
}
