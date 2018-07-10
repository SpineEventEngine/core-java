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
import io.spine.people.PersonName;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.InProcessSharding;
import io.spine.server.delivery.Sharding;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.system.server.given.EntityHistoryTestEnv.HistoryEventSubscriber;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregate;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregatePart;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregatePartRepository;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregateRepository;
import io.spine.system.server.given.EntityHistoryTestEnv.TestProjection;
import io.spine.system.server.given.EntityHistoryTestEnv.TestProjectionRepository;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.option.EntityOption.Kind.PROJECTION;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.SystemBoundedContexts.systemOf;
import static io.spine.server.storage.memory.InMemoryStorageFactory.newInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

        private HistoryEventSubscriber eventWatcher;
        private String id;

        @BeforeEach
        void setUp() {
            eventWatcher = new HistoryEventSubscriber();
            system.getEventBus()
                  .register(eventWatcher);
            id = Identifier.newUuid();
        }

        @Test
        @DisplayName("entity is created")
        void entityCreated() {
            createPerson();
            eventWatcher.assertEventCount(7);

            checkEntityCreated(AGGREGATE, TestAggregate.TYPE);
            checkCommandDispatchedToAggregateHandler();
            checkEventDispatchedToApplier();
            checkEntityStateChanged(Person.newBuilder()
                                          .setId(id)
                                          .setName(PersonName.getDefaultInstance())
                                          .build());
            checkEntityCreated(PROJECTION, TestProjection.TYPE);
            checkEventDispatchedToSubscriber();
            checkEntityStateChanged(PersonView.newBuilder()
                                              .setId(id)
                                              .setName(PersonName.getDefaultInstance())
                                              .build());
        }

        @Test
        @DisplayName("entity is archived or deleted")
        void archivedAndDeleted() {
            hidePerson();
            eventWatcher.assertEventCount(7);

            eventWatcher.nextEvent(EntityCreated.class);
            eventWatcher.nextEvent(CommandDispatchedToHandler.class);
            eventWatcher.nextEvent(EventDispatchedToApplier.class);

            checkEntityArchived();

            eventWatcher.nextEvent(EntityCreated.class);
            eventWatcher.nextEvent(EventDispatchedToSubscriber.class);

            checkEntityDeleted();
        }

        @Test
        @DisplayName("entity is extracted from archive or restored after deletion")
        void unArchivedAndUnDeleted() {
            hidePerson();
            eventWatcher.clearEvents();

            UnHidePerson command = UnHidePerson
                    .newBuilder()
                    .setId(id)
                    .build();
            postCommand(command);

            eventWatcher.assertEventCount(5);

            eventWatcher.nextEvent(CommandDispatchedToHandler.class);
            eventWatcher.nextEvent(EventDispatchedToApplier.class);

            checkEntityExtracted();

            eventWatcher.nextEvent(EventDispatchedToSubscriber.class);

            checkEntityRestored();
        }

        @Test
        @DisplayName("command dispatched to handler in aggregate")
        void commandToAggregate() {
            createPerson();
            eventWatcher.clearEvents();

            Message domainCommand = hidePerson();
            assertCommandDispatched(domainCommand);

            eventWatcher.nextEvent(EventDispatchedToApplier.class);
        }

        @Test
        @DisplayName("command dispatched to handler in aggregate part")
        void commandToPart() {
            CreatePersonName domainCommand = CreatePersonName
                    .newBuilder()
                    .setId(id)
                    .setFirstName("Ringo")
                    .build();
            postCommand(domainCommand);

            checkEntityCreated(AGGREGATE, TestAggregatePart.TYPE);
            assertCommandDispatched(domainCommand);
            eventWatcher.nextEvent(EventDispatchedToApplier.class);
        }

        private void createPerson() {
            CreatePerson command = CreatePerson.newBuilder()
                                               .setId(id)
                                               .build();
            postCommand(command);
        }

        private HidePerson hidePerson() {
            HidePerson command = HidePerson.newBuilder()
                                           .setId(id)
                                           .build();
            postCommand(command);
            return command;
        }

        private void assertCommandDispatched(Message command) {
            CommandDispatchedToHandler commandDispatched =
                    eventWatcher.nextEvent(CommandDispatchedToHandler.class);
            assertId(commandDispatched.getReceiver());
            Message commandMessage = unpack(commandDispatched.getPayload()
                                                             .getCommand()
                                                             .getMessage());
            assertEquals(command, commandMessage);
        }

        private void checkEntityCreated(EntityOption.Kind entityKind,
                                        TypeUrl entityType) {
            EntityCreated entityCreatedEvent = eventWatcher.nextEvent(EntityCreated.class);
            StringValue actualIdValue = unpack(entityCreatedEvent.getId()
                                                                 .getEntityId()
                                                                 .getId());
            assertEquals(id, actualIdValue.getValue());
            assertEquals(entityType.value(), entityCreatedEvent.getId()
                                                               .getTypeUrl());
            assertEquals(entityKind, entityCreatedEvent.getKind());
        }

        private void checkEventDispatchedToSubscriber() {
            EventDispatchedToSubscriber eventDispatchedEvent =
                    eventWatcher.nextEvent(EventDispatchedToSubscriber.class);
            EntityHistoryId receiver = eventDispatchedEvent.getReceiver();
            StringValue actualIdValue = unpack(receiver.getEntityId().getId());
            PersonCreated payload = unpack(eventDispatchedEvent.getPayload()
                                                               .getEvent()
                                                               .getMessage());
            assertEquals(id, actualIdValue.getValue());
            assertEquals(TestProjection.TYPE.value(), receiver.getTypeUrl());
            assertEquals(id, payload.getId());
        }

        private void checkEntityStateChanged(Message state) {
            EntityStateChanged event = eventWatcher.nextEvent(EntityStateChanged.class);
            String actualId = Identifier.unpack(event.getId()
                                                     .getEntityId()
                                                     .getId());
            assertEquals(id, actualId);
            assertEquals(state, unpack(event.getNewState()));
            assertFalse(event.getMessageIdList().isEmpty());
        }

        private void checkEventDispatchedToApplier() {
            EventDispatchedToApplier eventDispatchedEvent =
                    eventWatcher.nextEvent(EventDispatchedToApplier.class);
            EntityHistoryId receiver = eventDispatchedEvent.getReceiver();
            StringValue actualIdValue = unpack(receiver.getEntityId().getId());
            PersonCreated payload = unpack(eventDispatchedEvent.getPayload()
                                                               .getEvent()
                                                               .getMessage());
            assertEquals(id, actualIdValue.getValue());
            assertEquals(TestAggregate.TYPE.value(), receiver.getTypeUrl());
            assertEquals(id, payload.getId());
        }

        private void checkCommandDispatchedToAggregateHandler() {
            CommandDispatchedToHandler commandDispatchedEvent =
                    eventWatcher.nextEvent(CommandDispatchedToHandler.class);
            EntityHistoryId receiver = commandDispatchedEvent.getReceiver();
            StringValue actualIdValue = unpack(receiver.getEntityId().getId());
            CreatePerson payload = unpack(commandDispatchedEvent.getPayload()
                                                                .getCommand()
                                                                .getMessage());
            assertEquals(id, actualIdValue.getValue());
            assertEquals(TestAggregate.TYPE.value(), receiver.getTypeUrl());
            assertEquals(id, payload.getId());
        }

        private void checkEntityArchived() {
            EntityArchived archivedEvent = eventWatcher.nextEvent(EntityArchived.class);

            assertEquals(TestAggregate.TYPE.value(),
                         archivedEvent.getId().getTypeUrl());
            String actualId = Identifier.unpack(archivedEvent.getId()
                                                             .getEntityId()
                                                             .getId());
            assertEquals(id, actualId);
        }

        private void checkEntityDeleted() {
            EntityDeleted deletedEvent = eventWatcher.nextEvent(EntityDeleted.class);

            assertEquals(TestProjection.TYPE.value(),
                         deletedEvent.getId().getTypeUrl());
            String actualId = Identifier.unpack(deletedEvent.getId()
                                                            .getEntityId()
                                                            .getId());
            assertEquals(id, actualId);
        }

        private void checkEntityExtracted() {
            EntityExtractedFromArchive extractedEvent =
                    eventWatcher.nextEvent(EntityExtractedFromArchive.class);

            assertEquals(TestAggregate.TYPE.value(),
                         extractedEvent.getId().getTypeUrl());
            String actualId = Identifier.unpack(extractedEvent.getId()
                                                              .getEntityId()
                                                              .getId());
            assertEquals(id, actualId);
        }

        private void checkEntityRestored() {
            EntityRestored restoredEvent = eventWatcher.nextEvent(EntityRestored.class);

            assertEquals(TestProjection.TYPE.value(),
                         restoredEvent.getId().getTypeUrl());
            String actualId = Identifier.unpack(restoredEvent.getId()
                                                             .getEntityId()
                                                             .getId());
            assertEquals(id, actualId);
        }

        private void assertId(EntityHistoryId actual) {
            String actualId = Identifier.unpack(actual.getEntityId().getId());
            assertEquals(id, actualId);
        }
    }

    private void postCommand(Message commandMessage) {
        Command command = requestFactory.createCommand(commandMessage);
        context.getCommandBus().post(command, noOpObserver());
    }
}

