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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mutable value that may differ for the production and testing environments.
 *
 * <p>For example:
 * <pre>
 * {@code
 * EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
 * storageFactory.configure(InMemoryStorageFactory.newInstance()).forProduction();
 *
 * assertThat(storageFactory.production()).isPresent();
 * assertThat(storageFactory.tests()).isEmpty();
 * }
 * </pre>
 *
 * <p>{@code EnvironmentDependantValues} do not determine the environment themselves: it's up to the
 * caller to ask for the appropriate one.
 *
 * @param <P>
 *         the type of value
 */
public final class EnvSetting<P> {

    private @Nullable P productionValue;

    private @Nullable P testsValue;

    /**
     * Returns the value for the production environment if it was set, an empty {@code Optional}
     * otherwise.
     */
    Optional<P> production() {
        return Optional.ofNullable(productionValue);
    }

    /**
     * Runs the specified operations against the production if it's present, otherwise does nothing.
     *
     * <p>If you wish to run an operation that doesn't throw, use {@code production().ifPresent()}.
     *
     * @param operation
     *         operation to run
     */
    void ifProductionPresent(ThrowingConsumer<P> operation) throws Exception {
        if (productionValue != null) {
            operation.accept(productionValue);
        }
    }

    /**
     * Returns the value for the production environment if it's present.
     *
     * <p>If it's not present, assigns the specified default value while following the wrapping
     * rules, and returns it.
     */
    P productionOrAssignDefault(P defaultValue) {
        if (productionValue == null) {
            configure(defaultValue).forProduction();
        }
        return productionValue;
    }

    /**
     * Returns the value for the testing environment if it was set, an empty {@code Optional}
     * otherwise.
     */
    Optional<P> tests() {
        return Optional.ofNullable(testsValue);
    }

    /**
     * Returns the value for the testing environment if it's present.
     *
     * <p>If it's not present, assigns the specified default value while following the wrapping
     * rules, and returns it.
     */
    P testsOrAssignDefault(P defaultValue) {
        if (testsValue == null) {
            configure(defaultValue).forTests();
        }
        return testsValue;
    }

    /** Changes the production and the testing values to {@code null}. */
    void reset() {
        this.productionValue = null;
        this.testsValue = null;
    }

    /**
     * Allows to configure the specified value for testing or production as follows:
     *
     * {@code value.configure(testingValue).forTests();}
     *
     * @param value
     *         value to assign to one of environments
     */
    public Configurator configure(P value) {
        checkNotNull(value);
        return new Configurator(value);
    }

    /**
     * A intermediate object that facilitates the following API:
     *
     * {@code value.configure(prodValue).forProduction();}.
     *
     * @implNote note the private constructor. {@code Configurator} objects are not meant to
     *         be instantiated by anyone other than the class that nests it.
     */
    public class Configurator {

        private final P value;

        private Configurator(P value) {
            this.value = value;
        }

        /**
         * Changes the test environment value to the one specified by the {@code configure} method.
         */
        public void forTests() {
            testsValue = value;
        }

        /**
         * Changes the production value to the one specified by {@code configure} method.
         */
        public void forProduction() {
            productionValue = value;
        }
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
