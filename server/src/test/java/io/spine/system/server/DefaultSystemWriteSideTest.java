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

import io.spine.server.BoundedContext;
import io.spine.system.server.given.client.ShoppingListProjection;
import io.spine.test.system.server.ListId;
import io.spine.test.system.server.ShoppingList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.system.server.given.client.SystemClientTestEnv.contextWithSystemAggregate;
import static io.spine.system.server.given.client.SystemClientTestEnv.findAggregate;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Default implementation of SystemWriteSide should")
class DefaultSystemWriteSideTest {

    private SystemWriteSide systemWriteSide;
    private ListId aggregateId;

    @BeforeEach
    void setUp() {
        aggregateId = ListId
                .newBuilder()
                .setId(newUuid())
                .build();
    }

    @Nested
    @DisplayName("post system")
    class PostMessages {

        private BoundedContext domainContext;
        private BoundedContext systemContext;

        @BeforeEach
        void setUp() {
            domainContext = contextWithSystemAggregate();
            systemWriteSide = domainContext.systemClient().writeSide();
            systemContext = systemOf(domainContext);
            createAggregate();
        }

        @AfterEach
        void tearDown() throws Exception {
            domainContext.close();
        }

        @Test
        @DisplayName("events")
        void events() {
            int copiesCount = aggregateState().getHardCopiesCount();

            HardCopyPrinted event = HardCopyPrinted
                    .newBuilder()
                    .setListId(aggregateId)
                    .vBuild();
            systemWriteSide.postEvent(event);

            int newCopiesCount = aggregateState().getHardCopiesCount();
            assertEquals(copiesCount + 1, newCopiesCount);
        }

        private ShoppingList aggregateState() {
            ShoppingListProjection aggregate = findAggregate(aggregateId, systemContext);
            return aggregate.state();
        }

        private void createAggregate() {
            ShoppingListCreated command = ShoppingListCreated
                    .newBuilder()
                    .setId(aggregateId)
                    .vBuild();
            systemWriteSide.postEvent(command);
        }
    }
}
