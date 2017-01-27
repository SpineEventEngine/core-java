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

package org.spine3.server.storage;

import com.google.common.base.Supplier;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.util.Environment;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@code Supplier} of {@code StorageFactory} that takes into account
 * if the code runs under tests.
 *
 * <h2>Test mode</h2>
 * <p>Under tests  this class returns {@link InMemoryStorageFactory} if
 * a {@code Supplier} for tests was not set via {@link #init(Supplier, Supplier)}.
 *
 * <h2>Production mode</h2>
 * <p>In production mode this class obtains the instance provided by
 * the {@code Supplier} passed via {@link #init(Supplier, Supplier)}.
 * If the production {@code Supplier} was not initialized,
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

    @Nullable
    private StorageFactory storageFactory;

    @Nullable
    private Supplier<StorageFactory> productionSupplier;

    @Nullable
    private Supplier<StorageFactory> testsSupplier;

    private StorageFactorySwitch() {
        // Prevent construction of this singleton from outside code.
    }

    /**
     * Obtains the singleton instance.
     */
    public static StorageFactorySwitch instance() {
        return Singleton.INSTANCE.value;
    }

    /**
     * Initializes the switch with the suppliers of {@code StorageFactor}ies.
     *
     * @param productionSupplier the supplier for the production mode
     * @param testsSupplier the supplier for the tests mode.
     *                      If {@code null} is passed {@link InMemoryStorageFactory} will be used
     */
    public void init(Supplier<StorageFactory> productionSupplier,
                     @Nullable Supplier<StorageFactory> testsSupplier) {
        this.productionSupplier = checkNotNull(productionSupplier);
        this.testsSupplier = testsSupplier;
    }

    /**
     * Obtains {@code StorageFactory} for the current execution mode.
     *
     * @return {@code StorageFactory} instance
     * @throws IllegalStateException if production {@code Supplier} was not set via {@link #init(Supplier, Supplier)}
     */
    @Override
    public StorageFactory get() {
        if (storageFactory != null) {
            return storageFactory;
        }

        if (Environment.getInstance().isTests()) {
            storageFactory = testsSupplier != null
                             ? testsSupplier.get()
                             : InMemoryStorageFactory.getInstance();
        } else {
            if (productionSupplier == null) {
                throw new IllegalStateException(
                        "A supplier of a production StorageFactory is not set " +
                                "but the code runs in the production mode. " +
                        "Please call " + getClass().getSimpleName() + ".init().");
            }
            storageFactory = productionSupplier.get();
        }

        return storageFactory;
    }

    private enum Singleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final StorageFactorySwitch value = new StorageFactorySwitch();
    }
}
