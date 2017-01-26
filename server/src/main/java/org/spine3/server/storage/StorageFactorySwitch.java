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
 *
 * @author Alexander Yevsyukov
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

    public static StorageFactorySwitch instance() {
        return Singleton.INSTANCE.value;
    }

    public void init(Supplier<StorageFactory> productionSupplier,
                     @Nullable Supplier<StorageFactory> testsSupplier) {
        this.productionSupplier = checkNotNull(productionSupplier);
        this.testsSupplier = testsSupplier;
    }

    @Override
    public StorageFactory get() {
        if (storageFactory == null) {

            if (Environment.getInstance().isTests()) {
                if (testsSupplier != null) {
                    storageFactory = testsSupplier.get();
                } else {
                    storageFactory = InMemoryStorageFactory.getInstance();
                }
            } else {
                if (productionSupplier == null) {
                    throw new IllegalStateException(
                            "A supplier of a production StorageFactory is not set " +
                                    "but the code runs in the production mode. " +
                            "Please call StorageFactorySwitch.init().");
                }

                storageFactory = productionSupplier.get();
            }
        }

        return storageFactory;
    }

    private enum Singleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final StorageFactorySwitch value = new StorageFactorySwitch();
    }


}
