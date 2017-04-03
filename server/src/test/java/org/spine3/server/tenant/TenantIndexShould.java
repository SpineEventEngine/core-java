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

import com.google.common.collect.ImmutableList;
import com.google.common.testing.NullPointerTester;
import org.junit.Test;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StorageFactorySwitch;
import org.spine3.users.TenantId;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsyukov
 */
public class TenantIndexShould {

    @Test
    public void provide_utility_factory() {
        assertHasPrivateParameterlessCtor(TenantIndex.Factory.class);
    }

    @Test
    public void pass_null_tolerance_test() {
        new NullPointerTester()
                .setDefault(StorageFactory.class, StorageFactorySwitch.getInstance(false)
                                                                      .get())
                .testAllPublicStaticMethods(TenantIndex.Factory.class);
    }

    @Test
    public void provide_tenant_index_for_single_tenant_context() {
        final TenantIndex index = TenantIndex.Factory.singleTenant();

        final List<TenantId> items = ImmutableList.copyOf(index.getAll());

        assertEquals(1, items.size());
        assertEquals(CurrentTenant.singleTenant(), items.get(0));
    }
}
