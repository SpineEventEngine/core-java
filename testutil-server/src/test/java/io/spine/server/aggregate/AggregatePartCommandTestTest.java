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

package io.spine.server.aggregate;

import com.google.protobuf.Timestamp;
import io.spine.client.ActorRequestFactory;
import io.spine.core.TenantId;
import io.spine.server.aggregate.given.AggregatePartCommandTestTestEnv.TimerCounter;
import io.spine.server.aggregate.given.AggregatePartCommandTestTestEnv.TimerCountingTest;
import io.spine.server.command.CommandTest;
import io.spine.server.model.ModelTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.core.given.GivenUserId.newUuid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("AggregatePartCommandTest should")
class AggregatePartCommandTestTest {

    private AggregatePartCommandTest<Timestamp, TimerCounter> aggregatePartCommandTest;

    private static ActorRequestFactory newRequestFactory(Class<?> clazz) {
        final TenantId tenantId = TenantId.newBuilder()
                                          .setValue(clazz.getSimpleName())
                                          .build();
        return ActorRequestFactory.newBuilder()
                                  .setActor(newUuid())
                                  .setTenantId(tenantId)
                                  .build();
    }

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
        aggregatePartCommandTest = new TimerCountingTest();
    }

    @Test
    @DisplayName("create aggregate part in `setUp`")
    void createAggregatePartInSetUp() {
        assertFalse(aggregatePartCommandTest.aggregatePart()
                                            .isPresent());

        aggregatePartCommandTest.setUp();

        assertTrue(aggregatePartCommandTest.aggregatePart()
                                           .isPresent());
    }

    /**
     * Ensures existence of the constructor in {@link AggregatePartCommandTest} class.
     *
     * <p>We do this by simply invoking the constructor in the derived class.
     * We do not perform checks because they are done in the test suite that checks
     * {@link CommandTest} class.
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // Because we don't need the result.
    @Test
    @DisplayName("have constructor with ActorRequestFactory")
    void haveCtorWithRequestFactory() {
        new TimerCountingTest(newRequestFactory(getClass()));
    }
}
