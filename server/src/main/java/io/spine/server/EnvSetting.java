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

package io.spine.server;

import io.spine.annotation.Internal;
import io.spine.base.EnvironmentType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mutable value that may differ between {@linkplain EnvironmentType environment types}.
 *
 * <p>For example:
 * <pre>
 *
 * {@literal EnvSetting <StorageFactory>} storageFactory = new EnvSetting<>();
 * storageFactory.use(InMemoryStorageFactory.newInstance(), Production.class);
 *
 * assertThat(storageFactory.value(Production.class)).isPresent();
 * assertThat(storageFactory.value(Tests.class)).isEmpty();
 *
 * </pre>
 *
 * <p>{@code EnvSetting} values do not determine the environment themselves: it's up to the
 * caller to ask for the appropriate one.
 *
 * @param <V>
 *         the type of value
 */
@Internal
public final class EnvSetting<V> {

    private final Map<Class<? extends EnvironmentType>, V> environmentValues = new HashMap<>();

    /**
     * Returns the value for the specified environment type if it was set, an
     * empty {@code Optional} otherwise.
     */
    Optional<V> value(Class<? extends EnvironmentType> type) {
        return Optional.ofNullable(environmentValues.get(type));
    }

    /**
     * Runs the specified operations against the value corresponding to the specified environment
     * type if it's present, does nothing otherwise.
     *
     * <p>If you wish to run an operation that doesn't throw, use {@code
     * value(type).ifPresent(operation)}.
     *
     * @param operation
     *         operation to run
     */
    void ifPresentForEnvironment(Class<? extends EnvironmentType> type,
                                 ThrowingConsumer<V> operation) throws Exception {
        V settingValue = this.environmentValues.get(type);
        if (settingValue != null) {
            operation.accept(settingValue);
        }
    }

    /**
     * If the value corresponding to the specified environment type is set, just returns it.
     *
     * <p>If it is not set, runs the specified supplier, configures and returns the supplied value.
     */
    V assignOrDefault(Supplier<V> defaultValue, Class<? extends EnvironmentType> type) {
        checkNotNull(defaultValue);
        checkNotNull(type);
        if (environmentValues.containsKey(type)) {
            return environmentValues.get(type);
        } else {
            V value = defaultValue.get();
            this.use(value, type);
            return value;
        }
    }

    /**
     * Changes the value for all environments types, such that all of them return
     * {@code Optional.empty()} when {@linkplain #value(Class) acessing the value}.
     */
    void reset() {
        this.environmentValues.clear();
    }

    /**
     * Sets the specified value for the specified environment type.
     *
     * @param value
     *         value to assign to one of environments
     */
    void use(V value, Class<? extends EnvironmentType> type) {
        checkNotNull(value);
        checkNotNull(type);
        this.environmentValues.put(type, value);
    }

    /**
     * Represents an operation over a value that returns no result and may finish with an error.
     *
     * @param <V>
     *         the type of the input to the operation
     */
    interface ThrowingConsumer<V> {

        /** Performs this operation on the specified value. */
        void accept(V value) throws Exception;
    }
}
