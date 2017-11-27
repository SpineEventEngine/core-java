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

import com.google.common.testing.NullPointerTester;
import io.spine.core.TenantId;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.tenant.TenantIndex.TenantIdConsumer;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static io.spine.core.given.GivenTenantId.newUuid;
import static io.spine.server.BoundedContext.newName;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static java.util.Collections.synchronizedList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Alexander Yevsyukov
 */
public abstract class TenantIndexShould {

    private static final TenantId[] EMPTY_ARRAY = {};

    private TenantIndex index;

    protected final TenantIndex getIndex() {
        return index;
    }

    protected abstract TenantIndex createIndex();

    @Before
    public void beforeEach() {
        index = createIndex();
    }

    @Test
    public void provide_utility_factory() {
        assertHasPrivateParameterlessCtor(TenantIndex.Factory.class);
    }

    @Test
    public void pass_null_tolerance_test() {
        final InMemoryStorageFactory storageFactory =
                InMemoryStorageFactory.newInstance(newName(getClass().getSimpleName()), false);
        new NullPointerTester()
                .setDefault(StorageFactory.class, storageFactory)
                .testAllPublicStaticMethods(TenantIndex.Factory.class);
    }

    static Collection<TenantId> generate(int count) {
        final Collection<TenantId> tenants = newArrayListWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
            tenants.add(newUuid());
        }
        return tenants;
    }

    static class MemoizingConsumer implements TenantIdConsumer {

        private final List<TenantId> accepted = synchronizedList(new LinkedList<TenantId>());

        void assertContainsInAnyOrder(Collection<TenantId> expected) {
            assertThat(accepted, containsInAnyOrder(expected.toArray(EMPTY_ARRAY)));
        }

        void assertContains(Collection<TenantId> expected) {
            assertThat(accepted, contains(expected.toArray(EMPTY_ARRAY)));
        }

        @Override
        public void accept(TenantId tenantId) {
            assertNotNull(tenantId);
            accepted.add(tenantId);
        }
    }
}
