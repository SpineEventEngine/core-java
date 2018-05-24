/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.base.Time;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.spine.core.given.GivenTenantId.newUuid;

/**
 * @author Alexander Yevsyukov
 */
public class TenantAwareFunction0Should {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void require_current_tenant_set() {
        final TenantAwareFunction0<Timestamp> whichTime = new TenantAwareFunction0<Timestamp>() {
            @Override
            public Timestamp apply() {
                return Time.getCurrentTime();
            }
        };

        // This should pass since we're executing the operation with the current tenant ID set.
        new TenantAwareOperation(newUuid()) {
            @SuppressWarnings("CheckReturnValue") // can ignore in this test.
            @Override
            public void run() {
                whichTime.execute();
            }
        }.execute();

        thrown.expect(IllegalStateException.class);
        // This should fail.
        whichTime.execute();
    }
}
