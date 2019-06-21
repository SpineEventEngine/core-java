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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.event.EventBus;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.system.server.given.client.ShoppingListAggregate;
import io.spine.test.system.server.ListId;
import io.spine.test.system.server.ShoppingList;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Optional;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static io.spine.base.Identifier.newUuid;
import static io.spine.core.BoundedContextNames.assumingTestsValue;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.ContextSpec.singleTenant;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.system.server.given.client.SystemClientTestEnv.findAggregate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SystemReadSide should")
class DefaultSystemReadSideTest {

    private static final TestEventFactory events =
            TestEventFactory.newInstance(DefaultSystemReadSideTest.class);

    private BoundedContext domainContext;
    private SystemReadSide systemReadSide;

    @BeforeEach
    void setUp() {
        domainContext = BoundedContextBuilder
                .assumingTests()
                .build();
        systemReadSide = domainContext.systemClient().readSide();
    }

    @AfterEach
    void tearDown() throws Exception {
        domainContext.close();
    }

    @Test
    @DisplayName("not allow nulls on construction")
    void notAllowNullsOnConstruction() {
        InMemoryStorageFactory storageFactory =
                InMemoryStorageFactory.newInstance(singleTenant(assumingTestsValue()));
        new NullPointerTester()
                .setDefault(EventBus.class, EventBus
                        .newBuilder()
                        .setStorageFactory(storageFactory)
                        .build())
                .testStaticMethods(DefaultSystemReadSide.class, PACKAGE);
    }

    @Test
    @DisplayName("not allow nulls")
    void notAllowNulls() {
        new NullPointerTester()
                .testAllPublicInstanceMethods(systemReadSide);
    }

    @Nested
    class RegisterSystemEventDispatchers {

        @Test
        @DisplayName("pass system events to the domain")
        void passSystemEvents() {
            ProjectCreatedSubscriber subscriber = new ProjectCreatedSubscriber();
            systemReadSide.register(subscriber);

            EventMessage systemEvent = postSystemEvent();
            Optional<EventMessage> receivedEvent = subscriber.lastEvent();
            assertTrue(receivedEvent.isPresent());
            EventMessage actualEvent = receivedEvent.get();
            assertEquals(systemEvent, actualEvent);
        }

        @Test
        @DisplayName("unregister dispatchers")
        void unregisterDispatchers() {
            ProjectCreatedSubscriber subscriber = new ProjectCreatedSubscriber();
            systemReadSide.register(subscriber);
            systemReadSide.unregister(subscriber);

            postSystemEvent();
            Optional<EventMessage> receivedEvent = subscriber.lastEvent();
            assertFalse(receivedEvent.isPresent());
        }

        @CanIgnoreReturnValue
        private EventMessage postSystemEvent() {
            BoundedContext systemContext = systemOf(domainContext);
            EventMessage systemEvent = SMProjectCreated
                    .newBuilder()
                    .setUuid(newUuid())
                    .setName("System Bus test project")
                    .build();
            Event event = events.createEvent(systemEvent);
            systemContext.eventBus().post(event);
            return systemEvent;
        }
    }

    @Nested
    @DisplayName("read domain aggregate states")
    class ReadDomainAggregates {

        private final TestActorRequestFactory actorRequestFactory =
                new TestActorRequestFactory(DefaultSystemWriteSideTest.class);

        private ListId aggregateId;

        @BeforeEach
        void setUp() {
            domainContext.register(DefaultRepository.of(ShoppingListAggregate.class));
            aggregateId = ListId
                    .newBuilder()
                    .setId(newUuid())
                    .build();
            createAggregate();
        }

        @Test
        @DisplayName("by the given query")
        void query() {
            Query query =
                    actorRequestFactory.query()
                                       .byIds(ShoppingList.class, ImmutableSet.of(aggregateId));
            Iterator<EntityStateWithVersion> iterator = systemReadSide.readDomainAggregate(query);
            EntityStateWithVersion next = iterator.next();
            Message foundMessage = unpack(next.getState());

            ShoppingListAggregate aggregate = aggregate();
            assertEquals(aggregate.state(), foundMessage);
            assertEquals(aggregate.version(), next.getVersion());
        }

        private ShoppingListAggregate aggregate() {
            return findAggregate(aggregateId, domainContext);
        }

        private void createAggregate() {
            CreateShoppingList command = CreateShoppingList
                    .newBuilder()
                    .setId(aggregateId)
                    .build();
            Command cmd = actorRequestFactory.command()
                                             .create(command);
            domainContext.commandBus()
                         .post(cmd, noOpObserver());
        }
    }

    /**
     * A subscriber for {@link SMProjectCreated} events.
     *
     * <p>Memoizes the last received event and reports it on {@link #lastEvent()} calls.
     */
    private static final class ProjectCreatedSubscriber extends AbstractEventSubscriber {

        private EventMessage lastEvent;

        @Subscribe
        void on(SMProjectCreated event) {
            lastEvent = event;
        }

        private Optional<EventMessage> lastEvent() {
            return Optional.ofNullable(lastEvent);
        }
    }
}
