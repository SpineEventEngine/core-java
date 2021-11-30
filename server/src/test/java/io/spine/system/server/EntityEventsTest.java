/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.core.MessageId;
import io.spine.option.EntityOption;
import io.spine.people.PersonName;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.system.server.event.CommandDispatchedToHandler;
import io.spine.system.server.event.EntityArchived;
import io.spine.system.server.event.EntityCreated;
import io.spine.system.server.event.EntityDeleted;
import io.spine.system.server.event.EntityRestored;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.system.server.event.EntityUnarchived;
import io.spine.system.server.event.EventDispatchedToReactor;
import io.spine.system.server.event.EventDispatchedToSubscriber;
import io.spine.system.server.given.entity.HistoryEventWatcher;
import io.spine.system.server.given.entity.PersonAggregate;
import io.spine.system.server.given.entity.PersonNamePart;
import io.spine.system.server.given.entity.PersonProcman;
import io.spine.system.server.given.entity.PersonProcmanRepository;
import io.spine.system.server.given.entity.PersonProjection;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.option.EntityOption.Kind.PROCESS_MANAGER;
import static io.spine.option.EntityOption.Kind.PROJECTION;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("System events should be emitted when")
class EntityEventsTest {

    private static final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(EntityEventsTest.class);

    private BoundedContext context;
    private HistoryEventWatcher eventAccumulator;
    private PersonId id;

    @BeforeEach
    void setUp() {
        context = BoundedContextBuilder.assumingTests(false)
                                       .build();
        var system = systemOf(context);

        var ctx = context.internalAccess();
        ctx.register(DefaultRepository.of(PersonAggregate.class));
        ctx.register(DefaultRepository.of(PersonProjection.class));
        ctx.register(DefaultRepository.of(PersonNamePart.class));
        ctx.register(new PersonProcmanRepository());
        eventAccumulator = new HistoryEventWatcher();
        system.eventBus()
              .register(eventAccumulator);
        id = PersonId.generate();
    }

    @Test
    @DisplayName("entity is created")
    void entityCreated() {
        createPerson();
        eventAccumulator.assertEventCount(6);

        checkCommandDispatchedToAggregateHandler();
        checkEntityCreated(AGGREGATE, PersonAggregate.TYPE);
        checkEntityStateChanged(Person.newBuilder()
                                      .setId(id)
                                      .setName(PersonName.getDefaultInstance())
                                      .build());
        checkEventDispatchedToSubscriber();
        checkEntityCreated(PROJECTION, PersonProjection.TYPE);
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

        eventAccumulator.assertReceivedEvent(CommandDispatchedToHandler.class);
        eventAccumulator.assertReceivedEvent(EntityCreated.class);

        checkEntityArchived();

        eventAccumulator.assertReceivedEvent(EventDispatchedToSubscriber.class);
        eventAccumulator.assertReceivedEvent(EntityCreated.class);

        checkEntityDeleted();
    }

    @Test
    @DisplayName("entity is extracted from archive or restored after deletion")
    void unArchivedAndUnDeleted() {
        hidePerson();
        eventAccumulator.forgetEvents();

        var command = ExposePerson.newBuilder()
                .setId(id)
                .build();
        postCommand(command);

        eventAccumulator.assertEventCount(4);

        eventAccumulator.assertReceivedEvent(CommandDispatchedToHandler.class);

        checkEntityExtracted();

        eventAccumulator.assertReceivedEvent(EventDispatchedToSubscriber.class);

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
        assertCommandDispatched(domainCommand);
        checkEntityCreated(AGGREGATE, PersonNamePart.TYPE);
    }

    @Test
    @DisplayName("command is dispatched to handler in procman")
    void commandToPm() {
        CommandMessage startCommand = StartPersonCreation
                .newBuilder()
                .setId(id)
                .build();
        postCommand(startCommand);

        eventAccumulator.assertReceivedEvent(CommandDispatchedToHandler.class);
        checkEntityCreated(PROCESS_MANAGER, PersonProcman.TYPE);
        var stateChanged =
                eventAccumulator.assertReceivedEvent(EntityStateChanged.class);
        assertId(stateChanged.getEntity());
        var startedState = unpack(stateChanged.getNewState(),
                                  PersonCreation.class);
        assertFalse(startedState.getCreated());
        eventAccumulator.forgetEvents();

        var domainCommand = CompletePersonCreation.newBuilder()
                .setId(id)
                .build();
        postCommand(domainCommand);

        eventAccumulator.assertReceivedEvent(CommandDispatchedToHandler.class);
        var stateChangedAgain =
                eventAccumulator.assertReceivedEvent(EntityStateChanged.class);
        assertId(stateChangedAgain.getEntity());
        var completedState = unpack(stateChangedAgain.getNewState(),
                                    PersonCreation.class);
        assertTrue(completedState.getCreated());
    }

    @Test
    @DisplayName("event is dispatched to a reactor method in a ProcessManager")
    void eventToReactorInProcman() {
        createPersonName();

        eventAccumulator.assertReceivedEvent(CommandDispatchedToHandler.class);
        eventAccumulator.assertReceivedEvent(EntityCreated.class);
        eventAccumulator.assertReceivedEvent(EntityStateChanged.class);

        var dispatchedToReactor =
                eventAccumulator.assertReceivedEvent(EventDispatchedToReactor.class);
        assertId(dispatchedToReactor.getReceiver());

        var expectedType = TypeUrl.of(PersonNameCreated.class);
        var actualType = TypeUrl.ofEnclosed(dispatchedToReactor.getPayload()
                                                               .getMessage());
        assertEquals(expectedType, actualType);

        checkEntityCreated(PROCESS_MANAGER, PersonProcman.TYPE);

        var stateChanged =
                eventAccumulator.assertReceivedEvent(EntityStateChanged.class);
        var processState = unpack(stateChanged.getNewState(),
                                  PersonCreation.class);
        assertEquals(id, processState.getId());
        assertTrue(processState.getCreated());
    }

