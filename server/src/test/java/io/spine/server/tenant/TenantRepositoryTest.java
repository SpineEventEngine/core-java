/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.core.TenantId;
import io.spine.server.BoundedContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth8.assertThat;
import static io.spine.testing.core.given.GivenTenantId.generate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TenantRepository should")
class TenantRepositoryTest {

    private TenantRepository<?, ?> repository;

    @BeforeEach
    void setUp() {
        repository = new TenantRepositoryImpl();
        BoundedContextBuilder
                .assumingTests(true)
                .setTenantIndex(repository)
                .build();
    }

    @Test
    @DisplayName("cache passed value")
    void cachePassedValue() {
        TenantId tenantId = generate();

        repository.keep(tenantId);

        Optional<?> optional = repository.find(tenantId);

        assertThat(optional).isPresent();

        assertTrue(repository.cached(tenantId));
    }

    @Test
    @DisplayName("evict from cache")
    void evictFromCache() {
        TenantId tenantId = generate();

        repository.keep(tenantId);
        assertTrue(repository.unCache(tenantId));
        assertFalse(repository.unCache(tenantId));
    }

    @Test
    @DisplayName("clear cache")
    void clearCache() {
        TenantId tenantId = generate();

        repository.keep(tenantId);

        repository.clearCache();

        assertFalse(repository.unCache(tenantId));
    }

    private static class TenantRepositoryImpl
            extends TenantRepository<Tenant, DefaultTenantRepository.Entity> {
    }
}
