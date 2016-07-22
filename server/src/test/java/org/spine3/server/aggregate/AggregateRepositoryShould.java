/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.matchers.GreaterThan;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Commands;
import org.spine3.base.Errors;
import org.spine3.base.Event;
import org.spine3.base.FailureThrowable;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandStore;
import org.spine3.server.event.EventBus;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.type.CommandClass;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.TaskAdded;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyIterator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Events.getMessage;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.validate.Validate.isDefault;

@SuppressWarnings("InstanceMethodNamingConvention")
public class AggregateRepositoryShould {

    private AggregateRepository<ProjectId, ProjectAggregate> repository;

    /** Use spy only when it is required to avoid problems. */
    private AggregateRepository<ProjectId, ProjectAggregate> repositorySpy;

    private CommandStore commandStore;
    private EventBus eventBus;

    @Before
    public void setUp() {
        eventBus = mock(EventBus.class);
        commandStore = mock(CommandStore.class);
        doReturn(emptyIterator()).when(commandStore).iterator(any(CommandStatus.class)); // to avoid NPE
        final CommandBus commandBus = CommandBus.newInstance(commandStore);
        final BoundedContext boundedContext = newBoundedContext(commandBus, eventBus);
        repository = new TestAggregateRepository(boundedContext);
        repositorySpy = spy(repository);
        ProjectAggregate.clearCommandsHandled();
    }

    @Test
    public void return_default_aggregate_state_if_no_aggregate_found() {
        final ProjectAggregate aggregate = repository.load(Given.AggregateId.newProjectId());
        final Project state = aggregate.getState();

        assertTrue(isDefault(state));
    }

