/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.system.server;

import io.spine.base.EventMessage;
import io.spine.core.TenantId;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.system.server.WriteSideFunction.delegatingTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TenantAwareSystemWriteSide should")
class TenantAwareSystemWriteSideTest {

    private static final String POST_EVENTS = "post system events";

    private MemoizingWriteSide delegate;

    @Nested
    @DisplayName("in single-tenant env")
    class SingleTenant {

        private final TenantId defaultTenant = TenantId.getDefaultInstance();

        @BeforeEach
        void setUp() {
            delegate = MemoizingWriteSide.singleTenant();
        }

        @Test
        @DisplayName(POST_EVENTS)
        void postEvents() {
            postEventAndCheck(defaultTenant);
        }
    }

    @Nested
    @DisplayName("in multitenant env")
    class Multitenant {

        private TenantId tenantId;

        @BeforeEach
        void setUp() {
            delegate = MemoizingWriteSide.multitenant();
            tenantId = TenantId
                    .newBuilder()
                    .setValue(TenantAwareSystemWriteSideTest.class.getName())
                    .build();
        }

        @Test
        @DisplayName(POST_EVENTS)
        void postEvents() {
            postEventAndCheck(tenantId);
            assertEquals(tenantId, delegate.lastSeenEvent().tenant());
        }
    }

    private void postEventAndCheck(TenantId tenantId) {
        EventMessage event = Sample.messageOfType(ShoppingListCreated.class);
        SystemWriteSide writeSide = delegatingTo(delegate).get(tenantId);
        writeSide.postEvent(event);

        assertEquals(event, delegate.lastSeenEvent().message());
    }
}
