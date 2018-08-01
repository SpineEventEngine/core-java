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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.base.Time;
import io.spine.core.TenantId;
import io.spine.system.server.MemoizingGateway;
import io.spine.system.server.NoOpSystemGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("TenantAwareSystemGateway should")
class TenantAwareSystemGatewayTest {

    @Test
    @DisplayName("not allow null arguments on construction")
    void builderNonNull() {
        new NullPointerTester()
                .testInstanceMethods(TenantAwareSystemGateway.create(), PACKAGE);
    }

    @Test
    @DisplayName("not be created without delegate")
    void expectDelegate() {
        TenantAwareSystemGateway.Builder builder = TenantAwareSystemGateway
                .create()
                .withTenant(TenantId.getDefaultInstance());
        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    @DisplayName("not be created without tenant ID")
    void expectTenant() {
        TenantAwareSystemGateway.Builder builder = TenantAwareSystemGateway
                .create()
                .atopOf(NoOpSystemGateway.INSTANCE);
        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    @DisplayName("be created successfully with all arguments")
    void createSuccessfully() {
        TenantAwareSystemGateway result = TenantAwareSystemGateway
                .create()
                .atopOf(NoOpSystemGateway.INSTANCE)
                .withTenant(TenantId.getDefaultInstance())
                .build();
        assertNotNull(result);
    }

    @Nested
    @DisplayName("post system commands")
    class PostCommand {

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
            assertEquals(tenantId, delegate.lastSeen().tenant());
        }

        private void postAndCheck(MemoizingGateway delegate, TenantId tenantId) {
            Message command = Time.getCurrentTime();
            TenantAwareSystemGateway gateway = TenantAwareSystemGateway
                    .create()
                    .withTenant(tenantId)
                    .atopOf(delegate)
                    .build();

            gateway.postCommand(command);

            assertEquals(command, delegate.lastSeen().command());
        }
    }
}
