/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.server;

import com.google.common.annotations.VisibleForTesting;
import io.spine.environment.Environment;
import io.spine.environment.EnvironmentType;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
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
 * when the value for the environment hasn't been {@linkplain #use(Object, Class) set explicitly}.
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
 * @param <V>
 *         the type of value
 */
public final class EnvSetting<V> {

    private final Map<Class<? extends EnvironmentType>, Value<V>> environmentValues =
            new HashMap<>();

    private final Map<Class<? extends EnvironmentType>, Supplier<V>> fallbacks =
            new HashMap<>();

    private final ReadWriteLock locker = new ReentrantReadWriteLock();

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
        lockWriteOperation(() -> {
            this.fallbacks.put(type, fallback);
        });
    }

    /**
     * If the value for the specified environment has been configured, returns it. Returns an
     * empty {@code Optional} otherwise.
     */
    Optional<V> optionalValue(Class<? extends EnvironmentType> type) {
        return valueFor(type);
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
     * Applies the passed operation to this setting regardless of current environment.
     *
     * <p>This means the operation is applied to all passed setting {@linkplain #environmentValues
     * values} on a per-environment basis.
     *
     * @apiNote The not yet run {@linkplain #fallbacks fallback suppliers} are ignored to avoid an
     *        unnecessary value instantiation.
     */
    void apply(SettingOperation<V> operation) throws Exception {
        lockWriteOperation(() -> {
            for (Value<V> v : environmentValues.values()) {
                if (v.isResolved()) {
                    V value = v.get();
                    try {
                        operation.accept(value);
                    } catch (Exception e) {
                        throw illegalStateWithCauseOf(e);
                    }
                }
            }
        });
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
     * Returns the value corresponding to the current environment type.
     *
     * <p>If for the current environment, there is no value set in this setting
     * return a fallback value. If no fallback was configured,
     * an {@code IllegalStateException} is thrown.
     */
    V value() {
        Environment environment = Environment.instance();
        return value(environment.type());
    }

    /**
     * Clears this setting, forgetting all of the configured values.
     *
     * <p>Cached default values are also cleared and will be recalculated using the {@code
     * Supplier} passed to the {@linkplain #EnvSetting(Class, Supplier) constructor}.
     */
    @VisibleForTesting
    void reset() {
        lockWriteOperation(environmentValues::clear);
    }

    /**
     * Sets the specified value for the specified environment type.
     *
     * @param value
     *         value to assign to one of environments
     * @param type
     *         the type of the environment
     */
    void use(V value, Class<? extends EnvironmentType> type) {
        lockWriteOperation(() -> {
            checkNotNull(value);
            checkNotNull(type);
            this.environmentValues.put(type, new Value<>(value));
        });
    }

    /**
     * Sets the value lazily provided via the passed {@code Supplier}
     * for the specified environment type.
     *
     * <p>The supplier will not be invoked unless someone requests the value under
     * the matching environment.
     *
     * @param value
     *         supplier of the value to assign to one of environments
     * @param type
     *         the type of the environment
     */
    void lazyUse(Supplier<V> value, Class<? extends EnvironmentType> type) {
        lockWriteOperation(() -> {
            checkNotNull(value);
            checkNotNull(type);
            this.environmentValues.put(type, new Value<>(value));
        });
    }

    private Optional<V> valueFor(Class<? extends EnvironmentType> type) {
        checkNotNull(type);
        Value<V> value = lockReadOperation(() -> this.environmentValues.get(type));
        if (value == null) {
            Supplier<V> resultSupplier = lockReadOperation(() -> this.fallbacks.get(type));
            if (resultSupplier == null) {
                return Optional.empty();
            }
            V newValue = resultSupplier.get();
            checkNotNull(newValue);
            this.use(newValue, type);
            return Optional.of(newValue);
        }
        V result = value.get();
        return Optional.of(result);
    }

    /**
     * Executes the provided operation with a write lock.
     *
     * <p>This ensures that the operation is executed with exclusive access, preventing
     * other threads from performing read or write operations until the lock is released.
     *
     * @param operation
     *         the operation to execute
     */
    private void lockWriteOperation(Runnable operation) {
        locker.writeLock()
              .lock();
        try {
            operation.run();
        } finally {
            locker.writeLock()
                  .unlock();
        }
    }

    /**
     * Executes the provided operation with a read lock and returns the result.
     *
     * <p>This ensures that the operation is executed with shared access, allowing multiple
     * threads to read concurrently, but preventing write operations until the lock is released.
     *
     * @param operation
     *         the operation to execute
     */
    private <T> T lockReadOperation(Supplier<T> operation) {
        locker.readLock()
              .lock();
        try {
            return operation.get();
        } finally {
            locker.readLock()
                  .unlock();
        }
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

    /**
     * The value configured for the setting.
     *
     * <p>Supports lazy initialization via the {@code Supplier}. In this case, once the value
     * is {@linkplain #get() requested}, the supplier is invoked. The returned value is remembered
     * for all future requests.
     *
     * @param <V>
     *         type of the value
     */
    private static class Value<V> {

        private final Supplier<V> supplier;
        private @MonotonicNonNull V value;

        /**
         * Creates a value with the lazily resolving supplier.
         *
         * <p>The supplier is only invoked upon {@linkplain #get() request}.
         */
        private Value(Supplier<V> supplier) {
            this.supplier = supplier;
        }

        /**
         * Creates a new instance with the actual value already resolved.
         */
        private Value(V resolved) {
            this.supplier = () -> resolved;
            this.value = resolved;
        }

        /**
         * Tells whether this instance already has the value provided by the supplier.
         */
        private synchronized boolean isResolved() {
            return value != null;
        }

        private synchronized V get() {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }
    }
}
