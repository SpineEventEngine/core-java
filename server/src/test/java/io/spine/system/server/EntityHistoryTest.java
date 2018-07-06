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
            assertEquals(5, systemEvents.size());

            TypeUrl aggregateType = TypeUrl.of(Person.class);
            TypeUrl projectionType = TypeUrl.of(PersonView.class);

            checkEntityCreated(systemEvents.get(0), AGGREGATE, aggregateType);
            checkCommandDispatchedToHandler(systemEvents.get(1), aggregateType);
            checkEventDispatchedToApplier(systemEvents.get(2), aggregateType);
            checkEntityCreated(systemEvents.get(3), PROJECTION, projectionType);
            checkEventDispatchedToSubscriber(systemEvents.get(4), projectionType);
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

        private void checkEventDispatchedToSubscriber(Message event,
                                                      TypeUrl entityType) {
            assertThat(event, instanceOf(EventDispatchedToSubscriber.class));
            EventDispatchedToSubscriber eventDispatchedEvent = (EventDispatchedToSubscriber) event;
            StringValue actualIdValue = unpack(eventDispatchedEvent.getReceiver()
                                                                 .getEntityId()
                                                                 .getId());
            PersonCreated payload = unpack(eventDispatchedEvent.getPayload()
                                                             .getEvent()
                                                             .getMessage());
            assertEquals(id, actualIdValue.getValue());
            assertEquals(entityType.value(), eventDispatchedEvent.getReceiver()
                                                               .getTypeUrl());
            assertEquals(id, payload.getId());
        }

        private void checkEventDispatchedToApplier(Message event, TypeUrl entityType) {
            assertThat(event, instanceOf(EventDispatchedToApplier.class));
            EventDispatchedToApplier eventDispatchedEvent = (EventDispatchedToApplier) event;
            StringValue actualIdValue = unpack(eventDispatchedEvent.getReceiver()
                                                                   .getEntityId()
                                                                   .getId());
            PersonCreated payload = unpack(eventDispatchedEvent.getPayload()
                                                               .getEvent()
                                                               .getMessage());
            assertEquals(id, actualIdValue.getValue());
            assertEquals(entityType.value(), eventDispatchedEvent.getReceiver()
                                                                 .getTypeUrl());
            assertEquals(id, payload.getId());
        }

        private void checkCommandDispatchedToHandler(Message event, TypeUrl entityType) {
            assertThat(event, instanceOf(CommandDispatchedToHandler.class));
            CommandDispatchedToHandler commandDispatchedEvent = (CommandDispatchedToHandler) event;
            StringValue actualIdValue = unpack(commandDispatchedEvent.getReceiver()
                                                                     .getEntityId()
                                                                     .getId());
            CreatePerson payload = unpack(commandDispatchedEvent.getPayload()
                                                                .getCommand()
                                                                .getMessage());
            assertEquals(id, actualIdValue.getValue());
            assertEquals(entityType.value(), commandDispatchedEvent.getReceiver()
                                                                   .getTypeUrl());
            assertEquals(id, payload.getId());
        }
    }
}

