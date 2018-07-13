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

package io.spine.server.procman;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.core.Rejections;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.command.TestEventFactory;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.entity.given.Given;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.server.model.ModelTests;
import io.spine.server.procman.given.ProcessManagerTestEnv.AddTaskDispatcher;
import io.spine.server.procman.given.ProcessManagerTestEnv.TestProcessManager;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantAwareTest;
import io.spine.server.tenant.TenantIndex;
import io.spine.system.server.NoOpSystemGateway;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.core.Commands.getMessage;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.commandbus.Given.ACommand;
import static io.spine.server.procman.ProcessManagerDispatcher.dispatch;
import static io.spine.test.Verify.assertSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Alexander Litus
 * @author Dmytro Dashenkov
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"OverlyCoupledClass",
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("ProcessManager should")
class ProcessManagerTest {

    private static final ProjectId ID = Sample.messageOfType(ProjectId.class);

    private final TestEventFactory eventFactory =
            TestEventFactory.newInstance(Identifier.pack(ID), getClass());
    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private CommandBus commandBus;
    private TestProcessManager processManager;

    private static PmCreateProject createProject() {
        return ((PmCreateProject.Builder) Sample.builderForType(PmCreateProject.class))
                .setProjectId(ID)
                .build();
    }

    private static PmStartProject startProject() {
        return ((PmStartProject.Builder) Sample.builderForType(PmStartProject.class))
                .setProjectId(ID)
                .build();
    }

    private static PmAddTask addTask() {
        return ((PmAddTask.Builder) Sample.builderForType(PmAddTask.class))
                .setProjectId(ID)
                .build();
    }

    private static RejectionEnvelope entityAlreadyArchived(
            Class<? extends Message> commandMessageCls) {
        Any id = Identifier.pack(ProcessManagerTest.class.getName());
        EntityAlreadyArchived rejectionMessage = EntityAlreadyArchived.newBuilder()
                                                                      .setEntityId(id)
                                                                      .build();
        Command command = ACommand.withMessage(Sample.messageOfType(commandMessageCls));
        Rejection rejection = Rejections.createRejection(rejectionMessage, command);
        return RejectionEnvelope.of(rejection);
    }

    @BeforeEach
    void setUp() {
        ModelTests.clearModel();
        BoundedContext bc = BoundedContext.newBuilder()
                                          .setMultitenant(true)
                                          .build();
        StorageFactory storageFactory = bc.getStorageFactory();
        TenantIndex tenantIndex = TenantAwareTest.createTenantIndex(false, storageFactory);
        CommandStore commandStore = spy(
                new CommandStore(storageFactory, tenantIndex)
        );

        commandBus = spy(CommandBus.newBuilder()
                                   .setCommandStore(commandStore)
                                   .injectSystemGateway(NoOpSystemGateway.INSTANCE)
                                   .build());
        processManager = Given.processManagerOfClass(TestProcessManager.class)
                              .withId(ID)
                              .withVersion(2)
                              .withState(Any.getDefaultInstance())
                              .build();
    }

    @CanIgnoreReturnValue
    private List<? extends Message> testDispatchEvent(Message eventMessage) {
        Event event = eventFactory.createEvent(eventMessage);
        List<Event> result = dispatch(processManager, EventEnvelope.of(event));
        assertEquals(pack(eventMessage), processManager.getState());
        return result;
    }

    @CanIgnoreReturnValue
    private List<Event> testDispatchCommand(Message commandMsg) {
        CommandEnvelope envelope = CommandEnvelope.of(requestFactory.command()
                                                                    .create(commandMsg));
        List<Event> events = dispatch(processManager, envelope);
        assertEquals(pack(commandMsg), processManager.getState());
        return events;
    }

    @Test
    @DisplayName("have default state initially")
    void haveDefaultStateInitially() {
        assertEquals(processManager.getDefaultState(), processManager.getState());
    }

    @Nested
    @DisplayName("dispatch")
    class Dispatch {

        @Test
        @DisplayName("command")
        void command() {
            testDispatchCommand(addTask());
        }

        @Test
        @DisplayName("event")
        void event() {
            List<? extends Message> eventMessages =
                    testDispatchEvent(Sample.messageOfType(PmProjectStarted.class));

            assertEquals(1, eventMessages.size());
            assertTrue(eventMessages.get(0) instanceof Event);
        }
    }

    @Test
    @DisplayName("dispatch command and return events")
    void dispatchCommandAndReturnEvents() {
        List<Event> events = testDispatchCommand(createProject());

        assertEquals(1, events.size());
        Event event = events.get(0);
        assertNotNull(event);
        PmProjectCreated message = unpack(event.getMessage());
        assertEquals(ID, message.getProjectId());
    }

    @Nested
    @DisplayName("dispatch rejection by")
    class DispatchRejectionBy {

