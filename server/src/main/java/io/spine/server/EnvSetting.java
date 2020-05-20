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
import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mutable value that may differ for the production and testing environments.
 *
 * <p>Allows configuring a wrapping function to adjust mutations as such:
 * <pre>
 * {@code
 * EnvSetting<StorageFactory> storageFactory = EnvSetting
 *                          .<StorageFactory>newBuilder()
 *                          .wrapProductionValue(SystemAwareStorageFactory::wrap)
 *                          .build();
 * storageFactory.configure(InMemoryStorageFactory.newInstance()).forProduction();
 *
 * // As it was set.
 * assertThat(storageFactory.production()).isPresent();
 *
 * // As it was wrapped.
 * assertThat(storageFactory.production().get()).isInstanceOf(SystemAwareStorageFactory.class);
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
public final class EnvSetting<P> {

    private @Nullable P productionValue;
    private final UnaryOperator<P> productionWrappingFunction;

    private @Nullable P testsValue;
    private final UnaryOperator<P> testsWrappingFunction;

    private EnvSetting(Builder<P> builder) {
        this.productionWrappingFunction = builder.wrapProduction;
        this.testsWrappingFunction = builder.wrapTests;
    }

    /**
     * Returns the value for the production environment if it was set, an empty {@code Optional}
     * otherwise.
     */
    Optional<P> production() {
        return Optional.ofNullable(productionValue);
    }

    /**
     * Returns the value for the testing environment if it was set, an empty {@code Optional}
     * otherwise.
     */
    Optional<P> tests() {
        return Optional.ofNullable(testsValue);
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
         * Changes the test environment value to the one specified by {@code configure},
         * such that later calls to {@link EnvSetting#tests()} return
         * either {@code value} if no wrapping function was specified, or
         * {@code wrappingFunction.apply(value)} if wrapping function was specified.
         */
        public void forTests() {
            testsValue = testsWrappingFunction.apply(value);
        }

        /**
         * Changes the production value to the one specified by {@code configure},
         * such that later calls to {@link EnvSetting#production()} return
         * either {@code value} if no wrapping function was specified, or
         * {@code wrappingFunction.apply(value)} if wrapping function was specified.
         */
        public void forProduction() {
            productionValue = productionWrappingFunction.apply(value);
        }
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
     * EnvSetting<?> value = EnvSetting
     *                      .<?>newBuilder()
     *                      .build();
     *
     * value.configure(productionValue).forProduction();
     * assertThat(config.production().get()).isEqualTo(productionValue);
     * }
     * </pre>
     *
     * <p>On the other hand, if the wrapping function was specified, then the behaviour is as
     * follows:
     *
     * <pre>
     * {@code
     * EnvSetting<?> value = EnvSetting
     *                      .<?>newBuilder()
     *                      .wrapProductionValue(someFunction)
     *                      .build();
     *
     * value.configure(productionValue).forProduction();
     * assertThat(config.production().get()).isEqualTo(someFunction.apply(productionValue));
     * }
     * </pre>
     *
     * <p>Same goes for the values for the testing environment.
     *
     * @param <P>
     *         the type of the value
     */
    public static class Builder<P> {

        private UnaryOperator<P> wrapProduction;
        private UnaryOperator<P> wrapTests;

        /**
         * Configures the wrapping function for the production environment.
         *
         * <p>If no function is specified, {@link EnvSetting#tests()}
         * returns whatever was specified to the {@link EnvSetting#configure(Object)}.
         *
         * @param fn
         *         a wrapping function
         */
        public Builder<P> wrapProductionValue(UnaryOperator<P> fn) {
            checkNotNull(fn);
            this.wrapProduction = fn;
            return this;
        }

        /**
         * Configures the wrapping function for the testing environment.
         *
         * If no function is specified, {@link EnvSetting#production()} returns
         * whatever was specified to the {@link EnvSetting#configure(Object)}.
         *
         * @param fn
         *         a wrapping function
         */
        public Builder<P> wrapTestValue(UnaryOperator<P> fn) {
            checkNotNull(fn);
            this.wrapTests = fn;
            return this;
        }

        /**
         * Returns a new instance of the {@code EnvSetting}.
         *
         * @return a new instance of {@code EnvSetting}
         */
        EnvSetting<P> build() {
            this.wrapProduction = this.wrapProduction == null
                                  ? UnaryOperator.identity()
                                  : this.wrapProduction;
            this.wrapTests = this.wrapTests == null
                             ? UnaryOperator.identity()
                             : this.wrapTests;
            return new EnvSetting<>(this);
        }
    }
}
