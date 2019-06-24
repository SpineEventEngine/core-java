/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.base.Environment;
import io.spine.server.ContextSpec;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The {@code Supplier} of {@code StorageFactory} that takes into account if the code runs under
 * tests on in the production mode.
 *
 * <p><b>Test mode.</b>
 * Under tests this class returns {@link InMemoryStorageFactory} if
 * a {@code Supplier} for tests was not set via {@link #init}.
 *
 * <p><b>Production mode.</b>
 * In the production mode this class obtains the instance provided by the {@code Supplier} passed
 * via {@link #init}. If the production {@code Supplier} was not initialized,
 * {@code IllegalStateException} will be thrown.
 *
 * <p><b>Remembering {@code StorageFactory} obtained from suppliers.</b>
 * In both modes the reference to the {@code StorageFactory} obtained from a {@code Supplier}
 * will be stored. This means that a call to {@link Supplier#get()} method of
 * the suppliers passed via {@link #init} will be made only once.
 *
 * @see Environment#isTests()
 */
public final class StorageFactorySwitch implements Function<ContextSpec, StorageFactory> {

    private @Nullable Function<ContextSpec, StorageFactory> productionSupplier;

    private @Nullable Function<ContextSpec, StorageFactory> testsSupplier;

    /** Cached instance of the StorageFactory supplied by one of the suppliers. */
    private @Nullable StorageFactory storageFactory;

    /**
     * Initializes the current singleton instance with the suppliers.
     *
     * @param productionSupplier
     *         the supplier for the production mode
     * @param testsSupplier
     *         the supplier for the tests mode.
     *         If {@code null} is passed {@link InMemoryStorageFactory} will be used
     * @return this
     */
    @CanIgnoreReturnValue
    public StorageFactorySwitch init(Function<ContextSpec, StorageFactory> productionSupplier,
                           @Nullable Function<ContextSpec, StorageFactory> testsSupplier) {
        this.productionSupplier = checkNotNull(productionSupplier);
        this.testsSupplier = testsSupplier;
        return this;
    }

    /**
     * Clears the internal state. Required for tests.
     */
    @VisibleForTesting
    void reset() {
        storageFactory = null;
        productionSupplier = null;
        testsSupplier = null;
    }

    /**
     * Obtains production supplier. Required for tests.
     */
    @VisibleForTesting
    Optional<Function<ContextSpec, StorageFactory>> productionSupplier() {
        return Optional.ofNullable(productionSupplier);
    }

    /**
     * Obtains tests supplier. Required for tests.
     */
    @VisibleForTesting
    Optional<Function<ContextSpec, StorageFactory>> testsSupplier() {
        return Optional.ofNullable(testsSupplier);
    }

    /**
     * Obtains {@code StorageFactory} for the current execution mode.
     *
     * @return {@code StorageFactory} instance
     * @throws IllegalStateException if production {@code Supplier} was not set via {@link #init}
     */
    @Override
    public StorageFactory apply(ContextSpec spec) {
        if (storageFactory != null) {
            return storageFactory;
        }

        boolean inTests = Environment.getInstance()
                                     .isTests();
        if (inTests) {
            storageFactory = testsSupplier != null
                             ? testsSupplier.apply(spec)
                             : InMemoryStorageFactory.newInstance(spec);
        } else {
            if (productionSupplier == null) {
                throw newIllegalStateException(
                        "A supplier of a production StorageFactory is not set " +
                                "but the code runs in the production mode. " +
                                "Please call %s.init().", getClass().getSimpleName());
            }
            storageFactory = productionSupplier.apply(spec);
        }
        return storageFactory;
    }
}
