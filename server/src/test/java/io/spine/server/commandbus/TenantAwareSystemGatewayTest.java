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
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.Query;
import io.spine.core.TenantId;
import io.spine.system.server.MemoizingGateway;
import io.spine.system.server.SystemGateway;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.system.server.GatewayFunction.delegatingTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("TenantAwareSystemGateway should")
class TenantAwareSystemGatewayTest {

    private static final String POST_COMMANDS = "post system commands";
    private static final String POST_EVENTS = "post system events";
    private static final String QUERY = "query system BC for domain aggregates";

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(TenantAwareSystemGatewayTest.class);

    @Nested
    @DisplayName("in single-tenant env")
    class SingleTenant {

        private final TenantId defaultTenant = TenantId.getDefaultInstance();

        private MemoizingGateway delegate;

        @BeforeEach
        void setUp() {
            delegate = MemoizingGateway.singleTenant();
        }

        @Test
        @DisplayName(POST_COMMANDS)
        void postCommands() {
            postCommandAndCheck(delegate, defaultTenant);
        }

        @Test
        @DisplayName(POST_EVENTS)
        void postEvents() {
            postEventAndCheck(delegate, defaultTenant);
        }

        @Test
        @DisplayName(QUERY)
        void query() {
            queryAndCheck(delegate, defaultTenant);
        }
    }

    @Nested
    @DisplayName("in multitenant env")
    class Multitenant {

        private TenantId tenantId;
        private MemoizingGateway delegate;

        @BeforeEach
        void setUp() {
            delegate = MemoizingGateway.multitenant();
            tenantId = TenantId
                    .newBuilder()
                    .setValue(TenantAwareSystemGatewayTest.class.getName())
                    .build();
        }

        @Test
        @DisplayName(POST_COMMANDS)
        void postCommands() {
            postCommandAndCheck(delegate, tenantId);
            assertEquals(tenantId, delegate.lastSeenCommand().tenant());
        }

        @Test
        @DisplayName(POST_EVENTS)
        void postEvents() {
            postEventAndCheck(delegate, tenantId);
            assertEquals(tenantId, delegate.lastSeenEvent().tenant());
        }

        @Test
        @DisplayName(QUERY)
        void query() {
            queryAndCheck(delegate, tenantId);
            assertEquals(tenantId, delegate.lastSeenQuery().tenant());
        }
    }

    private static void postCommandAndCheck(MemoizingGateway delegate, TenantId tenantId) {
        Message command = Time.getCurrentTime();
        SystemGateway gateway = delegatingTo(delegate).get(tenantId);
        gateway.postCommand(command);

        assertEquals(command, delegate.lastSeenCommand().message());
    }

    private static void postEventAndCheck(MemoizingGateway delegate, TenantId tenantId) {
        Message event = Time.getCurrentTime();
        SystemGateway gateway = delegatingTo(delegate).get(tenantId);
        gateway.postEvent(event);

        assertEquals(event, delegate.lastSeenEvent().message());
    }

    @SuppressWarnings("CheckReturnValue")
    private static void queryAndCheck(MemoizingGateway delegate, TenantId tenantId) {
        Query query = requestFactory.query().all(Timestamp.class);
        SystemGateway gateway = delegatingTo(delegate).get(tenantId);
        gateway.readDomainAggregate(query);

        assertEquals(query, delegate.lastSeenQuery().message());
    }
}
