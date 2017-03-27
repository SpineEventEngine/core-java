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

package org.spine3.server.tenant;

import com.google.common.collect.ImmutableSet;
import org.spine3.server.storage.StorageFactory;
import org.spine3.users.TenantId;

import java.util.Set;

/**
 * The index of tenant IDs in a multi-tenant application.
 *
 * @author Alexander Yevsyukov
 */
public interface TenantIndex extends AutoCloseable {

    /**
     * Stores the passed tenant ID in the index.
     */
    void keep(TenantId id);

    /**
     * Obtains the set of all stored tenant IDs.
     */
    Set<TenantId> getAll();

    class Factory {

        private static final TenantIndex SINGLE_TENANT_INDEX = new TenantIndex() {
            @Override
            public void keep(TenantId id) {
                // Do nothing.
            }

            @Override
            public Set<TenantId> getAll() {
                return ImmutableSet.of(CurrentTenant.singleTenant());
            }

            @Override
            public void close() throws Exception {
                // Do nothing;
            }
        };

        private Factory() {
            // Prevent instantiation of this utility class.
        }

        /**
         * Creates default implementation of {@code TenantIndex}.
         */
        public static TenantIndex createDefault(StorageFactory storageFactory) {
            final DefaultTenantRepository tenantRepo = new DefaultTenantRepository();
            tenantRepo.initStorage(storageFactory);
            return tenantRepo;
        }

        /**
         * Creates an {@code TenantIndex} to be used in single-tenant context.
         */
        public static TenantIndex singleTenant() {
            return SINGLE_TENANT_INDEX;
        }
    }
}
