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

import com.google.protobuf.Timestamp;
import io.spine.client.Query;
import io.spine.core.TenantId;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.system.server.ReadSideFunction.delegatingTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("TenantAwareSystemReadSide should")
class TenantAwareSystemReadSideTest {

    private static final String QUERY = "query system BC for domain aggregates";

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(TenantAwareSystemReadSideTest.class);

    private MemoizingReadSide delegate;

    @Nested
    @DisplayName("in single-tenant env")
    class SingleTenant {

        private final TenantId defaultTenant = TenantId.getDefaultInstance();

        @BeforeEach
        void setUp() {
            delegate = MemoizingReadSide.singleTenant();
        }

        @Test
        @DisplayName(QUERY)
        void query() {
            queryAndCheck(defaultTenant);
        }
    }

    @Nested
    @DisplayName("in multitenant env")
    class Multitenant {

        private TenantId tenantId;

        @BeforeEach
        void setUp() {
            delegate = MemoizingReadSide.multitenant();
            tenantId = TenantId
                    .newBuilder()
                    .setValue(TenantAwareSystemReadSideTest.class.getName())
                    .build();
        }

        @Test
        @DisplayName(QUERY)
        void query() {
            queryAndCheck(tenantId);
            assertEquals(tenantId, delegate.lastSeenQuery().tenant());
        }
    }

    @SuppressWarnings("CheckReturnValue")
    private void queryAndCheck(TenantId tenantId) {
        Query query = requestFactory.query()
                                    .all(Timestamp.class);
        SystemReadSide readSide = delegatingTo(delegate).get(tenantId);
        readSide.readDomainAggregate(query);

        assertEquals(query, delegate.lastSeenQuery()
                                    .message());
    }
}