    @Test
    public void store_and_load_aggregate() {
        final ProjectId id = Given.AggregateId.newProjectId();
        final ProjectAggregate expected = givenAggregateWithAppliedEvents(id);

        repository.store(expected);
        final ProjectAggregate actual = repository.load(id);

        assertEquals(expected.getState(), actual.getState());
        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    public void store_snapshot_if_needed() {
        @SuppressWarnings("unchecked")
        final AggregateStorage<ProjectId> storage = mock(AggregateStorage.class);
        doReturn(storage).when(repositorySpy).aggregateStorage();

        final ProjectAggregate aggregate = givenAggregateWithAppliedEvents();
        repositorySpy.setSnapshotTrigger(1);
        repositorySpy.store(aggregate);

        verify(storage).write(any(ProjectId.class), any(Snapshot.class));
        verify(storage).writeEventCountAfterLastSnapshot(any(ProjectId.class), eq(0));
    }

    @Test
    public void not_store_snapshot_if_not_needed() {
        @SuppressWarnings("unchecked")
        final AggregateStorage<ProjectId> storage = mock(AggregateStorage.class);
        doReturn(storage).when(repositorySpy).aggregateStorage();// TODO:2016-07-22:alexander.litus: DRY

        final ProjectAggregate aggregate = givenAggregateWithAppliedEvents();
        repositorySpy.store(aggregate);

        verify(storage, never()).write(any(ProjectId.class), any(Snapshot.class));
        verify(storage).writeEventCountAfterLastSnapshot(any(ProjectId.class), intThat(new GreaterThan<>(0)));
    }

    @Test
    public void dispatch_command() {
        final Command cmd = Given.Command.createProject();

        repository.dispatch(cmd);

        final Command cmdHandled = ProjectAggregate.getCommandHandled(getId(cmd));
        assertEquals(cmd, cmdHandled);
    }

    @Test
    public void post_events_on_command_dispatching() {
        final ProjectId id = Given.AggregateId.newProjectId();
        final Command cmd = Given.Command.createProject(id);

        repository.dispatch(cmd);

        final Event event = verifyEventPosted();
        final ProjectCreated msg = getMessage(event);
        assertEquals(id, msg.getProjectId());
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
        final ProjectId id = Given.AggregateId.newProjectId();
        final Command cmd = Given.Command.createProject(id);

        repositorySpy.dispatch(cmd);

        final ProjectAggregate aggregate = verifyAggregateStored(repositorySpy);
        assertEquals(id, aggregate.getId());
        assertEquals(ProjectAggregate.NAME, aggregate.getState()
                                                     .getName());
    }

    @Test
    public void set_cmd_status_to_error_if_failed_to_store_aggregate_on_dispatching() {
        final ProjectId id = Given.AggregateId.newProjectId();
        final Command cmd = Given.Command.createProject(id);
        final RuntimeException exception = new RuntimeException(newUuid());
        doThrow(exception).when(repositorySpy)
                          .store(any(ProjectAggregate.class));

        repositorySpy.dispatch(cmd);

        verify(commandStore).updateStatus(getId(cmd), exception);
    }

    @Test
    public void set_cmd_status_to_error_if_got_exception_on_dispatching() {
        final Command cmd = Given.Command.createProject(); // TODO:2016-07-22:alexander.litus: DRY
        final CommandId commandId = getId(cmd);
        final Exception exception = new Exception(newUuid());
        givenThrowingAggregate(exception, cmd);

        repositorySpy.dispatch(cmd);

        verify(commandStore).updateStatus(commandId, exception);
    }

    @Test
    public void set_cmd_status_to_failure_if_got_failure_on_dispatching() {
        final Command cmd = Given.Command.createProject();
        final CommandId commandId = getId(cmd);
        final TestFailure failure = new TestFailure();
        givenThrowingAggregate(failure, cmd);

        repositorySpy.dispatch(cmd);

        verify(commandStore).updateStatus(commandId, failure.toMessage());
    }

    @Test
    public void set_cmd_status_to_error_if_got_unknown_throwable_on_dispatching() {
        final Command cmd = Given.Command.createProject();
        final CommandId commandId = getId(cmd);
        final TestThrowable throwable = new TestThrowable();
        givenThrowingAggregate(throwable, cmd);

        repositorySpy.dispatch(cmd);

        verify(commandStore).updateStatus(commandId, Errors.fromThrowable(throwable));
    }

    @Test
    public void return_aggregate_class() {
        assertEquals(ProjectAggregate.class, repository.getAggregateClass());
    }

    @Test
    public void have_default_value_for_snapshot_trigger() {
        assertEquals(AggregateRepository.DEFAULT_SNAPSHOT_TRIGGER, repository.getSnapshotTrigger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_negative_snapshot_trigger() {
        repository.setSnapshotTrigger(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_zero_snapshot_trigger() {
        repository.setSnapshotTrigger(0);
    }

    @Test
    public void allow_to_change_snapshot_trigger() {
        final int newSnapshotTrigger = 1000;
        repository.setSnapshotTrigger(newSnapshotTrigger);
        assertEquals(newSnapshotTrigger, repository.getSnapshotTrigger());
    }

    @Test
    public void expose_classes_of_commands_of_its_aggregate() {
        final Set<CommandClass> aggregateCommands = CommandClass.setOf(Aggregate.getCommandClasses(ProjectAggregate.class));
        final Set<CommandClass> exposedByRepository = repository.getCommandClasses();

        assertTrue(exposedByRepository.containsAll(aggregateCommands));
    }

    private static ProjectAggregate givenAggregateWithAppliedEvents() {
        return givenAggregateWithAppliedEvents(Given.AggregateId.newProjectId());
    }

    private static ProjectAggregate givenAggregateWithAppliedEvents(ProjectId id) {
        final ProjectAggregate aggregate = new ProjectAggregate(id);
        aggregate.dispatchForTest(Given.CommandMessage.createProject(id), createCommandContext());
        aggregate.dispatchForTest(Given.CommandMessage.addTask(id), createCommandContext());
        return aggregate;
    }

    private void givenThrowingAggregate(Throwable cause, Command cmd) {
        final ProjectAggregate throwingAggregate = mock(ProjectAggregate.class);
        final Message msg = unpack(cmd.getMessage());
        final RuntimeException wrapped = new RuntimeException(cause);
        doThrow(wrapped).when(throwingAggregate).dispatch(msg, cmd.getContext());
        doReturn(throwingAggregate).when(repositorySpy).load(any(ProjectId.class));
    }

    private Event verifyEventPosted() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).post(eventCaptor.capture());
        return eventCaptor.getValue();
    }

    private static ProjectAggregate verifyAggregateStored(AggregateRepository<ProjectId, ProjectAggregate> repository) {
        final ArgumentCaptor<ProjectAggregate> aggregateCaptor = ArgumentCaptor.forClass(ProjectAggregate.class);
        verify(repository).store(aggregateCaptor.capture());
        return aggregateCaptor.getValue();
    }

    private static class TestAggregateRepository extends AggregateRepository<ProjectId, ProjectAggregate> {
        protected TestAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
            initStorage(InMemoryStorageFactory.getInstance());
        }
    }

    private static class ProjectAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        // needs to be static
        private static final Map<CommandId, Command> commandsHandled = newHashMap();

        private static final String NAME = "TestProject123";

        public ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        public ProjectCreated handle(CreateProject msg, CommandContext context) {
            final Command command = Commands.create(msg, context);
            commandsHandled.put(context.getCommandId(), command);
            return Given.EventMessage.projectCreated(msg.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext context) {
            return Given.EventMessage.taskAdded(cmd.getProjectId());
        }

        @Apply
        private void apply(ProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setName(NAME);
        }

        @Apply
        private void apply(TaskAdded event) {
            getBuilder().setId(event.getProjectId())
                        .addTask(event.getTask());
        }

        /* package */ static Command getCommandHandled(CommandId id) {
            return commandsHandled.get(id);
        }

        /* package */ static void clearCommandsHandled() {
            commandsHandled.clear();
        }
    }

    @SuppressWarnings("serial")
    private static class TestFailure extends FailureThrowable {
        private TestFailure() {
            super(StringValue.newBuilder()
                             .setValue(TestFailure.class.getName())
                             .build());
        }
    }

    @SuppressWarnings("serial")
    private static class TestThrowable extends Throwable {
    }
}
