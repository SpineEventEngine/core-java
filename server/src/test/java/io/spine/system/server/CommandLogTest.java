/*
 * Copyright 2022, TeamDev. All rights reserved.
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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.CommandMessage;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.core.ActorContext;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.MessageId;
import io.spine.core.UserId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.DefaultRepository;
import io.spine.server.commandbus.CommandBus;
import io.spine.system.server.event.CommandAcknowledged;
import io.spine.system.server.event.CommandDispatched;
import io.spine.system.server.event.CommandErrored;
import io.spine.system.server.event.CommandHandled;
import io.spine.system.server.event.CommandReceived;
import io.spine.system.server.event.CommandRejected;
import io.spine.system.server.event.TargetAssignedToCommand;
import io.spine.system.server.given.command.CommandLifecycleWatcher;
import io.spine.system.server.given.command.CompanyAggregate;
import io.spine.system.server.given.command.CompanyNameProcman;
import io.spine.system.server.given.command.CompanyNameProcmanRepo;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.core.given.GivenUserId;
import io.spine.testing.logging.MuteLogging;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.Messages.isNotDefault;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.system.server.given.command.CompanyNameProcman.FAULTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CommandLog should")
class CommandLogTest {

    private static final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(EntityEventsTest.class);

    private BoundedContext context;
    private BoundedContext system;

    @BeforeEach
    void setUp() {
        BoundedContextName contextName = BoundedContextName
                .newBuilder()
                .setValue(EntityEventsTest.class.getSimpleName())
                .build();
        context = BoundedContext
                .singleTenant(contextName.getValue())
                .build();
        system = systemOf(context);

        BoundedContext.InternalAccess ctx = context.internalAccess();
        ctx.register(DefaultRepository.of(CompanyAggregate.class));
        ctx.register(new CompanyNameProcmanRepo());
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @DisplayName("produce system events when command")
    @Nested
    class ProduceEvents {

        private CommandLifecycleWatcher eventAccumulator;
        private CompanyId id;

        @BeforeEach
        void setUp() {
            this.eventAccumulator = new CommandLifecycleWatcher();
            system.eventBus().register(eventAccumulator);
            id = CompanyId.generate();
        }

        @Test
        @DisplayName("is processed successfully")
        void successful() {
            EstablishCompany successfulCommand = EstablishCompany
                    .newBuilder()
                    .setId(id)
                    .setFinalName("Good company name")
                    .build();
            CommandId commandId = postCommand(successfulCommand);
            checkReceived(successfulCommand);
            checkAcknowledged(commandId);
            checkDispatched(commandId);
            checkTargetAssigned(commandId, CompanyAggregate.TYPE);
            checkHandled(commandId);
        }

        @Test
        @DisplayName("is filtered out")
        void invalid() {
            Command invalidCommand = buildInvalidCommand();
            CommandId actualId = postBuiltCommand(invalidCommand);

            checkReceived(unpack(invalidCommand.getMessage()));
            CommandId expectedId = invalidCommand.getId();
            assertEquals(expectedId, actualId);

            Error error = checkErrored(actualId);
            assertTrue(isNotDefault(error.getValidationError()));
        }

        @Test
        @DisplayName("is rejected by handler")
        void rejected() {
            EstablishCompany rejectedCommand = EstablishCompany
                    .newBuilder()
                    .setId(id)
                    .setFinalName(CompanyAggregate.TAKEN_NAME)
                    .build();
            CommandId commandId = postCommand(rejectedCommand);
            checkReceived(rejectedCommand);

            eventAccumulator.assertReceivedEvent(CommandAcknowledged.class);
            eventAccumulator.assertReceivedEvent(CommandDispatched.class);
            eventAccumulator.assertReceivedEvent(TargetAssignedToCommand.class);

            checkRejected(commandId, Rejections.CompanyNameAlreadyTaken.class);
        }

        @Test
        @DisplayName("causes a runtime exception")
        @MuteLogging
        void errored() {
            StartCompanyEstablishing start = StartCompanyEstablishing
                    .newBuilder()
                    .setId(id)
                    .build();
            CommandId startId = postCommand(start);

            eventAccumulator.assertEventCount(5);

            checkReceived(start);
            checkAcknowledged(startId);
            checkDispatched(startId);
            checkTargetAssigned(startId, CompanyNameProcman.TYPE);
            checkHandled(startId);

            eventAccumulator.forgetEvents();

            ProposeCompanyName propose = ProposeCompanyName
                    .newBuilder()
                    .setId(id)
                    .setName(FAULTY_NAME)
                    .build();
            CommandId proposeId = postCommand(propose);

            eventAccumulator.assertEventCount(5);

            checkReceived(propose);
            checkAcknowledged(proposeId);
            checkDispatched(proposeId);
            checkTargetAssigned(proposeId, CompanyNameProcman.TYPE);
            Error error = checkHandlerFailed(proposeId);
            assertThat(error.getType())
                    .isEqualTo(IllegalArgumentException.class.getCanonicalName());
        }

        private Command buildInvalidCommand() {
            EstablishCompany invalidCommand = EstablishCompany.getDefaultInstance();
            UserId actor = GivenUserId.newUuid();
            Timestamp now = Time.currentTime();
            ActorContext actorContext = ActorContext
                    .newBuilder()
                    .setTimestamp(now)
                    .setActor(actor)
                    .build();
            CommandContext context = CommandContext
                    .newBuilder()
                    .setActorContext(actorContext)
                    .build();
            Command command = Command
                    .newBuilder()
                    .setId(CommandId.generate())
                    .setMessage(AnyPacker.pack(invalidCommand))
                    .setContext(context)
                    .build();
            return command;
        }

        private void checkReceived(Message expectedCommand) {
            CommandReceived received = eventAccumulator.assertReceivedEvent(CommandReceived.class);
            Message actualCommand = unpack(received.getPayload().getMessage());
            assertEquals(expectedCommand, actualCommand);
        }

        private void checkAcknowledged(CommandId commandId) {
            CommandAcknowledged acknowledged = eventAccumulator.assertReceivedEvent(CommandAcknowledged.class);
            assertEquals(commandId, acknowledged.getId());
        }

        private void checkDispatched(CommandId commandId) {
            CommandDispatched dispatched = eventAccumulator.assertReceivedEvent(CommandDispatched.class);
            assertEquals(commandId, dispatched.getId());
        }

        private void checkTargetAssigned(CommandId commandId, TypeUrl entityType) {
            TargetAssignedToCommand assigned =
                    eventAccumulator.assertReceivedEvent(TargetAssignedToCommand.class);
            CommandTarget target = assigned.getTarget();
            Any actualId = target.getEntityId().getId();
            assertEquals(commandId, assigned.getId());
            assertEquals(id, Identifier.unpack(actualId));
            assertEquals(entityType.value(), target.getTypeUrl());
        }

        private void checkHandled(CommandId commandId) {
            CommandHandled handled = eventAccumulator.assertReceivedEvent(CommandHandled.class);
            assertEquals(commandId, handled.getId());
        }

        @CanIgnoreReturnValue
        private Error checkErrored(CommandId commandId) {
            CommandErrored errored = eventAccumulator.assertReceivedEvent(CommandErrored.class);
            assertEquals(commandId, errored.getId());
            return errored.getError();
        }

        @CanIgnoreReturnValue
        private Error checkHandlerFailed(CommandId commandId) {
            HandlerFailedUnexpectedly errored =
                    eventAccumulator.assertReceivedEvent(HandlerFailedUnexpectedly.class);
            MessageId signalId = errored.getHandledSignal();
            assertTrue(signalId.isCommand());
            assertEquals(commandId, signalId.asCommandId());
            return errored.getError();
        }

        private void checkRejected(CommandId commandId,
                                   Class<? extends Message> expectedRejectionClass) {
            CommandRejected rejected = eventAccumulator.assertReceivedEvent(CommandRejected.class);
            assertEquals(commandId, rejected.getId());
            Event rejectionEvent = rejected.getRejectionEvent();
            TypeUrl rejectionType = rejectionEvent.enclosedTypeUrl();
            TypeUrl expectedType = TypeUrl.of(expectedRejectionClass);
            assertEquals(expectedType, rejectionType);
        }

        private CommandId postCommand(CommandMessage commandMessage) {
            Command command = requestFactory.createCommand(commandMessage);
            return postBuiltCommand(command);
        }

        private CommandId postBuiltCommand(Command command) {
            CommandBus commandBus = context.commandBus();
            commandBus.post(command, noOpObserver());
            return command.getId();
        }
    }
}
