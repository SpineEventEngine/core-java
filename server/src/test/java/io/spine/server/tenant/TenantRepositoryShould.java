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

package io.spine.server.tenant;

import com.google.protobuf.Timestamp;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.tenant.TenantIndex.TenantIdConsumer;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static io.spine.core.given.GivenTenantId.newUuid;
import static java.util.Collections.synchronizedList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Yevsyukov
 */
public class TenantRepositoryShould {

    private static final TenantId[] EMPTY_ARRAY = {};

    private TenantRepository<?, ?> repository;

    @Before
    public void setUp() {
        final BoundedContext bc = BoundedContext.newBuilder().build();
        TenantRepository<?, ?> impl = new TenantRepositoryImpl();
        impl.initStorage(bc.getStorageFactory());
        repository = spy(impl);
    }

    @Test
    public void cache_passed_value() {
        final TenantId tenantId = newUuid();

        repository.keep(tenantId);
        repository.keep(tenantId);

        verify(repository, times(1)).find(tenantId);
    }

    @Test
    public void un_cache_values() {
        final TenantId tenantId = newUuid();

        repository.keep(tenantId);
        assertTrue(repository.unCache(tenantId));
        assertFalse(repository.unCache(tenantId));
    }

    @Test
    public void clear_cache() {
        final TenantId tenantId = newUuid();

        repository.keep(tenantId);

        repository.clearCache();

        assertFalse(repository.unCache(tenantId));
    }

    @Test
    public void apply_operation_for_each_tenant() {
        final MemoizingConsumer operation = new MemoizingConsumer();
        final Set<TenantId> current = repository.getAll();
        assertTrue(current.isEmpty());

        final TenantId first = newUuid();
        final TenantId second = newUuid();
        repository.keep(first);
        repository.forEachTenant(operation);

        assertEquals(1, operation.accepted.size());
        assertThat(operation.accepted, contains(first));

        repository.keep(second);
        assertEquals(2, operation.accepted.size());
        assertThat(operation.accepted, contains(first, second));
    }

    @Test
    public void apply_operation_for_each_tenant_concurrently() throws InterruptedException {
        final int parallelism = 5;
        final Collection<TenantId> tenants = generate(parallelism);
        final MemoizingConsumer operation = new MemoizingConsumer();
        repository.forEachTenant(operation);
        final ExecutorService executor = newFixedThreadPool(parallelism);
        for (final TenantId tenant : tenants) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    repository.keep(tenant);
                }
            });
        }
        executor.awaitTermination(2, SECONDS);
        final TenantId[] expected = tenants.toArray(EMPTY_ARRAY);
        assertThat(operation.accepted, containsInAnyOrder(expected));
    }

    private static Collection<TenantId> generate(int count) {
        final Collection<TenantId> tenants = newArrayListWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
            tenants.add(newUuid());
        }
        return tenants;
    }

    private static class MemoizingConsumer implements TenantIdConsumer {

        private final List<TenantId> accepted = synchronizedList(new LinkedList<TenantId>());

        @Override
        public void accept(TenantId tenantId) {
            assertNotNull(tenantId);
            accepted.add(tenantId);
        }
    }

    private static class TenantRepositoryImpl
            extends TenantRepository<Timestamp, DefaultTenantRepository.Entity> {
    }
}
