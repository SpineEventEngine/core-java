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

package org.spine3.server.aggregate;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Commands;
import org.spine3.base.Errors;
import org.spine3.base.Event;
import org.spine3.base.FailureThrowable;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.storage.AggregateEvents;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandStore;
import org.spine3.server.event.EventBus;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.testdata.Sample;

import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;
import static java.util.Collections.emptyIterator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Events.getMessage;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;

public class CommandEndpointShould {

    private AggregateRepository<ProjectId, CommandEndpointShould.ProjectAggregate> repository;

    /** Use spy only when it is required to avoid problems, make tests faster and make it easier to debug. */
    private AggregateRepository<ProjectId, CommandEndpointShould.ProjectAggregate> repositorySpy;
    private CommandStore commandStore;
    private EventBus eventBus;

    private final ProjectId projectId = Sample.messageOfType(ProjectId.class);

    @Before
    public void setUp() {
        eventBus = mock(EventBus.class);
        commandStore = mock(CommandStore.class);
        doReturn(emptyIterator()).when(commandStore)
                                 .iterator(any(CommandStatus.class)); // to avoid NPE
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .build();
        final BoundedContext boundedContext = newBoundedContext(commandBus, eventBus);
        repository = new ProjectAggregateRepository(boundedContext);
        repositorySpy = spy(repository);
    }

    @After
    public void tearDown() throws Exception {
        ProjectAggregate.clearCommandsHandled();
        repository.close();
    }

    @Test
    public void post_events_on_command_dispatching() {
        final Command cmd = Given.Command.createProject(projectId);

        repository.dispatch(cmd);

        final Event event = verifyEventPosted();
        final ProjectCreated msg = getMessage(event);
        assertEquals(projectId, msg.getProjectId());
    }

    @Test
    public void set_ok_command_status_on_command_dispatching() {
        final Command cmd = Given.Command.createProject();
        final CommandId commandId = getId(cmd);

        repository.dispatch(cmd);

        verify(commandStore).setCommandStatusOk(commandId);
    }

    @Test
    public void store_aggregate_on_command_dispatching() {
        final Command cmd = Given.Command.createProject(projectId);
        final CreateProject msg = Commands.getMessage(cmd);

        repositorySpy.dispatch(cmd);

        final ProjectAggregate aggregate = verifyAggregateStored(repositorySpy);
        assertEquals(projectId, aggregate.getId());
        assertEquals(msg.getName(), aggregate.getState()
                                             .getName());
    }

    @Test
    public void set_cmd_status_to_error_if_failed_to_store_aggregate_on_dispatching() {
        final Command cmd = Given.Command.createProject();
        final RuntimeException exception = new RuntimeException(newUuid());
        doThrow(exception).when(repositorySpy)
                          .store(any(ProjectAggregate.class));

        repositorySpy.dispatch(cmd);

        verify(commandStore).updateStatus(getId(cmd), exception);
    }

    @Test
    public void repeat_command_dispatching_if_event_count_is_changed_during_dispatching() {
        @SuppressWarnings("unchecked")
        final AggregateStorage<ProjectId> storage = mock(AggregateStorage.class);
        final Command cmd = Given.Command.createProject(projectId);

        // Change reported event count upon the second invocation and trigger re-dispatch.
        doReturn(0, 1).when(storage).readEventCountAfterLastSnapshot(projectId);
        doReturn(Optional.of(AggregateEvents.getDefaultInstance())).when(storage).read(projectId);
        doReturn(storage).when(repositorySpy).aggregateStorage();
        doReturn(Optional.absent()).when(storage).readStatus(projectId);

        repositorySpy.dispatch(cmd);

        // Load should be executed twice due to repeated dispatching.
        verify(repositorySpy, times(2)).loadOrCreate(projectId);

        // Reading event count is executed 2 times per dispatch (so 2 * 2) plus once upon storing the state.
        verify(storage, times(2 * 2 + 1)).readEventCountAfterLastSnapshot(projectId);
    }

    @Test
    public void dispatch_command() {
        assertDispatches(Given.Command.createProject());
    }

    @Test
    public void dispatch_several_commands() {
        assertDispatches(Given.Command.createProject(projectId));
        assertDispatches(Given.Command.addTask(projectId));
        assertDispatches(Given.Command.startProject(projectId));
    }

    @Test
    public void set_cmd_status_to_error_if_got_exception_on_dispatching() {
        final Exception exception = new Exception(newUuid());

        final CommandId id = dispatchCmdToAggregateThrowing(exception);

        verify(commandStore).updateStatus(id, exception);
    }

