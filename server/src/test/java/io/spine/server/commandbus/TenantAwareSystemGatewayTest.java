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

package io.spine.server.commandbus;

import com.google.protobuf.Message;
import io.spine.base.Time;
import io.spine.core.TenantId;
import io.spine.system.server.MemoizingGateway;
import io.spine.system.server.SystemGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("TenantAwareSystemGateway should post system commands")
class TenantAwareSystemGatewayTest {

    @Test
    @DisplayName("in single-tenant env")
    void singleTenant() {
        MemoizingGateway delegate = MemoizingGateway.singleTenant();
        TenantId tenantId = TenantId.getDefaultInstance();
        postAndCheck(delegate, tenantId);
    }

    @Test
    @DisplayName("in multitenant env")
    void multitenant() {
        MemoizingGateway delegate = MemoizingGateway.multitenant();
        TenantId tenantId = TenantId
                .newBuilder()
                .setValue(TenantAwareSystemGatewayTest.class.getName())
                .build();
        postAndCheck(delegate, tenantId);
        assertEquals(tenantId, delegate.lastSeen()
                                       .tenant());
    }

    private static void postAndCheck(MemoizingGateway delegate, TenantId tenantId) {
        Message command = Time.getCurrentTime();
        SystemGateway gateway = TenantAwareSystemGateway.forTenant(tenantId, delegate);
        gateway.postCommand(command);

        assertEquals(command, delegate.lastSeen().command());
    }
}