    @Test
    @DisplayName("event is dispatched to a reactor method in an Aggregate")
    void eventToReactorInAggregate() {
        createPerson();
        createPersonName();
        eventAccumulator.forgetEvents();

        var domainCommand = RenamePerson.newBuilder()
                .setId(id)
                .setNewFirstName("Paul")
                .build();
        postCommand(domainCommand);

        eventAccumulator.assertReceivedEvent(CommandDispatchedToHandler.class);
        eventAccumulator.assertReceivedEvent(EntityStateChanged.class);

        var dispatched =
                eventAccumulator.assertReceivedEvent(EventDispatchedToReactor.class);
        assertId(dispatched.getReceiver());
        var expectedType = TypeUrl.of(PersonRenamed.class);
        var actualType = TypeUrl.ofEnclosed(dispatched.getPayload()
                                                      .getMessage());
        assertEquals(expectedType, actualType);
    }

    private void createPerson() {
        var command = CreatePerson.newBuilder()
                .setId(id)
                .build();
        postCommand(command);
    }

    @CanIgnoreReturnValue
    private HidePerson hidePerson() {
        var command = HidePerson.newBuilder()
                .setId(id)
                .build();
        postCommand(command);
        return command;
    }

    @CanIgnoreReturnValue
    private CreatePersonName createPersonName() {
        var domainCommand = CreatePersonName.newBuilder()
                .setId(id)
                .setFirstName("Ringo")
                .build();
        postCommand(domainCommand);
        return domainCommand;
    }

    private void assertCommandDispatched(Message command) {
        var event =
                eventAccumulator.assertReceivedEvent(CommandDispatchedToHandler.class);
        assertId(event.getReceiver());
        Message commandMessage = event.getPayload()
                                      .enclosedMessage();
        assertEquals(command, commandMessage);
    }

    private void checkEntityCreated(EntityOption.Kind entityKind,
                                    TypeUrl entityType) {
        var event = eventAccumulator.assertReceivedEvent(EntityCreated.class);
        var entityId = event.getEntity();
        assertId(entityId);
        assertEquals(entityType.value(), entityId.getTypeUrl());
        assertEquals(entityKind, event.getKind());
    }

    private void checkEventDispatchedToSubscriber() {
        var event =
                eventAccumulator.assertReceivedEvent(EventDispatchedToSubscriber.class);
        var receiver = event.getReceiver();
        var payload = (PersonCreated) event.getPayload()
                                           .enclosedMessage();
        assertId(receiver);
        assertEquals(PersonProjection.TYPE.value(), receiver.getTypeUrl());
        assertEquals(id, payload.getId());
    }

    private void checkEntityStateChanged(EntityState<?> state) {
        var event = eventAccumulator.assertReceivedEvent(EntityStateChanged.class);
        assertId(event.getEntity());
        assertEquals(state, unpack(event.getNewState()));
        assertFalse(event.getSignalIdList()
                         .isEmpty());
    }

    private void checkCommandDispatchedToAggregateHandler() {
        var commandDispatchedEvent =
                eventAccumulator.assertReceivedEvent(CommandDispatchedToHandler.class);
        var receiver = commandDispatchedEvent.getReceiver();
        var payload = (CreatePerson)
                commandDispatchedEvent.getPayload()
                                      .enclosedMessage();
        assertId(receiver);
        assertEquals(PersonAggregate.TYPE.value(), receiver.getTypeUrl());
        assertEquals(id, payload.getId());
    }

    private void checkEntityArchived() {
        var event = eventAccumulator.assertReceivedEvent(EntityArchived.class);
        var entityId = event.getEntity();
        assertEquals(PersonAggregate.TYPE.value(),
                     entityId.getTypeUrl());
        assertId(entityId);
    }

    private void checkEntityDeleted() {
        var event = eventAccumulator.assertReceivedEvent(EntityDeleted.class);
        var entityId = event.getEntity();
        assertEquals(PersonProjection.TYPE.value(),
                     entityId.getTypeUrl());
        assertId(entityId);
    }

    private void checkEntityExtracted() {
        var event = eventAccumulator.assertReceivedEvent(EntityUnarchived.class);
        var entityId = event.getEntity();
        assertEquals(PersonAggregate.TYPE.value(),
                     entityId.getTypeUrl());
        assertId(entityId);
    }

    private void checkEntityRestored() {
        var event = eventAccumulator.assertReceivedEvent(EntityRestored.class);
        var entityId = event.getEntity();
        assertEquals(PersonProjection.TYPE.value(),
                     entityId.getTypeUrl());
        assertId(entityId);
    }

    private void assertId(MessageId entityId) {
        var idValue = entityId.getId();
        assertEquals(id, Identifier.unpack(idValue));
    }

    private void postCommand(CommandMessage commandMessage) {
        var command = requestFactory.createCommand(commandMessage);
        context.commandBus()
               .post(command, noOpObserver());
    }
}

