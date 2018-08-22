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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.grpc.MemoizingObserver;
import io.spine.people.PersonName;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.delivery.InProcessSharding;
import io.spine.server.delivery.Sharding;
import io.spine.server.entity.EntityKind;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.system.server.given.EntityHistoryTestEnv.HistoryEventWatcher;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregate;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregatePart;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregatePartRepository;
import io.spine.system.server.given.EntityHistoryTestEnv.TestAggregateRepository;
import io.spine.system.server.given.EntityHistoryTestEnv.TestProcman;
import io.spine.system.server.given.EntityHistoryTestEnv.TestProcmanRepository;
import io.spine.system.server.given.EntityHistoryTestEnv.TestProjection;
import io.spine.system.server.given.EntityHistoryTestEnv.TestProjectionRepository;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.blackbox.CommandMemoizingTap;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.EntityKind.AGGREGATE;
import static io.spine.server.entity.EntityKind.PROCESS_MANAGER;
import static io.spine.server.entity.EntityKind.PROJECTION;
import static io.spine.server.storage.memory.InMemoryStorageFactory.newInstance;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private CommandMemoizingTap commandMemoizingTap;

    @BeforeEach
    void setUp() {
        BoundedContextName contextName = BoundedContextName
                .newBuilder()
                .setValue(EntityHistoryTest.class.getSimpleName())
                .build();
        commandMemoizingTap = new CommandMemoizingTap();
        CommandBus.Builder commandBus = CommandBus.newBuilder()
                .appendFilter(commandMemoizingTap);
        context = BoundedContext
                .newBuilder()
                .setName(contextName)
                .setCommandBus(commandBus)
                .setStorageFactorySupplier(() -> newInstance(contextName, false))
                .build();
        system = systemOf(context);

        context.register(new TestAggregateRepository());
        context.register(new TestProjectionRepository());
        context.register(new TestAggregatePartRepository());
        context.register(new TestProcmanRepository());
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

        private HistoryEventWatcher eventAccumulator;
        private PersonId id;

        @BeforeEach
        void setUp() {
            eventAccumulator = new HistoryEventWatcher();
            system.getEventBus()
                  .register(eventAccumulator);
            id = PersonId.newBuilder()
                         .setUuid(newUuid())
                         .build();
        }

        @Test
        @DisplayName("entity is created")
        void entityCreated() {
            createPerson();
            eventAccumulator.assertEventCount(6);

            checkEntityCreated(AGGREGATE, TestAggregate.TYPE);
            checkCommandDispatchedToAggregateHandler();
            checkEntityStateChanged(Person.newBuilder()
                                          .setId(id)
                                          .setName(PersonName.getDefaultInstance())
                                          .build());
            checkEntityCreated(PROJECTION, TestProjection.TYPE);
            checkEventDispatchedToSubscriber();
            checkEntityStateChanged(PersonDetails.newBuilder()
                                                 .setId(id)
                                                 .setName(PersonName.getDefaultInstance())
                                                 .build());
        }

        @Test
        @DisplayName("entity is archived or deleted")
        void archivedAndDeleted() {
            hidePerson();
            eventAccumulator.assertEventCount(6);

            eventAccumulator.nextEvent(EntityCreated.class);
            eventAccumulator.nextEvent(CommandDispatchedToHandler.class);

            checkEntityArchived();

            eventAccumulator.nextEvent(EntityCreated.class);
            eventAccumulator.nextEvent(EventDispatchedToSubscriber.class);

            checkEntityDeleted();
        }

        @Test
        @DisplayName("entity is extracted from archive or restored after deletion")
        void unArchivedAndUnDeleted() {
            hidePerson();
            eventAccumulator.forgetEvents();

            ExposePerson command = ExposePerson
                    .newBuilder()
                    .setId(id)
                    .build();
            postCommand(command);

            eventAccumulator.assertEventCount(4);

            eventAccumulator.nextEvent(CommandDispatchedToHandler.class);

            checkEntityExtracted();

            eventAccumulator.nextEvent(EventDispatchedToSubscriber.class);

            checkEntityRestored();
        }

        @Test
        @DisplayName("command is dispatched to handler in aggregate")
        void commandToAggregate() {
            createPerson();
            eventAccumulator.forgetEvents();

            Message domainCommand = hidePerson();
            assertCommandDispatched(domainCommand);
        }

        @Test
        @DisplayName("command is dispatched to handler in aggregate part")
        void commandToPart() {
            Message domainCommand = createPersonName();
            checkEntityCreated(AGGREGATE, TestAggregatePart.TYPE);
            assertCommandDispatched(domainCommand);
        }

        @Test
        @DisplayName("command is dispatched to handler in procman")
        void commandToPm() {
            Message startCommand = StartPersonCreation
                    .newBuilder()
                    .setId(id)
                    .build();
            postCommand(startCommand);

            checkEntityCreated(PROCESS_MANAGER, TestProcman.TYPE);
            eventAccumulator.nextEvent(CommandDispatchedToHandler.class);
            EntityStateChanged stateChanged = eventAccumulator.nextEvent(EntityStateChanged.class);
            assertId(stateChanged.getId());
            PersonCreation startedState = unpack(stateChanged.getNewState());
            assertFalse(startedState.getCreated());
            eventAccumulator.forgetEvents();

            Message domainCommand = CompletePersonCreation
                    .newBuilder()
                    .setId(id)
                    .build();
            postCommand(domainCommand);

            eventAccumulator.nextEvent(CommandDispatchedToHandler.class);
            EntityStateChanged stateChangedAgain = eventAccumulator.nextEvent(EntityStateChanged.class);
            assertId(stateChangedAgain.getId());
            PersonCreation completedState = unpack(stateChangedAgain.getNewState());
            assertTrue(completedState.getCreated());
        }

        @Test
        @DisplayName("event is dispatched to a reactor method in a ProcessManager")
        void eventToReactorInProcman() {
            createPersonName();

            eventAccumulator.nextEvent(EntityCreated.class);
            eventAccumulator.nextEvent(CommandDispatchedToHandler.class);
            eventAccumulator.nextEvent(EntityStateChanged.class);

            checkEntityCreated(PROCESS_MANAGER, TestProcman.TYPE);
            EventDispatchedToReactor dispatchedToReactor =
                    eventAccumulator.nextEvent(EventDispatchedToReactor.class);
            assertId(dispatchedToReactor.getReceiver());

            TypeUrl expectedType = TypeUrl.of(PersonNameCreated.class);
            TypeUrl actualType = TypeUrl.of(findEvent(dispatchedToReactor.getPayload()));
            assertEquals(expectedType, actualType);

            EntityStateChanged stateChanged = eventAccumulator.nextEvent(EntityStateChanged.class);
            PersonCreation processState = unpack(stateChanged.getNewState());
            assertEquals(id, processState.getId());
            assertTrue(processState.getCreated());
        }

        @Test
        @DisplayName("event is dispatched to a reactor method in an Aggregate")
        void eventToReactorInAggregate() {
            createPerson();
            createPersonName();
            eventAccumulator.forgetEvents();

            RenamePerson domainCommand = RenamePerson
                    .newBuilder()
                    .setId(id)
                    .setNewFirstName("Paul")
                    .build();
            postCommand(domainCommand);

            eventAccumulator.nextEvent(CommandDispatchedToHandler.class);
            eventAccumulator.nextEvent(EntityStateChanged.class);

            EventDispatchedToReactor dispatched =
                    eventAccumulator.nextEvent(EventDispatchedToReactor.class);
            assertId(dispatched.getReceiver());
            TypeUrl expectedType = TypeUrl.of(PersonRenamed.class);
            TypeUrl actualType = TypeUrl.of(findEvent(dispatched.getPayload()));
            assertEquals(expectedType, actualType);
        }

        private void createPerson() {
            CreatePerson command = CreatePerson.newBuilder()
                                               .setId(id)
                                               .build();
            postCommand(command);
        }

        @CanIgnoreReturnValue
        private HidePerson hidePerson() {
            HidePerson command = HidePerson.newBuilder()
                                           .setId(id)
                                           .build();
            postCommand(command);
            return command;
        }

        @CanIgnoreReturnValue
        private CreatePersonName createPersonName() {
            CreatePersonName domainCommand = CreatePersonName
                    .newBuilder()
                    .setId(id)
                    .setFirstName("Ringo")
                    .build();
            postCommand(domainCommand);
            return domainCommand;
        }

        private void assertCommandDispatched(Message command) {
            CommandDispatchedToHandler commandDispatched =
                    eventAccumulator.nextEvent(CommandDispatchedToHandler.class);
            assertId(commandDispatched.getReceiver());
            Message commandMessage = findCommand(commandDispatched.getPayload());
            assertEquals(command, commandMessage);
        }

        private void checkEntityCreated(EntityKind entityKind,
                                        TypeUrl entityType) {
            EntityCreated entityCreatedEvent = eventAccumulator.nextEvent(EntityCreated.class);
            assertId(entityCreatedEvent.getId());
            assertEquals(entityType.value(), entityCreatedEvent.getId()
                                                               .getTypeUrl());
            assertEquals(entityKind, entityCreatedEvent.getRepositorySpec().getKind());
        }

        private void checkEventDispatchedToSubscriber() {
            EventDispatchedToSubscriber eventDispatchedEvent =
                    eventAccumulator.nextEvent(EventDispatchedToSubscriber.class);
            EntityHistoryId receiver = eventDispatchedEvent.getReceiver();
            PersonId actualIdValue = unpack(receiver.getEntityId().getId());
            DispatchedEvent dispatchedEvent = eventDispatchedEvent.getPayload();
            PersonCreated payload = findEvent(dispatchedEvent, PersonCreated.class);
            assertEquals(id, actualIdValue);
            assertEquals(TestProjection.TYPE.value(), receiver.getTypeUrl());
            assertEquals(id, payload.getId());
        }

        private void checkEntityStateChanged(Message state) {
            EntityStateChanged event = eventAccumulator.nextEvent(EntityStateChanged.class);
            assertId(event.getId());
            assertEquals(state, unpack(event.getNewState()));
            assertFalse(event.getMessageIdList().isEmpty());
        }

        private void checkCommandDispatchedToAggregateHandler() {
            CommandDispatchedToHandler commandDispatchedEvent =
                    eventAccumulator.nextEvent(CommandDispatchedToHandler.class);
            EntityHistoryId receiver = commandDispatchedEvent.getReceiver();
            PersonId actualIdValue = unpack(receiver.getEntityId().getId());
            DispatchedCommand dispatchedCommand = commandDispatchedEvent.getPayload();
            CreatePerson payload = findCommand(dispatchedCommand, CreatePerson.class);
            assertEquals(id, actualIdValue);
            assertEquals(TestAggregate.TYPE.value(), receiver.getTypeUrl());
            assertEquals(id, payload.getId());
        }

        private void checkEntityArchived() {
            EntityArchived archivedEvent = eventAccumulator.nextEvent(EntityArchived.class);

            assertEquals(TestAggregate.TYPE.value(),
                         archivedEvent.getId().getTypeUrl());
            assertId(archivedEvent.getId());
        }

        private void checkEntityDeleted() {
            EntityDeleted deletedEvent = eventAccumulator.nextEvent(EntityDeleted.class);

            assertEquals(TestProjection.TYPE.value(),
                         deletedEvent.getId()
                                     .getTypeUrl());
            assertId(deletedEvent.getId());
        }

        private void checkEntityExtracted() {
            EntityExtractedFromArchive extractedEvent =
                    eventAccumulator.nextEvent(EntityExtractedFromArchive.class);

            assertEquals(TestAggregate.TYPE.value(),
                         extractedEvent.getId()
                                       .getTypeUrl());
            PersonId actualId = Identifier.unpack(extractedEvent.getId()
                                                                .getEntityId()
                                                                .getId());
            assertEquals(id, actualId);
        }

        private void checkEntityRestored() {
            EntityRestored restoredEvent = eventAccumulator.nextEvent(EntityRestored.class);

            assertEquals(TestProjection.TYPE.value(),
                         restoredEvent.getId()
                                      .getTypeUrl());
            assertId(restoredEvent.getId());
        }

        private void assertId(EntityHistoryId actual) {
            PersonId actualId = Identifier.unpack(actual.getEntityId().getId());
            assertEquals(id, actualId);
        }
    }

    private void postCommand(Message commandMessage) {
        Command command = requestFactory.createCommand(commandMessage);
        context.getCommandBus().post(command, noOpObserver());
    }

    private Message findCommand(DispatchedCommand dispatchedCommand) {
        return findCommand(dispatchedCommand, Message.class);
    }

    private <M extends Message> M findCommand(DispatchedCommand dispatchedCommand,
                                              Class<M> commandClass) {
        CommandId commandId = dispatchedCommand.getCommand();
        Optional<M> found = commandMemoizingTap.find(commandId, commandClass);
        assertTrue(found.isPresent());
        return found.get();
    }

    private Message findEvent(DispatchedEvent dispatchedEvent) {
        return findEvent(dispatchedEvent, Message.class);
    }

    private <M extends Message> M findEvent(DispatchedEvent dispatchedEvent,
                                            Class<M> eventClass) {
        MemoizingObserver<Event> eventObserver = memoizingObserver();
        context.getEventBus()
               .getEventStore()
               .read(EventStreamQuery.getDefaultInstance(), eventObserver);
        EventId expectedId = dispatchedEvent.getEvent();
        String errorMessage = format("Event with ID %s not found.", expectedId.getValue());
        Any eventAny = eventObserver.responses()
                                  .stream()
                                  .filter(event -> expectedId.equals(event.getId()))
                                  .findAny()
                                  .map(Event::getMessage)
                                  .orElseThrow(() -> newIllegalStateException(errorMessage));
        Message eventMessage = unpack(eventAny);
        assertThat(eventMessage, instanceOf(eventClass));
        @SuppressWarnings("unchecked") // Checked with an assertion.
        M result = (M) eventMessage;
        return result;
    }
}
