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

package io.spine.server.tenant;

import com.google.protobuf.Timestamp;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("TenantRepository should")
class TenantRepositoryTest {

    private TenantRepository<?, ?> repository;

    @BeforeEach
    void setUp() {
        BoundedContext bc = BoundedContextBuilder.assumingTests().build();
        TenantRepository<?, ?> impl = new TenantRepositoryImpl();
        impl.initStorage(bc.storageFactory());
        repository = spy(impl);
    }

    @Test
    @DisplayName("cache passed value")
    void cachePassedValue() {
        TenantId tenantId = newUuid();

        repository.keep(tenantId);
        repository.keep(tenantId);

        verify(repository, times(1)).find(tenantId);
    }

    @Test
    @DisplayName("evict from cache")
    void evictFromCache() {
        TenantId tenantId = newUuid();

        repository.keep(tenantId);
        assertTrue(repository.unCache(tenantId));
        assertFalse(repository.unCache(tenantId));
    }

    @Test
    @DisplayName("clear cache")
    void clearCache() {
        TenantId tenantId = newUuid();

        repository.keep(tenantId);

        repository.clearCache();

        assertFalse(repository.unCache(tenantId));
    }

    private static class TenantRepositoryImpl
            extends TenantRepository<Timestamp, DefaultTenantRepository.Entity> {
    }
}
