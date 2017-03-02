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

import org.junit.Test;
import org.spine3.test.Tests;
import org.spine3.users.TenantId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.spine3.test.Tests.newTenantUuid;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OptionalGetWithoutIsPresent") // OK for the tests. We set right before we get().
public class TenantDataOperationShould {

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_tenant() {
        createOperation(Tests.<TenantId>nullRef());
    }

    @Test
    public void substitute_single_tenant_for_default_value() {
        final TenantDataOperation op = createOperation(TenantId.getDefaultInstance());
        assertEquals(CurrentTenant.singleTenant(), op.tenantId());
    }

    @Test
    public void remember_and_restore_current_tenant() {
        final TenantId previousTenant = newTenantUuid();
        CurrentTenant.set(previousTenant);

        final TenantId newTenant = newTenantUuid();
        final TenantDataOperation op = createOperation(newTenant);

        // Check that the construction of the operation does not change the current tenant.
        assertEquals(previousTenant, CurrentTenant.get().get());

        op.execute();

        // Check that we got right value inside run().
        assertEquals(newTenant, getTenantFromRun(op));

        // Check that the current tenant is restored.
        assertEquals(previousTenant, CurrentTenant.get().get());
    }

    @Test
    public void clear_current_tenant_on_restore_if_no_tenant_was_set() {
        // Make sure there's not current tenant.
        CurrentTenant.clear();

        // Create new operation.
        final TenantId newTenant = newTenantUuid();
        final TenantDataOperation op = createOperation(newTenant);

        // Check that the construction did not set the tenant.
        assertFalse(CurrentTenant.get().isPresent());

        op.execute();

        // Check that the execution was on the correct tenant.
        assertEquals(newTenant, getTenantFromRun(op));

        // Check that the execution cleared the current tenant.
        assertFalse(CurrentTenant.get().isPresent());
    }

    private static TestOp createOperation(TenantId newTenant) {
        return new TestOp(newTenant);
    }

    private static TenantId getTenantFromRun(TenantDataOperation op) {
        return ((TestOp)op).tenantInRun;
    }

    /**
     * The tenant data operation which remembers the current tenant in {@link #run()}.
     */
    private static class TestOp extends TenantDataOperation {

        private TenantId tenantInRun;

        private TestOp(TenantId tenantId) {
            super(tenantId);
        }

        @Override
        public void run() {
            tenantInRun = CurrentTenant.get().get();
        }
    }
}