    @Test
    public void set_cmd_status_to_failure_if_got_failure_on_dispatching() {
        final TestFailure failure = new TestFailure();

        final CommandId id = dispatchCmdToAggregateThrowing(failure);

        verify(commandStore).updateStatus(id, failure.toMessage());
    }

    @Test
    public void set_cmd_status_to_error_if_got_unknown_throwable_on_dispatching() {
        final TestThrowable throwable = new TestThrowable();

        final CommandId id = dispatchCmdToAggregateThrowing(throwable);

        verify(commandStore).updateStatus(id, Errors.fromThrowable(throwable));
    }

    /*
     * Utility methods.
     ****************************/

    private Event verifyEventPosted() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).post(eventCaptor.capture());
        return eventCaptor.getValue();
    }

    private CommandId dispatchCmdToAggregateThrowing(Throwable throwable) {
        final Command cmd = Given.Command.createProject();
        givenThrowingAggregate(throwable, cmd, repositorySpy);
        repositorySpy.dispatch(cmd);
        final CommandId commandId = getId(cmd);
        return commandId;
    }

    private static void givenThrowingAggregate(
            Throwable cause,
            Command cmd,
            AggregateRepository<ProjectId, CommandEndpointShould.ProjectAggregate> repositorySpy) {
        final ProjectAggregate throwingAggregate = mock(ProjectAggregate.class);
        final Message msg = unpack(cmd.getMessage());
        final RuntimeException exception = new RuntimeException(cause);
        doThrow(exception).when(throwingAggregate).dispatch(msg, cmd.getContext());
        doReturn(throwingAggregate).when(repositorySpy).loadOrCreate(any(ProjectId.class));
    }

    private static ProjectAggregate verifyAggregateStored(AggregateRepository<ProjectId,
            CommandEndpointShould.ProjectAggregate> repository) {
        final ArgumentCaptor<CommandEndpointShould.ProjectAggregate> aggregateCaptor = ArgumentCaptor.forClass(ProjectAggregate.class);
        verify(repository).store(aggregateCaptor.capture());
        return aggregateCaptor.getValue();
    }

    private void assertDispatches(Command cmd) {
        repository.dispatch(cmd);
        ProjectAggregate.assertHandled(cmd);
    }

    /*
     * Test environment classes
     ****************************/

    private static class TestFailure extends FailureThrowable {
        private static final long serialVersionUID = 0L;

        private TestFailure() {
            super(StringValue.newBuilder()
                             .setValue(TestFailure.class.getName())
                             .build());
        }
    }

    private static class TestThrowable extends Throwable {
        private static final long serialVersionUID = 0L;
    }

    private static class ProjectAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        // Needs to be `static` to share the state updates in scope of the test.
        private static final Map<CommandId, Command> commandsHandled = newConcurrentMap();

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        ProjectCreated handle(CreateProject msg, CommandContext context) {
            final Command cmd = Commands.createCommand(msg, context);
            commandsHandled.put(context.getCommandId(), cmd);
            return ProjectCreated.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .setName(msg.getName())
                                 .build();
        }

        @Apply
        private void apply(ProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName(event.getName());
        }

        @Assign
        TaskAdded handle(AddTask msg, CommandContext context) {
            final Command cmd = Commands.createCommand(msg, context);
            commandsHandled.put(context.getCommandId(), cmd);
            return TaskAdded.newBuilder()
                            .setProjectId(msg.getProjectId())
                            .build();
        }

        @Apply
        private void apply(TaskAdded event) {
            getBuilder().setId(event.getProjectId());
        }

        @Assign
        ProjectStarted handle(StartProject msg, CommandContext context) {
            final Command cmd = Commands.createCommand(msg, context);
            commandsHandled.put(context.getCommandId(), cmd);
            return ProjectStarted.newBuilder()
                                 .setProjectId(msg.getProjectId())
                                 .build();
        }

        @Apply
        private void apply(ProjectStarted event) {
        }

        static void assertHandled(Command expected) {
            final CommandId id = Commands.getId(expected);
            final Command actual = commandsHandled.get(id);
            final String cmdName = Commands.getMessage(expected)
                                           .getClass()
                                           .getName();
            assertNotNull("No such command handled: " + cmdName, actual);
            assertEquals(expected, actual);
        }

        static void clearCommandsHandled() {
            commandsHandled.clear();
        }
    }

    private static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, CommandEndpointShould.ProjectAggregate> {
        protected ProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
            initStorage(InMemoryStorageFactory.getInstance());
        }
    }
}