        @Test
        @DisplayName("rejection message only")
        void rejectionMessage() {
            RejectionEnvelope rejection = entityAlreadyArchived(StringValue.class);
            dispatch(processManager, rejection);
            assertEquals(rejection.getOuterObject()
                                  .getMessage(), processManager.getState());
        }

        @Test
        @DisplayName("rejection and command message")
        void rejectionAndCommandMessage() {
            RejectionEnvelope rejection = entityAlreadyArchived(PmAddTask.class);
            dispatch(processManager, rejection);
            assertEquals(AnyPacker.pack(rejection.getCommandMessage()), processManager.getState());
        }
    }

    @Nested
    @DisplayName("dispatch several")
    class DispatchSeveral {

        @Test
        @DisplayName("commands")
        void commands() {
            commandBus.register(new AddTaskDispatcher());
            processManager.injectCommandBus(commandBus);

            testDispatchCommand(createProject());
            testDispatchCommand(addTask());
            testDispatchCommand(startProject());
        }

        @Test
        @DisplayName("events")
        void events() {
            testDispatchEvent(Sample.messageOfType(PmProjectCreated.class));
            testDispatchEvent(Sample.messageOfType(PmTaskAdded.class));
            testDispatchEvent(Sample.messageOfType(PmProjectStarted.class));
        }
    }

    /**
     * Tests command routing.
     *
     * @see TestProcessManager#handle(PmStartProject, CommandContext)
     */
    @Test
    @DisplayName("route commands")
    void routeCommands() {
        // Add dispatcher for the routed command. Otherwise the command would reject the command.
        AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);
        processManager.injectCommandBus(commandBus);

        List<Event> events = testDispatchCommand(startProject());

        // There's only one event generated.
        assertEquals(1, events.size());

        Event event = events.get(0);

        // The producer of the event is our Process Manager.
        assertEquals(processManager.getId(), Events.getProducer(event.getContext()));

        Message message = AnyPacker.unpack(event.getMessage());

        // The event type is CommandRouted.
        assertThat(message, instanceOf(CommandRouted.class));

        CommandRouted commandRouted = (CommandRouted) message;

        // The source of the command is StartProject.
        assertThat(getMessage(commandRouted.getSource()), instanceOf(PmStartProject.class));
        List<CommandEnvelope> dispatchedCommands = dispatcher.getCommands();
        assertSize(1, dispatchedCommands);
        CommandEnvelope dispatchedCommand = dispatcher.getCommands()
                                                      .get(0);
        assertEquals(commandRouted.getProduced(0), dispatchedCommand.getCommand());
    }

    @Nested
    @DisplayName("throw ISE when dispatching unknown")
    class ThrowOnUnknown {

        @Test
        @DisplayName("command")
        void command() {
            Int32Value unknownCommand = Int32Value.getDefaultInstance();

            CommandEnvelope envelope = CommandEnvelope.of(
                    requestFactory.createCommand(unknownCommand)
            );

            assertThrows(IllegalStateException.class,
                         () -> processManager.dispatchCommand(envelope));
        }

        @Test
        @DisplayName("event")
        void event() {
            StringValue unknownEvent = StringValue.getDefaultInstance();
            EventEnvelope envelope = EventEnvelope.of(eventFactory.createEvent(unknownEvent));

            assertThrows(IllegalStateException.class, () -> dispatch(processManager, envelope));
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("CommandRouter")
        void commandRouter() {
            StringValue commandMessage = toMessage("create_router");
            CommandContext commandContext = requestFactory.createCommandContext();

            processManager.injectCommandBus(mock(CommandBus.class));

            CommandRouter router = processManager.newRouterFor(commandMessage, commandContext);
            assertNotNull(router);

            assertEquals(commandMessage, getMessage(router.getSource()));
            assertEquals(commandContext, router.getSource()
                                               .getContext());
        }

        @Test
        @DisplayName("IteratingCommandRouter")
        void iteratingCommandRouter() {
            StringValue commandMessage = toMessage("create_iterating_router");
            CommandContext commandContext = requestFactory.createCommandContext();

            processManager.injectCommandBus(mock(CommandBus.class));

            IteratingCommandRouter router
                    = processManager.newIteratingRouterFor(commandMessage,
                                                           commandContext);
            assertNotNull(router);

            assertEquals(commandMessage, getMessage(router.getSource()));
            assertEquals(commandContext, router.getSource()
                                               .getContext());
        }
    }

    @Test
    @DisplayName("require command bus when creating router")
    void requireCommandBusForRouter() {
        assertThrows(IllegalStateException.class,
                     () -> processManager.newRouterFor(StringValue.getDefaultInstance(),
                                                       CommandContext.getDefaultInstance()));
    }
}
