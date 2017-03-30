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

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.storage.StorageFactorySwitch;
import org.spine3.users.TenantId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.test.Tests.newTenantUuid;

/**
 * @author Alexander Yevsyukov
 */
public class TenantRepositoryShould {

    private TenantRepository<?, ?> repository;

    @Before
    public void setUp() {
        TenantRepository<?, ?> impl = new TenantRepositoryImpl();
        impl.initStorage(StorageFactorySwitch.getInstance(false)
                                             .get());
        repository = spy(impl);
    }

    @Test
    public void cache_passed_value() {
        final TenantId tenantId = newTenantUuid();

        repository.keep(tenantId);
        repository.keep(tenantId);

        verify(repository, times(1)).find(tenantId);
    }

    @Test
    public void un_cache_values() {
        final TenantId tenantId = newTenantUuid();

        repository.keep(tenantId);
        assertTrue(repository.unCache(tenantId));
        assertFalse(repository.unCache(tenantId));
    }

    @Test
    public void clear_cache() {
        final TenantId tenantId = newTenantUuid();

        repository.keep(tenantId);

        repository.clearCache();

        assertFalse(repository.unCache(tenantId));
    }


    private static class TenantRepositoryImpl
            extends TenantRepository<Timestamp, DefaultTenantRepository.Entity> {
        @Override
        protected Class<? extends Message> getEntityStateClass() {
            return Timestamp.class;
        }
    }
}
