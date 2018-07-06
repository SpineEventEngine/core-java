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
import com.google.protobuf.StringValue;
import io.spine.base.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.option.EntityOption;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.InProcessSharding;
import io.spine.server.delivery.Sharding;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.system.server.given.EntityHistoryTestEnv.HistoryEventSubscriber;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregatePartRepository;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregateRepository;
import io.spine.system.server.given.EntityHistoryTestEnv.TestProjectionRepository;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.option.EntityOption.Kind.PROJECTION;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.SystemBoundedContexts.systemOf;
import static io.spine.server.storage.memory.InMemoryStorageFactory.newInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("EntityHistory should")
@SuppressWarnings("InnerClassMayBeStatic")
class EntityHistoryTest {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(EntityHistoryTest.class);

    private BoundedContext context;
    private BoundedContext system;

    @BeforeEach
    void setUp() {
        BoundedContextName contextName = BoundedContextName
                .newBuilder()
                .setValue(EntityHistoryTest.class.getSimpleName())
                .build();
        context = BoundedContext
                .newBuilder()
                .setName(contextName)
                .setStorageFactorySupplier(() -> newInstance(contextName, false))
                .build();
        system = systemOf(context);

        context.register(new TestAggregateRepository());
        context.register(new TestProjectionRepository());
        context.register(new TestAggregatePartRepository());
    }

    @AfterEach
    void tearDown() {
        Sharding sharding = new InProcessSharding(InMemoryTransportFactory.newInstance());
        ServerEnvironment.getInstance()
                         .replaceSharding(sharding);
    }

    @Nested
    @DisplayName("produce system events when")
    class ProduceEvents {

        private HistoryEventSubscriber eventSubscriber;
        private String id;

        @BeforeEach
        void setUp() {
            eventSubscriber = new HistoryEventSubscriber();
            system.getEventBus()
                  .register(eventSubscriber);
            id = Identifier.newUuid();
        }

        @Test
        @DisplayName("entity is created")
        void entityCreated() {
            Command command = requestFactory.createCommand(CreatePerson.newBuilder()
                                                                       .setId(id)
                                                                       .build());
            context.getCommandBus()
                   .post(command, noOpObserver());
            List<? extends Message> systemEvents = eventSubscriber.events();
            assertEquals(2, systemEvents.size());
            checkEntityCreated(systemEvents.get(0), AGGREGATE, TypeUrl.of(Person.class));
            checkEntityCreated(systemEvents.get(1), PROJECTION, TypeUrl.of(PersonView.class));
        }

        private void checkEntityCreated(Message event,
                                        EntityOption.Kind entityKind,
                                        TypeUrl entityType) {
            assertThat(event, instanceOf(EntityCreated.class));
            EntityCreated entityCreatedEvent = (EntityCreated) event;
            StringValue actualIdValue = unpack(entityCreatedEvent.getId()
                                                                 .getEntityId()
                                                                 .getId());
            assertEquals(id, actualIdValue.getValue());
            assertEquals(entityType.value(), entityCreatedEvent.getId()
                                                               .getTypeUrl());
            assertEquals(entityKind, entityCreatedEvent.getKind());
        }
    }

}

