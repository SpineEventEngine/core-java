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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mutable value that may differ for the value and testing environments.
 *
 * <p>For example:
 * <pre>
 * {@code
 * EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
 * storageFactory.configure(InMemoryStorageFactory.newInstance()).forProduction();
 *
 * assertThat(storageFactory.value()).isPresent();
 * assertThat(storageFactory.tests()).isEmpty();
 * }
 * </pre>
 *
 * <p>{@code EnvSetting} values do not determine the environment themselves: it's up to the
 * caller to ask for the appropriate one.
 *
 * @param <P>
 *         the type of value
 */
public final class EnvSetting<P> {

    private final Map<EnvironmentType, P> settingValue = new HashMap<>();

    /**
     * Returns the value for the value environment if it was set, an empty {@code Optional}
     * otherwise.
     */
    Optional<P> value(EnvironmentType type) {
        return Optional.ofNullable(settingValue.get(type));
    }

    /**
     * Runs the specified operations against the value if it's present, otherwise does nothing.
     *
     * <p>If you wish to run an operation that doesn't throw, use {@code value().ifPresent()}.
     *
     * @param operation
     *         operation to run
     */
    void ifPresentForEnvironment(EnvironmentType type, ThrowingConsumer<P> operation)
            throws Exception {
        P settingValue = this.settingValue.get(type);
        if (settingValue != null) {
            operation.accept(settingValue);
        }
    }

    /**
     * Returns the value for the value environment if it's present.
     *
     * <p>If it's not present, assigns the specified default value and returns it.
     */
    P assignOrDefault(P defaultValue, EnvironmentType type) {
        return settingValue.putIfAbsent(type, defaultValue);
    }

    /** Changes the value and the testing values to {@code null}. */
    void reset() {
        this.settingValue.clear();
    }

    /**
     * Allows to configure the specified value for testing or value as follows:
     *
     * {@code value.configure(testingValue).forTests();}
     *
     * @param value
     *         value to assign to one of environments
     */
    public void configure(P value, EnvironmentType type) {
        checkNotNull(value);
        this.settingValue.put(type, value);
    }

    public interface EnvironmentType {

    }

    public enum EnvType implements EnvironmentType {
        PRODUCTION, TESTS
    }

    /**
     * Represents an operation over a value that returns no result and may finish with an error.
     *
     * @param <V>
     *         the type of the input to the operation
     */
    public interface ThrowingConsumer<V> {

        /** Performs this operation on the specified value. */
        void accept(V value) throws Exception;
    }
}
