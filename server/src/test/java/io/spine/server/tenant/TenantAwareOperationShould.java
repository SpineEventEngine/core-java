/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import io.spine.test.Tests;
import org.junit.Test;

import static io.spine.core.given.GivenTenantId.newUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
// OK for the tests. We set right before we get().
public class TenantAwareOperationShould {

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_tenant() {
        createOperation(Tests.<TenantId>nullRef());
    }

    @Test
    public void substitute_single_tenant_for_default_value() {
        final TenantAwareOperation op = createOperation(TenantId.getDefaultInstance());
        assertEquals(CurrentTenant.singleTenant(), op.tenantId());
    }

    @Test
    public void remember_and_restore_current_tenant() {
        final TenantId previousTenant = newUuid();
        CurrentTenant.set(previousTenant);

        final TenantId newTenant = newUuid();
        final TenantAwareOperation op = createOperation(newTenant);

        // Check that the construction of the operation does not change the current tenant.
        assertEquals(previousTenant, CurrentTenant.get()
                                                  .get());

        op.execute();

        // Check that we got right value inside run().
        assertEquals(newTenant, getTenantFromRun(op));

        // Check that the current tenant is restored.
        assertEquals(previousTenant, CurrentTenant.get()
                                                  .get());
    }

    @Test
    public void clear_current_tenant_on_restore_if_no_tenant_was_set() {
        // Make sure there's not current tenant.
        CurrentTenant.clear();

        // Create new operation.
        final TenantId newTenant = newUuid();
        final TenantAwareOperation op = createOperation(newTenant);

        // Check that the construction did not set the tenant.
        assertFalse(CurrentTenant.get()
                                 .isPresent());

        op.execute();

        // Check that the execution was on the correct tenant.
        assertEquals(newTenant, getTenantFromRun(op));

        // Check that the execution cleared the current tenant.
        assertFalse(CurrentTenant.get()
                                 .isPresent());
    }

    @Test
    public void create_instance_for_non_command_execution_context() {
        final TenantId tenant = newUuid();
        CurrentTenant.set(tenant);

        final TenantAwareOperation op = createOperation();

        assertEquals(tenant, op.tenantId());

        op.execute();

        assertEquals(tenant, getTenantFromRun(op));
    }

    @Test(expected = IllegalStateException.class)
    public void do_not_allow_creating_instance_in_non_command_execution_without_current_tenant() {
        CurrentTenant.clear();

        // This should fail.
        createOperation();
    }

    /*
     * Test environment utilities.
     *********************************/

    private static TestOp createOperation(TenantId newTenant) {
        return new TestOp(newTenant);
    }

    private static TestOp createOperation() {
        return new TestOp();
    }

    private static TenantId getTenantFromRun(TenantAwareOperation op) {
        return ((TestOp)op).tenantInRun;
    }

    /**
     * The tenant data operation which remembers the current tenant in {@link #run()}.
     */
    private static class TestOp extends TenantAwareOperation {

        private TenantId tenantInRun;

        private TestOp(TenantId tenantId) {
            super(tenantId);
        }

        private TestOp() {
            super();
        }

        @Override
        public void run() {
            tenantInRun = CurrentTenant.get().get();
        }
    }
}
