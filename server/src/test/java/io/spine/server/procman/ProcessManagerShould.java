/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.RejectionEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.command.TestEventFactory;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.entity.given.Given;
import io.spine.server.entity.rejection.Rejections.EntityAlreadyArchived;
import io.spine.server.procman.given.ProcessManagerTestEnv.AddTaskDispatcher;
import io.spine.server.procman.given.ProcessManagerTestEnv.TestProcessManager;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantAwareTest;
import io.spine.server.tenant.TenantIndex;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testdata.Sample;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.spine.core.Commands.getMessage;
import static io.spine.core.Rejections.createRejection;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.procman.ProcessManagerDispatcher.dispatch;
import static io.spine.test.TestValues.newUuidValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Alexander Litus
 * @author Dmytro Dashenkov
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OverlyCoupledClass")
public class ProcessManagerShould {

    private static final ProjectId ID = Sample.messageOfType(ProjectId.class);

    private final TestEventFactory eventFactory =
            TestEventFactory.newInstance(Identifier.pack(ID), getClass());

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private CommandBus commandBus;

    private TestProcessManager processManager;

    @Before
    public void setUp() {
        final BoundedContext bc = BoundedContext.newBuilder()
                                                .setMultitenant(true)
                                                .build();
        final StorageFactory storageFactory = bc.getStorageFactory();
        final TenantIndex tenantIndex = TenantAwareTest.createTenantIndex(false, storageFactory);
        final CommandStore commandStore = spy(
                new CommandStore(storageFactory, tenantIndex)
        );

        commandBus = spy(CommandBus.newBuilder()
                                   .setCommandStore(commandStore)
                                   .build());
        processManager = Given.processManagerOfClass(TestProcessManager.class)
                              .withId(ID)
                              .withVersion(2)
                              .withState(Any.getDefaultInstance())
                              .build();
    }

    @Test
    public void have_default_state_initially() {
        assertEquals(processManager.getDefaultState(), processManager.getState());
    }

    @Test
    public void dispatch_event() {
        testDispatchEvent(Sample.messageOfType(PmProjectStarted.class));
    }

    @Test
    public void dispatch_several_events() {
        testDispatchEvent(Sample.messageOfType(PmProjectCreated.class));
        testDispatchEvent(Sample.messageOfType(PmTaskAdded.class));
        testDispatchEvent(Sample.messageOfType(PmProjectStarted.class));
    }

    private void testDispatchEvent(Message eventMessage) {
        final Event event = eventFactory.createEvent(eventMessage);
        dispatch(processManager, EventEnvelope.of(event));
        assertEquals(pack(eventMessage), processManager.getState());
    }

    @Test
    public void dispatch_command() {
        testDispatchCommand(addTask());
    }

    @Test
    public void dispatch_several_commands() {
        commandBus.register(new AddTaskDispatcher());
        processManager.injectCommandBus(commandBus);

        testDispatchCommand(createProject());
        testDispatchCommand(addTask());
        testDispatchCommand(startProject());
    }

    private List<Event> testDispatchCommand(Message commandMsg) {
        final CommandEnvelope envelope = CommandEnvelope.of(requestFactory.command()
                                                                          .create(commandMsg));
        final List<Event> events = dispatch(processManager, envelope);
        assertEquals(pack(commandMsg), processManager.getState());
        return events;
    }

    @Test
    public void dispatch_command_and_return_events() {
        final List<Event> events = testDispatchCommand(createProject());

        assertEquals(1, events.size());
        final Event event = events.get(0);
        assertNotNull(event);
        final PmProjectCreated message = unpack(event.getMessage());
        assertEquals(ID, message.getProjectId());
    }

