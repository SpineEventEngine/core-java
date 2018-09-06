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

package io.spine.system.server;

import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.core.Command;
import io.spine.server.BoundedContext;
import io.spine.system.server.given.gateway.ShoppingListAggregate;
import io.spine.system.server.given.gateway.ShoppingListRepository;
import io.spine.test.system.server.ListId;
import io.spine.test.system.server.ShoppingList;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.system.server.given.gateway.DefaultSystemGatewayTestEnv.contextWithDomainAggregate;
import static io.spine.system.server.given.gateway.DefaultSystemGatewayTestEnv.contextWithSystemAggregate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("Default implementation of SystemGateway should")
class DefaultSystemGatewayTest  {

    private SystemGateway gateway;
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

        private BoundedContext systemContext;

        @BeforeEach
        void setUp() {
            BoundedContext context = contextWithSystemAggregate();
            gateway = context.getSystemGateway();
            systemContext = systemOf(context);
            createAggregate();
        }

        @Test
        @DisplayName("events")
        void events() {
            int copiesCount = aggregate().getHardCopiesCount();

            HardCopyPrinted event = HardCopyPrinted
                    .newBuilder()
                    .setListId(aggregateId)
                    .build();
            gateway.postEvent(event);

            int newCopiesCount = aggregate().getHardCopiesCount();
            assertEquals(copiesCount + 1, newCopiesCount);
        }

        @Test
        @DisplayName("commands")
        void commands() {
            List<String> items = aggregate().getItemList();
            assertTrue(items.isEmpty());

            AddListItem command = AddListItem
                    .newBuilder()
                    .setListId(aggregateId)
                    .setItem("Milk")
                    .build();
            gateway.postCommand(command);

            List<String> newItems = aggregate().getItemList();
            assertEquals(1, newItems.size());
            assertEquals(command.getItem(), newItems.get(0));
        }

        private ShoppingList aggregate() {
            return findAggreagte(systemContext);
        }

        private void createAggregate() {
            CreateShoppingList command = CreateShoppingList
                    .newBuilder()
                    .setId(aggregateId)
                    .build();
            gateway.postCommand(command);
        }
    }

    @Nested
    @DisplayName("read domain aggregate states")
    class ReadDomainAggregates {

        private final TestActorRequestFactory actorRequestFactory =
                TestActorRequestFactory.newInstance(DefaultSystemGatewayTest.class);

        private BoundedContext domainContext;

        @BeforeEach
        void setUp() {
            domainContext = contextWithDomainAggregate();
            gateway = domainContext.getSystemGateway();
            createAggregate();
        }

        @Test
        @DisplayName("by the given query")
        void query() {
            Query query = actorRequestFactory.query()
                                             .byIds(ShoppingList.class, of(aggregateId));
            Message foundMessage = unpack(gateway.readDomainAggregate(query).next());
            assertEquals(aggregate(), foundMessage);
        }

        private ShoppingList aggregate() {
            return findAggreagte(domainContext);
        }

        private void createAggregate() {
            CreateShoppingList command = CreateShoppingList
                    .newBuilder()
                    .setId(aggregateId)
                    .build();
            Command cmd = actorRequestFactory.createCommand(command);
            domainContext.getCommandBus()
                         .post(cmd, noOpObserver());
        }
    }

    private ShoppingList findAggreagte(BoundedContext context) {
        ShoppingListRepository repository = (ShoppingListRepository)
                context.findRepository(ShoppingList.class)
                             .orElseGet(() -> fail("Repository should be registered."));
        ShoppingListAggregate aggregate =
                repository.find(aggregateId)
                          .orElseGet(() -> fail("Aggregate should be present."));
        return aggregate.getState();
    }
}
