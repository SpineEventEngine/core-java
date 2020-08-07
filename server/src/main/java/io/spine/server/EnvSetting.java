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

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.Internal;
import io.spine.base.EnvironmentType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A mutable value that may differ between {@linkplain EnvironmentType environment types}.
 *
 * <p>For example:
 * <pre>
 * {@literal EnvSetting<StorageFactory>} storageFactory ={@literal new EnvSetting<>();}
 * storageFactory.use(InMemoryStorageFactory.newInstance(), Production.class);
 *
 * assertThat(storageFactory.optionalValue(Production.class)).isPresent();
 * assertThat(storageFactory.optionalValue(Tests.class)).isEmpty();
 * </pre>
 *
 * <h1>Fallback</h1>
 * <p>{@code EnvSetting} allows to configure a default value for an environment type. It is used
 * when the value for the environment hasn't been {@linkplain #use(V, Class) set explicitly}.
 * <pre>
 *      // Assuming the environment is `Tests`.
 *
 *      StorageFactory fallbackStorageFactory = createStorageFactory();
 *     {@literal EnvSetting<StorageFactory>} setting =
 *          {@literal new EnvSetting<>(Tests.class, () -> fallbackStorageFactory)};
 *
 *     // `use` was never called, so the fallback value is calculated and returned.
 *     assertThat(setting.optionalValue()).isPresent();
 *     assertThat(setting.value()).isSameInstanceAs(fallbackStorageFactory);
 * </pre>
 *
 * <p>Fallback values are calculated once on first {@linkplain #value(Class) access} for the
 * specified environment. Every subsequent access returns the cached value.
 *
 * <pre>
 *      // This `Supplier` is calculated only once.
 *     {@literal Supplier<StorageFactory>} fallbackStorage = InMemoryStorageFactory::newInstance;
 *
 *     {@literal EnvSetting<StorageFactory>} setting =
 *     {@literal new EnvSetting<>(Tests.class, fallbackStorage);}
 *
 *     // `Supplier` is calculated and cached.
 *     StorageFactory storageFactory = setting.value();
 *
 *     // Fallback value is taken from cache.
 *     StorageFactory theSameFactory = setting.value();
 * </pre>
 *
 * <p>{@code EnvSetting} values do not determine the environment themselves: it's up to the
 * caller to ask for the appropriate one.
 *
 * <p>This implementation does <b>not</b> perform any synchronization, thus, if different threads
 * {@linkplain #use(Object, Class) configure} and {@linkplain #value(Class) read the value},
 * no effort is made to ensure any consistency.
 *
 * @param <V>
 *         the type of value
 */
@Internal
final class EnvSetting<V> {

    private final Map<Class<? extends EnvironmentType>, V> environmentValues =
            new HashMap<>();

    private final Map<Class<? extends EnvironmentType>, Supplier<V>> fallbacks =
            new HashMap<>();

    /**
     * Creates a new instance without any fallback configuration.
     */
    EnvSetting() {
    }

    /**
     * Creates a new instance, configuring {@code fallback} to supply a default value.
     *
     * <p>If a value for {@code type} is not {@linkplain #use(Object, Class) set explicitly},
     * {@link #value(Class)} and {@link #optionalValue(Class)} return the {@code fallback} result.
     */
    EnvSetting(Class<? extends EnvironmentType> type, Supplier<V> fallback) {
        this.fallbacks.put(type, fallback);
    }

    /**
     * If the value for the specified environment has been configured, returns it. Returns an
     * empty {@code Optional} otherwise.
     */
    Optional<V> optionalValue(Class<? extends EnvironmentType> type) {
        Optional<V> result = valueFor(type);
        return result;
    }

    /**
     * If the value for the specified environment has been configured, runs the specified operation
     * against it. Does nothing otherwise.
     *
     * <p>If you wish to run an operation that doesn't throw, use {@code
     * optionalValue(type).ifPresent(operation)}.
     *
     * @param operation
     *         operation to run
     */
    void ifPresentForEnvironment(Class<? extends EnvironmentType> type,
                                 SettingOperation<V> operation) throws Exception {
        Optional<V> value = valueFor(type);
        if (value.isPresent()) {
            operation.accept(value.get());
        }
    }

    /**
     * If the value corresponding to the specified environment type is set, returns it.
     *
     * <p>If it is not set, returns a fallback value. If no fallback was configured, an
     * {@code IllegalStateException} is thrown.
     */
    V value(Class<? extends EnvironmentType> type) {
        checkNotNull(type);
        Optional<V> result = valueFor(type);
        return result.orElseThrow(
                () -> newIllegalStateException("Env setting for environment `%s` is unset.",
                                               type));
    }

    /**
     * Clears this setting, forgetting all of the configured values.
     *
     * <p>Cached default values are also cleared and will be recalculated using the {@code
     * Supplier} passed to the {@linkplain #EnvSetting(Class, Supplier) constructor}.
     */
    @VisibleForTesting
    void reset() {
        environmentValues.clear();
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

    private Optional<V> valueFor(Class<? extends EnvironmentType> type) {
        checkNotNull(type);
        V result = this.environmentValues.get(type);
        if (result == null) {
            Supplier<V> resultSupplier = this.fallbacks.get(type);
            if (resultSupplier == null) {
                return Optional.empty();
            }
            V newValue = resultSupplier.get();
            checkNotNull(newValue);
            this.use(newValue, type);
            return Optional.of(newValue);
        }
        return Optional.of(result);
    }

    /**
     * Represents an operation over the setting that returns no result and may finish with an error.
     *
     * @param <V>
     *         the type of setting to perform the operation over
     */
    interface SettingOperation<V> {

        /** Performs this operation on the specified value. */
        void accept(V value) throws Exception;
    }
}