    /**
     * Tests command routing.
     *
     * @see TestProcessManager#handle(PmStartProject, CommandContext)
     */
    @Test
    public void route_commands() {
        // Add dispatcher for the routed command. Otherwise the command would reject the command.
        final AddTaskDispatcher dispatcher = new AddTaskDispatcher();
        commandBus.register(dispatcher);
        processManager.injectCommandBus(commandBus);

        final List<Event> events = testDispatchCommand(startProject());

        // There's only one event generated.
        assertEquals(1, events.size());

        final Event event = events.get(0);

        // The producer of the event is our Process Manager.
        assertEquals(processManager.getId(), Events.getProducer(event.getContext()));

        final Message message = AnyPacker.unpack(event.getMessage());

        // The event type is CommandRouted.
        assertThat(message, instanceOf(CommandRouted.class));

        final CommandRouted commandRouted = (CommandRouted) message;

        // The source of the command is StartProject.
        assertThat(getMessage(commandRouted.getSource()), instanceOf(PmStartProject.class));
        final List<CommandEnvelope> dispatchedCommands = dispatcher.getCommands();
        assertSize(1, dispatchedCommands);
        final CommandEnvelope dispatchedCommand = dispatcher.getCommands()
                                                            .get(0);
        assertEquals(commandRouted.getProduced(0), dispatchedCommand.getCommand());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_dispatch_unknown_command() {
        final Int32Value unknownCommand = Int32Value.getDefaultInstance();

        final CommandEnvelope envelope = CommandEnvelope.of(
                requestFactory.command()
                              .create(unknownCommand)
        );
        processManager.dispatchCommand(envelope);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_dispatch_unknown_event() {
        final StringValue unknownEvent = StringValue.getDefaultInstance();
        final EventEnvelope envelope = EventEnvelope.of(eventFactory.createEvent(unknownEvent));
        dispatch(processManager, envelope);
    }

    @Test
    public void return_handled_event_classes() {
        final Set<EventClass> classes =
                ProcessManager.TypeInfo.getEventClasses(TestProcessManager.class);
        assertEquals(3, classes.size());
        assertContains(classes, PmProjectCreated.class);
        assertContains(classes, PmTaskAdded.class);
        assertContains(classes, PmProjectStarted.class);
    }

    @Test
    public void dispatch_rejection() {
        final EntityAlreadyArchived rejectionMessage =
                EntityAlreadyArchived.newBuilder()
                                     .setEntityId(getClass().getName())
                                     .build();

        final Command command = requestFactory.command()
                                              .create(newUuidValue());
        final RejectionEnvelope rejection = RejectionEnvelope.of(
                createRejection(rejectionMessage, command)
        );

        dispatch(processManager, rejection);

        assertEquals(rejection.getOuterObject()
                              .getMessage(), processManager.getState());
    }

    private static void assertContains(Collection<EventClass> eventClasses,
                                       Class<? extends Message> eventClass) {
        assertTrue(eventClasses.contains(EventClass.of(eventClass)));
    }

    @Test
    public void create_iterating_router() {
        final StringValue commandMessage = toMessage("create_iterating_router");
        final CommandContext commandContext = requestFactory.createCommandContext();

        processManager.injectCommandBus(mock(CommandBus.class));

        final IteratingCommandRouter router
                = processManager.newIteratingRouterFor(commandMessage,
                                                       commandContext);
        assertNotNull(router);

        assertEquals(commandMessage, getMessage(router.getSource()));
        assertEquals(commandContext, router.getSource()
                                           .getContext());
    }

    @Test(expected = IllegalStateException.class)
    public void require_command_bus_when_creating_router() {
        processManager.newRouterFor(StringValue.getDefaultInstance(),
                                    CommandContext.getDefaultInstance());
    }

    @Test
    public void create_router() {
        final StringValue commandMessage = toMessage("create_router");
        final CommandContext commandContext = requestFactory.createCommandContext();

        processManager.injectCommandBus(mock(CommandBus.class));

        final CommandRouter router = processManager.newRouterFor(commandMessage, commandContext);
        assertNotNull(router);

        assertEquals(commandMessage, getMessage(router.getSource()));
        assertEquals(commandContext, router.getSource()
                                           .getContext());
    }

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

    @Test
    public void have_TypeInfo_utility_class() {
        assertHasPrivateParameterlessCtor(ProcessManager.TypeInfo.class);
    }
}
