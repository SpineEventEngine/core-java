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

import com.google.common.truth.extensions.proto.ProtoTruth;
import io.spine.server.BoundedContext;
import io.spine.system.server.given.client.MealOrderProjection;
import io.spine.test.system.server.MealOrder;
import io.spine.test.system.server.OrderId;
import io.spine.test.system.server.OrderPlaced;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.system.server.given.client.SystemClientTestEnv.contextWithSystemProjection;
import static io.spine.system.server.given.client.SystemClientTestEnv.findProjection;

@DisplayName("Default implementation of SystemWriteSide should")
class DefaultSystemWriteSideTest {

    private SystemWriteSide systemWriteSide;
    private OrderId projectionId;

    @BeforeEach
    void setUp() {
        projectionId = OrderId.generate();
    }

    @Nested
    @DisplayName("post system")
    class PostMessages {

        private BoundedContext domainContext;
        private BoundedContext systemContext;

        @BeforeEach
        void setUp() {
            domainContext = contextWithSystemProjection();
            systemWriteSide = domainContext.systemClient()
                                           .writeSide();
            systemContext = systemOf(domainContext);
        }

        @AfterEach
        void tearDown() throws Exception {
            domainContext.close();
        }

        @Test
        @DisplayName("events")
        void events() {
            OrderPlaced event = OrderPlaced
                    .newBuilder()
                    .setId(projectionId)
                    .addItem("Pizza")
                    .vBuild();
            systemWriteSide.postEvent(event);

            MealOrder order = projectionState();
            ProtoTruth.assertThat(order.getWhenPlaced())
                      .isNotEqualToDefaultInstance();
            assertThat(order.getItemList())
                    .isEqualTo(event.getItemList());
        }

        private MealOrder projectionState() {
            MealOrderProjection aggregate = findProjection(projectionId, systemContext);
            return aggregate.state();
        }
    }
}
