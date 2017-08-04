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

package io.spine.server.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import io.spine.Environment;
import io.spine.core.BoundedContextId;
import io.spine.server.storage.memory.InMemoryStorageFactory;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The {@code Supplier} of {@code StorageFactory} that takes into account if the code runs under
 * tests on in the production mode.
 *
 * <h2>Test mode</h2>
 * <p>Under tests  this class returns {@link InMemoryStorageFactory} if
 * a {@code Supplier} for tests was not set via {@link #init(Supplier, Supplier)}.
 *
 * <h2>Production mode</h2>
 * <p>In the production mode this class obtains the instance provided by the {@code Supplier} passed
 * via {@link #init(Supplier, Supplier)}. If the production {@code Supplier} was not initialized,
 * {@code IllegalStateException} will be thrown.
 *
 * <h2>Remembering {@code StorageFactory} obtained from suppliers</h2>
 * In both modes the reference to the {@code StorageFactory} obtained from a {@code Supplier}
 * will be stored. This means that a call to {@link Supplier#get()} method of
 * the suppliers passed via {@link #init(Supplier, Supplier)} will be made only once.
 *
 * @author Alexander Yevsyukov
 * @see Environment#isTests()
 */
public final class StorageFactorySwitch implements Supplier<StorageFactory> {

    /** The ID of the BoundedContext where this switch works. */
    private final BoundedContextId boundedContextId;

    @Nullable
    private Supplier<StorageFactory> productionSupplier;

    @Nullable
    private Supplier<StorageFactory> testsSupplier;

    /** Cached instance of the StorageFactory supplied by one of the suppliers. */
    @Nullable
    private StorageFactory storageFactory;

    private final boolean multitenant;

    private StorageFactorySwitch(BoundedContextId boundedContextId, boolean multitenant) {
        this.boundedContextId = boundedContextId;
        this.multitenant = multitenant;
    }

    /**
     * Obtains the instance of the switch that corresponds to multi-tenancy mode.
     *
     * @param boundedContextId the ID of the BoundedContext in which this switch works
     * @param multitenant if {@code true} the switch is requested for the multi-tenant execution
     *                    context, {@code false} for the single-tenant context
     */
    public static StorageFactorySwitch newInstance(BoundedContextId boundedContextId,
                                                   boolean multitenant) {
        return new StorageFactorySwitch(boundedContextId, multitenant);
    }

    /**
     * Initializes the current singleton instance with the suppliers.
     *
     *
     * @param productionSupplier the supplier for the production mode
     * @param testsSupplier the supplier for the tests mode.
     *                      If {@code null} is passed {@link InMemoryStorageFactory} will be used
     * @return this
     */
    public StorageFactorySwitch init(Supplier<StorageFactory> productionSupplier,
                                     @Nullable Supplier<StorageFactory> testsSupplier) {
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
    Optional<Supplier<StorageFactory>> productionSupplier() {
        return Optional.fromNullable(productionSupplier);
    }

    /**
     * Obtains tests supplier. Required for tests.
     */
    @VisibleForTesting
    Optional<Supplier<StorageFactory>> testsSupplier() {
        return Optional.fromNullable(testsSupplier);
    }

    /**
     * Obtains {@code StorageFactory} for the current execution mode.
     *
     * @return {@code StorageFactory} instance
     * @throws IllegalStateException if production {@code Supplier} was not set via
     *                               {@link #init(Supplier, Supplier)}
     */
    @Override
    public StorageFactory get() {
        if (storageFactory != null) {
            return storageFactory;
        }

        if (Environment.getInstance()
                       .isTests()) {
            storageFactory = testsSupplier != null
                             ? testsSupplier.get()
                             : InMemoryStorageFactory.newInstance(boundedContextId, multitenant);
        } else {
            if (productionSupplier == null) {
                throw newIllegalStateException(
                        "A supplier of a production StorageFactory is not set " +
                                "but the code runs in the production mode. " +
                        "Please call %s.init().", getClass().getSimpleName());
            }
            storageFactory = productionSupplier.get();
        }

        return storageFactory;
    }
}
