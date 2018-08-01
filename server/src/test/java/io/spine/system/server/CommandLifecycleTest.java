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
import com.google.protobuf.Timestamp;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.core.ActorContext;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.core.Rejection;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.system.server.given.CommandLifecycleTestEnv.CommandLifecycleWatcher;
import io.spine.system.server.given.CommandLifecycleTestEnv.TestAggregate;
import io.spine.system.server.given.CommandLifecycleTestEnv.TestAggregateRepository;
import io.spine.system.server.given.CommandLifecycleTestEnv.TestProcman;
import io.spine.system.server.given.CommandLifecycleTestEnv.TestProcmanRepository;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.storage.memory.InMemoryStorageFactory.newInstance;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static io.spine.system.server.given.CommandLifecycleTestEnv.TestProcman.FAULTY_NAME;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("CommandLifecycle should")
class CommandLifecycleTest {

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
        context.register(new TestProcmanRepository());
    }

    @DisplayName("produce system events when command")
    @Nested
    class ProduceEvents {

        private CommandLifecycleWatcher eventAccumulator;
        private CompanyId id;

        @BeforeEach
        void setUp() {
            this.eventAccumulator = new CommandLifecycleWatcher();
            system.getEventBus().register(eventAccumulator);
            id = CompanyId.newBuilder()
                          .setUuid(newUuid())
                          .build();
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
            checkTargetAssigned(commandId, TestAggregate.TYPE);
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
                    .setFinalName(TestAggregate.TAKEN_NAME)
                    .build();
            CommandId commandId = postCommand(rejectedCommand);
            checkReceived(rejectedCommand);

            eventAccumulator.nextEvent(CommandAcknowledged.class);
            eventAccumulator.nextEvent(CommandDispatched.class);
            eventAccumulator.nextEvent(TargetAssignedToCommand.class);

            checkRejected(commandId, Rejections.CompanyNameAlreadyTaken.class);
        }

        @Test
        @DisplayName("causes a runtime exception")
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
            checkTargetAssigned(startId, TestProcman.TYPE);
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
            checkTargetAssigned(proposeId, TestProcman.TYPE);
            checkErrored(proposeId);
        }

        private Command buildInvalidCommand() {
            EstablishCompany invalidCommand = EstablishCompany.getDefaultInstance();
            CommandId commandId = CommandId
                    .newBuilder()
                    .setUuid(newUuid())
                    .build();
            UserId actor = UserId
                    .newBuilder()
                    .setValue(newUuid())
                    .build();
            Timestamp now = Time.getCurrentTime();
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
                    .setId(commandId)
                    .setMessage(pack(invalidCommand))
                    .setContext(context)
                    .build();
            return command;
        }

        private void checkReceived(Message expectedCommand) {
            CommandReceived received = eventAccumulator.nextEvent(CommandReceived.class);
            Message actualCommand = unpack(received.getPayload().getMessage());
            assertEquals(expectedCommand, actualCommand);
        }

        private void checkAcknowledged(CommandId commandId) {
            CommandAcknowledged acknowledged = eventAccumulator.nextEvent(CommandAcknowledged.class);
            assertEquals(commandId, acknowledged.getId());
        }

        private void checkDispatched(CommandId commandId) {
            CommandDispatched dispatched = eventAccumulator.nextEvent(CommandDispatched.class);
            assertEquals(commandId, dispatched.getId());
        }

        private void checkTargetAssigned(CommandId commandId, TypeUrl entityType) {
            TargetAssignedToCommand assigned =
                    eventAccumulator.nextEvent(TargetAssignedToCommand.class);
            CommandTarget target = assigned.getTarget();
            Any actualId = target.getEntityId().getId();
            assertEquals(commandId, assigned.getId());
            assertEquals(id, Identifier.unpack(actualId));
            assertEquals(entityType.value(), target.getTypeUrl());
        }

        private void checkHandled(CommandId commandId) {
            CommandHandled handled = eventAccumulator.nextEvent(CommandHandled.class);
            assertEquals(commandId, handled.getId());
        }

        @CanIgnoreReturnValue
        private Error checkErrored(CommandId commandId) {
            CommandErrored errored = eventAccumulator.nextEvent(CommandErrored.class);
            assertEquals(commandId, errored.getId());
            return errored.getError();
        }

        private void checkRejected(CommandId commandId,
                                   Class<? extends Message> expectedRejectionClass) {
            CommandRejected rejected = eventAccumulator.nextEvent(CommandRejected.class);
            assertEquals(commandId, rejected.getId());
            Rejection rejection = rejected.getRejection();
            TypeUrl rejectionType = TypeUrl.ofEnclosed(rejection.getMessage());
            TypeUrl expectedType = TypeUrl.of(expectedRejectionClass);
            assertEquals(expectedType, rejectionType);
        }
    }

    private CommandId postCommand(Message commandMessage) {
        Command command = requestFactory.createCommand(commandMessage);
        return postBuiltCommand(command);
    }

    private CommandId postBuiltCommand(Command command) {
        CommandBus commandBus = context.getCommandBus();
        commandBus.post(command, noOpObserver());
        return command.getId();
    }
}
