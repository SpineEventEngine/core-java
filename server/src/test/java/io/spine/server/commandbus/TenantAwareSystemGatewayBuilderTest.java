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
import io.spine.core.TenantId;
import io.spine.system.server.NoOpSystemGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("TenantAwareSystemGateway.Builder should")
class TenantAwareSystemGatewayBuilderTest {

    @Test
    @DisplayName("not allow null arguments")
    void builderNonNull() {
        new NullPointerTester()
                .testInstanceMethods(TenantAwareSystemGateway.create(), PACKAGE);
    }

    @Test
    @DisplayName("not build without delegate")
    void expectDelegate() {
        TenantAwareSystemGateway.Builder builder = TenantAwareSystemGateway
                .create()
                .withTenant(TenantId.getDefaultInstance());
        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    @DisplayName("not build without tenant ID")
    void expectTenant() {
        TenantAwareSystemGateway.Builder builder = TenantAwareSystemGateway
                .create()
                .atopOf(NoOpSystemGateway.INSTANCE);
        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    @DisplayName("build successfully with all arguments")
    void createSuccessfully() {
        NoOpSystemGateway delegate = NoOpSystemGateway.INSTANCE;
        TenantId tenant = TenantId.getDefaultInstance();
        TenantAwareSystemGateway result = TenantAwareSystemGateway
                .create()
                .atopOf(delegate)
                .withTenant(tenant)
                .build();
        assertNotNull(result);

        assertSame(delegate, result.getDelegate());
        assertSame(tenant, result.getTenantId());
    }
}
