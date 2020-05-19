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

import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mutable value that may differ for the production and testing environments.
 *
 * <p>Allows configuring a wrapping function to adjust mutations as such:
 * <pre>
 * {@code
 * EnvironmentDependantValue<StorageFactory> storages = EnvironmentDependantValue
 *                          .<StorageFactory>newBuilder()
 *                          .wrappingProduction(SystemAwareStorageFactory::wrap)
 *                          .build();
 * storages.production(InMemoryStorageFactory.newInstance());
 *
 * // As it was wrapped.
 * assertThat(storages.production()).isInstanceOf(SystemAwareStorageFactory.class);
 * }
 * </pre>
 * <p>If no wrapping function is specified, the value is never wrapped.
 *
 * <p>{@code EnvironmentDependantValues} do not determine the environment themselves: it's up to the
 * caller to ask for the appropriate one.
 *
 * @param <P>
 *         the type of value
 */
public class EnvironmentDependantValue<P> {

    private @Nullable P productionValue;
    private final UnaryOperator<P> productionWrappingFunction;

    private @Nullable P testsValue;
    private final UnaryOperator<P> testsWrappingFunction;

    private EnvironmentDependantValue(Builder<P> builder) {
        this.productionWrappingFunction = builder.productionWrappingFunction;
        this.testsWrappingFunction = builder.testsWrappingFunction;
    }

    /** Returns the value for the production environment. */
    @Nullable P production() {
        return productionValue;
    }

    /** Returns the value for the testing environment. */
    @Nullable P tests() {
        return testsValue;
    }

    /** Changes the production and the testing values to {@code null}. */
    void nullifyBoth() {
        this.productionValue = null;
        this.testsValue = null;
    }

    /**
     * Changes the production value to the specified one, such that later calls to
     * {@link EnvironmentDependantValue#production()} return either {@code value} if no wrapping
     * function was specified, or {@code wrappingFunction.apply(value)} if wrapping function was
     * specified.
     *
     * @param value
     *         a new value for the production environment
     */
    public void production(P value) {
        checkNotNull(value);
        this.productionValue = productionWrappingFunction.apply(value);
    }

    /**
     * Changes the test environment  value to the specified one, such that later calls to
     * {@link EnvironmentDependantValue#tests()} ()} return either {@code value} if no wrapping
     * function was specified, or {@code wrappingFunction.apply(value)} if wrapping function was
     * specified.
     *
     * @param value
     *         a new value for the testing environment
     */
    public void tests(P value) {
        checkNotNull(value);
        this.testsValue = testsWrappingFunction.apply(value);
    }

    /**
     * Returns a new builder for the environment dependant values.
     *
     * @param <P>
     *         the type of the value
     * @return a new builder
     */
    public static <P> Builder<P> newBuilder() {
        return new Builder<>();
    }

    /**
     * A builder of environment dependant values.
     *
     * <p>Allows to configure the wrapping functions.
     *
     * <p>If no wrapping functions were specified, the instance from this builder
     * returns whatever was specified through the mutation method, e.g.
     * <pre>
     * {@code
     * EnvironmentDependantValue<?> config = EnvironmentDependantValue
     *                      .<?>newBuilder()
     *                      .build();
     *
     * config.production(productionValue);
     * assertThat(config.production()).isEqualTo(productionValue);
     * }
     * </pre>
     *
     * <p>On the other hand, if the wrapping function was specified, then the behaviour is as
     * follows:
     *
     * <pre>
     * {@code
     * EnvironmentDependantValue<?> config = EnvironmentDependantValue
     *                      .<?>newBuilder()
     *                      .wrappingProduction(someFunction)
     *                      .build();
     *
     * config.production(productionValue);
     * assertThat(config.production()).isEqualTo(someFunction.apply(productionValue));
     * }
     * </pre>
     *
     * <p>Same goes for the values for the testing environment.
     *
     * @param <P>
     *         the type of the value
     */
    public static class Builder<P> {

        private UnaryOperator<P> productionWrappingFunction;
        private UnaryOperator<P> testsWrappingFunction;

        /**
         * Configures the wrapping function for the production environment.
         *
         * <p>If no function is specified, {@link EnvironmentDependantValue#tests()}
         * returns whatever was specified to the {@link EnvironmentDependantValue#tests(Object)}.
         *
         * @param fn
         *         a wrapping function
         */
        public Builder<P> wrappingProduction(UnaryOperator<P> fn) {
            checkNotNull(fn);
            this.productionWrappingFunction = fn;
            return this;
        }

        /**
         * Configures the wrapping function for the testing environment.
         *
         * If no function is specified, {@link EnvironmentDependantValue#production()} returns
         * whatever was specified to the {@link EnvironmentDependantValue#production(Object)}.
         *
         * @param fn
         *         a wrapping function
         */
        public Builder<P> wrappingTests(UnaryOperator<P> fn) {
            checkNotNull(fn);
            this.testsWrappingFunction = fn;
            return this;
        }

        /**
         * Returns a new instance of the {@code EnvironmentDependantValue}.
         *
         * @return a new instance of {@code EnvironmentDependantValue}
         */
        EnvironmentDependantValue<P> build() {
            this.productionWrappingFunction = this.productionWrappingFunction == null
                                              ? UnaryOperator.identity()
                                              : this.productionWrappingFunction;
            this.testsWrappingFunction = this.testsWrappingFunction == null
                                         ? UnaryOperator.identity()
                                         : this.testsWrappingFunction;
            return new EnvironmentDependantValue<>(this);
        }
    }
}
