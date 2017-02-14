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

package org.spine3.server.storage.memory;

import org.junit.Before;
import org.junit.Test;
import org.spine3.server.storage.TenantDataOperation;

import static org.junit.Assert.assertNotNull;
import static org.spine3.test.Tests.newTenantUuid;

/**
 * @author Alexander Yevsyukov
 */
public class MultitenantStorageShould {

    private MultitenantStorage storage;

    @Before
    public void setUp() {
        storage = new MultiStorage(true);
    }

    @Test(expected = IllegalStateException.class)
    public void report_missing_tenant_under_multitenant_context() {
        storage.getStorage();
    }

    @Test
    public void obtain_storage_for_current_tenant() {
        new TenantDataOperation(newTenantUuid()) {
            @Override
            public void run() {
                final TenantStorage tenantStorage = storage.getStorage();
                assertNotNull(tenantStorage);
            }
        };
    }

    private static class MultiStorage extends MultitenantStorage<TenantCommands> {

        MultiStorage(boolean multitenant) {
            super(multitenant);
        }

        @Override
        TenantCommands createSlice() {
            return new TenantCommands();
        }
    }
}
