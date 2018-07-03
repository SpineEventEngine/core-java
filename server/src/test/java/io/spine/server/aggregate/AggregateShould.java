/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.Time;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.Given;
import io.spine.server.aggregate.given.aggregate.AggregateWithMissingApplier;
import io.spine.server.aggregate.given.aggregate.AmishAggregate;
import io.spine.server.aggregate.given.aggregate.FaultyAggregate;
import io.spine.server.aggregate.given.aggregate.IntAggregate;
import io.spine.server.aggregate.given.aggregate.TaskAggregate;
import io.spine.server.aggregate.given.aggregate.TaskAggregateRepository;
import io.spine.server.aggregate.given.aggregate.TestAggregate;
import io.spine.server.aggregate.given.aggregate.TestAggregateRepository;
import io.spine.server.aggregate.given.aggregate.UserAggregate;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.entity.InvalidEntityStateException;
import io.spine.server.integration.BlackBoxBoundedContext;
import io.spine.server.model.Model;
import io.spine.server.model.ModelTests;
import io.spine.test.TimeTests;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggAssignTask;
import io.spine.test.aggregate.command.AggCancelProject;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggPauseProject;
import io.spine.test.aggregate.command.AggReassignTask;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.command.ImportEvents;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.test.aggregate.event.AggTaskAssigned;
import io.spine.test.aggregate.event.AggUserNotified;
import io.spine.test.aggregate.rejection.Rejections;
import io.spine.test.aggregate.user.User;
import io.spine.validate.ConstraintViolation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.Events.getRootCommandId;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.TestCommandClasses.assertContains;
import static io.spine.server.TestEventClasses.assertContains;
import static io.spine.server.TestEventClasses.getEventClasses;
import static io.spine.server.aggregate.AggregateMessageDispatcher.dispatchCommand;
import static io.spine.server.aggregate.AggregateMessageDispatcher.dispatchRejection;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static io.spine.server.aggregate.given.Given.EventMessage.projectStarted;
import static io.spine.server.aggregate.given.Given.EventMessage.taskAdded;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.assignTask;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.cannotModifyDeletedEntity;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.command;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.createTask;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.env;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.event;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.newTenantId;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.reassignTask;
import static io.spine.server.entity.given.Given.aggregateOfClass;
import static io.spine.server.integration.AckedCommandsVerifier.acked;
import static io.spine.server.integration.EmittedEventsVerifier.emitted;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukkov
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class AggregateShould {

    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("prj-01")
                                                 .build();

    private static final AggCreateProject createProject = Given.CommandMessage.createProject(ID);
    private static final AggPauseProject pauseProject = Given.CommandMessage.pauseProject(ID);
    private static final AggCancelProject cancelProject = Given.CommandMessage.cancelProject(ID);
    private static final AggAddTask addTask = Given.CommandMessage.addTask(ID);
    private static final AggStartProject startProject = Given.CommandMessage.startProject(ID);

    private TestAggregate aggregate;
    private AmishAggregate amishAggregate;
    private BoundedContext boundedContext;
    private TestAggregateRepository repository;

    private static TestAggregate newAggregate(ProjectId id) {
        final TestAggregate result = new TestAggregate(id);
        result.init();
        return result;
    }

    private static AmishAggregate newAmishAggregate(ProjectId id) {
        final AmishAggregate result = new AmishAggregate(id);
        result.init();
        return result;
    }

    private static void failNotThrows() {
        fail("Should have thrown RuntimeException.");
    }

    /**
     * Casts {@linkplain TestAggregate the aggregate under the test} to {@link Aggregate},
     * class, which is in the same package with this test, so that we call package-access methods.
     */
    private Aggregate<?, ?, ?> aggregate() {
        return aggregate;
    }

    private static List<Event> generateProjectEvents() {
        final String projectName = AggregateShould.class.getSimpleName();
        final List<Event> events = ImmutableList.<Event>builder()
                .add(event(projectCreated(ID, projectName), 1))
                .add(event(taskAdded(ID), 3))
                .add(event(projectStarted(ID), 4))
                .build();
        return events;
    }

    @Before
    public void setUp() {
        ModelTests.clearModel();
        aggregate = newAggregate(ID);
        amishAggregate = newAmishAggregate(ID);
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();

        repository = new TestAggregateRepository();
        boundedContext.register(repository);
    }

    @After
    public void tearDown() throws Exception {
        boundedContext.close();
    }

    @Test
    public void handle_one_command_and_apply_appropriate_event() {
        dispatchCommand(aggregate, env(createProject));

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);
    }

    @Test
    public void advances_the_version_by_one_upon_handling_command_with_one_event() {
        final int version = aggregate.versionNumber();

        dispatchCommand(aggregate, env(createProject));

        assertEquals(version + 1, aggregate.versionNumber());
    }

    /**
     * This is a most typical use-case with a single event returned in response to a command.
     */
    @Test
    public void advances_the_version_by_one_with_single_event_and_with_empty_event_applier() {
        final int version = amishAggregate.versionNumber();

        final List<? extends Message> messages = dispatchCommand(amishAggregate, env(pauseProject));
        assertEquals(1, messages.size());

        assertEquals(version + 1, amishAggregate.versionNumber());
    }

    /**
     * This tests a use-case implying returning a {@code List} of events in response to a command.
     */
    @Test
    public void advances_the_version_by_number_of_events_with_several_events_and_empty_appliers() {
        final int version = amishAggregate.versionNumber();

        final List<? extends Message> eventMessages =
                dispatchCommand(amishAggregate, env(cancelProject));
        // Expecting to return more than one to differ from other testing scenarios.
        assertTrue(eventMessages.size() > 1);

        assertEquals(version + eventMessages.size(), amishAggregate.versionNumber());
    }

    @Test
    public void write_its_version_into_event_context() {
        dispatchCommand(aggregate, env(createProject));

        // Get the first event since the command handler produces only one event message.
        final Aggregate<?, ?, ?> agg = this.aggregate;
        final List<Event> uncommittedEvents = agg.getUncommittedEvents();
        final Event event = uncommittedEvents.get(0);

        assertEquals(this.aggregate.getVersion(), event.getContext()
                                                       .getVersion());
    }

    @Test
    public void handle_only_dispatched_command() {
        dispatchCommand(aggregate, env(createProject));

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        assertFalse(aggregate.isAddTaskCommandHandled);
        assertFalse(aggregate.isTaskAddedEventApplied);

        assertFalse(aggregate.isStartProjectCommandHandled);
        assertFalse(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void invoke_applier_after_command_handler() {
        dispatchCommand(aggregate, env(createProject));
        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        dispatchCommand(aggregate, env(addTask));
        assertTrue(aggregate.isAddTaskCommandHandled);
        assertTrue(aggregate.isTaskAddedEventApplied);

        dispatchCommand(aggregate, env(startProject));
        assertTrue(aggregate.isStartProjectCommandHandled);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void react_on_rejection_by_rejection_message_only() {
        dispatchRejection(aggregate, cannotModifyDeletedEntity(StringValue.class));
        assertTrue(aggregate.isRejectionHandled);
        assertFalse(aggregate.isRejectionWithCmdHandled);
    }

    @Test
    public void react_on_rejection_by_rejection_and_command_message() {
        dispatchRejection(aggregate, cannotModifyDeletedEntity(AggAddTask.class));
        assertTrue(aggregate.isRejectionWithCmdHandled);
        assertFalse(aggregate.isRejectionHandled);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_command_handler() {
        ModelTests.clearModel();
        final AggregateWithMissingApplier aggregate = new AggregateWithMissingApplier(ID);

        // Pass a command for which the target aggregate does not have a handling method.
        dispatchCommand(aggregate, env(addTask));
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_event_applier_for_non_state_neutral_event() {
        ModelTests.clearModel();
        final AggregateWithMissingApplier aggregate =
                new AggregateWithMissingApplier(ID);
        try {
            dispatchCommand(aggregate, env(createProject));
        } catch (IllegalStateException e) { // expected exception
            assertTrue(aggregate.isCreateProjectCommandHandled());
            throw e;
        }
    }

    @Test
    public void return_command_classes_which_are_handled_by_aggregate() {
        final Set<CommandClass> commandClasses =
                Model.getInstance()
                     .asAggregateClass(TestAggregate.class)
                     .getCommands();

        assertTrue(commandClasses.size() == 4);

        assertContains(commandClasses,
                       AggCreateProject.class,
                       AggAddTask.class,
                       AggStartProject.class,
                       ImportEvents.class);
    }

    @Test
    public void return_default_state_by_default() {
        final Project state = aggregate.getState();

        assertEquals(aggregate.getDefaultState(), state);
    }

    @Test
    public void update_state_when_the_command_is_handled() {
        dispatchCommand(aggregate, env(createProject));

        final Project state = aggregate.getState();

        assertEquals(ID, state.getId());
        assertEquals(Status.CREATED, state.getStatus());
    }

    @Test
    public void return_current_state_after_several_dispatches() {
        dispatchCommand(aggregate, env(createProject));
        assertEquals(Status.CREATED, aggregate.getState()
                                              .getStatus());

        dispatchCommand(aggregate, env(startProject));
        assertEquals(Status.STARTED, aggregate.getState()
                                              .getStatus());
    }

    @Test
    public void return_non_null_time_when_was_last_modified() {
        final Timestamp creationTime = new TestAggregate(ID).whenModified();
        assertNotNull(creationTime);
    }

    @Test
    public void record_modification_time_when_command_handled() {
        try {
            final Timestamp frozenTime = Time.getCurrentTime();
            Time.setProvider(new TimeTests.FrozenMadHatterParty(frozenTime));

            dispatchCommand(aggregate, env(createProject));

            assertEquals(frozenTime, aggregate.whenModified());
        } finally {
            Time.resetProvider();
        }
    }

    @Test
    public void advance_version_on_command_handled() {
        final int version = aggregate.versionNumber();

        dispatchCommand(aggregate, env(createProject));
        dispatchCommand(aggregate, env(startProject));
        dispatchCommand(aggregate, env(addTask));

        assertEquals(version + 3, aggregate.versionNumber());
    }

    @Test
    public void play_events() {
        final List<Event> events = generateProjectEvents();
        final AggregateStateRecord aggregateStateRecord =
                AggregateStateRecord.newBuilder()
                                    .addAllEvent(events)
                                    .build();

        final AggregateTransaction tx = AggregateTransaction.start(aggregate);
        aggregate().play(aggregateStateRecord);
        tx.commit();

        assertTrue(aggregate.isProjectCreatedEventApplied);
        assertTrue(aggregate.isTaskAddedEventApplied);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void restore_snapshot_during_play() {
        dispatchCommand(aggregate, env(createProject));

        final Snapshot snapshot = aggregate().toShapshot();

        final Aggregate anotherAggregate = newAggregate(aggregate.getId());

        final AggregateTransaction tx = AggregateTransaction.start(anotherAggregate);
        anotherAggregate.play(AggregateStateRecord.newBuilder()
                                                  .setSnapshot(snapshot)
                                                  .build());
        tx.commit();

        assertEquals(aggregate, anotherAggregate);
    }

    @Test
    public void not_return_any_uncommitted_event_records_by_default() {
        final List<Event> events = aggregate().getUncommittedEvents();

        assertTrue(events.isEmpty());
    }

    @Test
    public void return_uncommitted_event_records_after_dispatch() {
        aggregate.dispatchCommands(command(createProject),
                                   command(addTask),
                                   command(startProject));

        final List<Event> events = aggregate().getUncommittedEvents();

        assertContains(getEventClasses(events),
                       AggProjectCreated.class, AggTaskAdded.class, AggProjectStarted.class);
    }

    @Test
    public void not_return_any_event_records_when_commit_by_default() {
        final List<Event> events = aggregate().commitEvents();

        assertTrue(events.isEmpty());
    }

    @Test
    public void return_events_when_commit_after_dispatch() {
        aggregate.dispatchCommands(command(createProject),
                                   command(addTask),
                                   command(startProject));

        final List<Event> events = aggregate().commitEvents();

        assertContains(getEventClasses(events),
                       AggProjectCreated.class, AggTaskAdded.class, AggProjectStarted.class);
    }

    @Test
    public void clear_event_records_when_commit_after_dispatch() {
        aggregate.dispatchCommands(command(createProject),
                                   command(addTask),
                                   command(startProject));

        final List<Event> events = aggregate().commitEvents();
        assertFalse(events.isEmpty());

        final List<Event> emptyList = aggregate().commitEvents();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void transform_current_state_to_snapshot_event() {

        dispatchCommand(aggregate, env(createProject));

        final Snapshot snapshot = aggregate().toShapshot();
        final Project state = unpack(snapshot.getState());

        assertEquals(ID, state.getId());
        assertEquals(Status.CREATED, state.getStatus());
    }

    @Test
    public void restore_state_from_snapshot() {

        dispatchCommand(aggregate, env(createProject));

        final Snapshot snapshotNewProject = aggregate().toShapshot();

        final Aggregate anotherAggregate = newAggregate(aggregate.getId());

        final AggregateTransaction tx = AggregateTransaction.start(anotherAggregate);
        anotherAggregate.restore(snapshotNewProject);
        tx.commit();

        assertEquals(aggregate.getState(), anotherAggregate.getState());
        assertEquals(aggregate.getVersion(), anotherAggregate.getVersion());
        assertEquals(aggregate.getLifecycleFlags(), anotherAggregate.getLifecycleFlags());
    }

    @Test
    public void import_events() {
        final String projectName = getClass().getSimpleName();
        final ProjectId id = aggregate.getId();
        final ImportEvents importCmd =
                ImportEvents.newBuilder()
                            .setProjectId(id)
                            .addEvent(event(projectCreated(id, projectName), 1))
                            .addEvent(event(taskAdded(id), 2))
                            .build();
        aggregate.dispatchCommands(command(importCmd));

        assertTrue(aggregate.isProjectCreatedEventApplied);
        assertTrue(aggregate.isTaskAddedEventApplied);
    }

    @Test
    public void increment_version_when_applying_state_changing_event() {
        final int version = aggregate.getVersion()
                                     .getNumber();
        // Dispatch two commands that cause events that modify aggregate state.
        aggregate.dispatchCommands(command(createProject), command(startProject));

        assertEquals(version + 2, aggregate.getVersion()
                                           .getNumber());
    }

    @Test
    public void record_modification_timestamp() {
        try {
            final TimeTests.BackToTheFuture provider = new TimeTests.BackToTheFuture();
            Time.setProvider(provider);

            Timestamp currentTime = Time.getCurrentTime();

            aggregate.dispatchCommands(command(createProject));

            assertEquals(currentTime, aggregate.whenModified());

            currentTime = provider.forward(10);

            aggregate.dispatchCommands(command(startProject));

            assertEquals(currentTime, aggregate.whenModified());
        } finally {
            Time.resetProvider();
        }
    }

    @Test
    public void propagate_RuntimeException_when_handler_throws() {
        ModelTests.clearModel();
        final FaultyAggregate faultyAggregate = new FaultyAggregate(ID, true, false);

        final Command command = Given.ACommand.createProject();
        try {
            dispatchCommand(faultyAggregate, env(command.getMessage()));
            failNotThrows();
        } catch (RuntimeException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored") // We need it for checking.
            final Throwable cause = getRootCause(e);
            assertTrue(cause instanceof IllegalStateException);
            assertEquals(FaultyAggregate.BROKEN_HANDLER, cause.getMessage());
        }
    }

    @Test
    public void propagate_RuntimeException_when_applier_throws() {
        ModelTests.clearModel();
        final FaultyAggregate faultyAggregate =
                new FaultyAggregate(ID, false, true);

        final Command command = Given.ACommand.createProject();
        try {
            dispatchCommand(faultyAggregate, env(command.getMessage()));
            failNotThrows();
        } catch (RuntimeException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            // because we need it for checking.
            final Throwable cause = getRootCause(e);
            assertTrue(cause instanceof IllegalStateException);
            assertEquals(FaultyAggregate.BROKEN_APPLIER, cause.getMessage());
        }
    }

    @Test
    public void propagate_RuntimeException_when_play_raises_exception() {
        ModelTests.clearModel();
        final FaultyAggregate faultyAggregate =
                new FaultyAggregate(ID, false, true);
        try {
            final Event event = event(projectCreated(ID, getClass().getSimpleName()), 1);

            final AggregateTransaction tx = AggregateTransaction.start(faultyAggregate);
            ((Aggregate) faultyAggregate).play(AggregateStateRecord.newBuilder()
                                                                   .addEvent(event)
                                                                   .build());
            tx.commit();
            failNotThrows();
        } catch (RuntimeException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            // because we need it for checking.
            final Throwable cause = getRootCause(e);
            assertTrue(cause instanceof IllegalStateException);
            assertEquals(FaultyAggregate.BROKEN_APPLIER, cause.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void do_not_allow_getting_state_builder_from_outside_the_event_applier() {
        new IntAggregate(100).getBuilder();
    }

    @Test
    public void throw_InvalidEntityStateException_if_state_is_invalid() {
        final User user = User.newBuilder()
                              .setFirstName("|")
                              .setLastName("|")
                              .build();
        try {
            aggregateOfClass(UserAggregate.class).withId(getClass().getName())
                                                 .withVersion(1)
                                                 .withState(user)
                                                 .build();
            fail();
        } catch (InvalidEntityStateException e) {
            final List<ConstraintViolation> violations = e.getError()
                                                          .getValidationError()
                                                          .getConstraintViolationList();
            assertSize(user.getAllFields()
                           .size(), violations);
        }
    }

    @Test
    public void update_valid_entity_state() {
        final User user = User.newBuilder()
                              .setFirstName("Fname")
                              .setLastName("Lname")
                              .build();
        aggregateOfClass(UserAggregate.class).withId(getClass().getName())
                                             .withVersion(1)
                                             .withState(user)
                                             .build();
    }

    @Test
    public void traverse_the_history_iterating_through_newest_events_first() {
        final TenantId tenantId = newTenantId();
        final Command createCommand = command(createProject, tenantId);
        final Command startCommand = command(startProject, tenantId);
        final Command addTaskCommand = command(addTask, tenantId);
        final Command addTaskCommand2 = command(addTask, tenantId);

        final CommandBus commandBus = boundedContext.getCommandBus();
        final StreamObserver<Ack> noOpObserver = noOpObserver();
        commandBus.post(createCommand, noOpObserver);
        commandBus.post(addTaskCommand, noOpObserver);
        commandBus.post(newArrayList(addTaskCommand2, startCommand), noOpObserver);

        final TestAggregate aggregate = repository.loadAggregate(tenantId, ID);

        final Iterator<Event> history = aggregate.historyBackward();

        assertEquals(startCommand.getId(), getRootCommandId(history.next()));
        assertEquals(addTaskCommand2.getId(), getRootCommandId(history.next()));
        assertEquals(addTaskCommand.getId(), getRootCommandId(history.next()));
        assertEquals(createCommand.getId(), getRootCommandId(history.next()));
        assertFalse(history.hasNext());
    }

    @Test
    public void traverse_the_history_up_to_the_latest_snapshot() {
        repository.setSnapshotTrigger(3);

        final TenantId tenantId = newTenantId();
        final Command createCommand = command(createProject, tenantId);
        final Command startCommand = command(startProject, tenantId);
        final Command addTaskCommand = command(addTask, tenantId);
        final Command addTaskCommand2 = command(addTask, tenantId);

        final CommandBus commandBus = boundedContext.getCommandBus();
        final StreamObserver<Ack> noOpObserver = noOpObserver();
        commandBus.post(createCommand, noOpObserver);
        commandBus.post(startCommand, noOpObserver);
        commandBus.post(newArrayList(addTaskCommand, addTaskCommand2), noOpObserver);

        final TestAggregate aggregate = repository.loadAggregate(tenantId, ID);

        final Iterator<Event> history = aggregate.historyBackward();

        assertEquals(addTaskCommand2.getId(), getRootCommandId(history.next()));
        assertFalse(history.hasNext());
    }

    @Test
    public void acknowledge_DuplicateCommandException_when_the_command_was_handled_since_last_snapshot() {
        final TenantId tenantId = newTenantId();
        final Command createCommand = command(createProject, tenantId);

        final CommandBus commandBus = boundedContext.getCommandBus();
        final StreamObserver<Ack> noOpObserver = noOpObserver();
        final MemoizingObserver<Ack> memoizingObserver = memoizingObserver();
        commandBus.post(createCommand, noOpObserver);
        commandBus.post(createCommand, memoizingObserver);

        final List<Ack> responses = memoizingObserver.responses();
        final Ack ack = responses.get(0);
        assertTrue(ack.getStatus()
                      .hasError());

        final String errorType = DuplicateCommandException.class.getCanonicalName();
        assertEquals(errorType, ack.getStatus()
                                   .getError()
                                   .getType());
    }

    /**
     * Ensures that a {@linkplain io.spine.server.tuple.Pair pair} with an empty second
     * optional value returned from a command handler stores a single event.
     *
     * <p>The command handler that should return a pair is
     * {@link TaskAggregate#handle(AggAssignTask)
     * TaskAggregate#handle(AggAssignTask)}.
     */
    @Test
    public void create_single_event_for_a_pair_of_events_with_empty_for_a_command_dispatch() {
        BlackBoxBoundedContext
                .with(new TaskAggregateRepository())
                .receivesCommand(createTask())
                .verify(acked(1).withoutErrorsOrRejections())
                .verify(emitted(1))
                .close();
    }

    /**
     * Ensures that a {@linkplain io.spine.server.tuple.Pair pair} with an empty second optional
     * value returned from a reaction on an event stores a single event.
     *
     * <p>The first event is produced while handling a command by the
     * {@link TaskAggregate#handle(AggAssignTask) TaskAggregate#handle(AggAssignTask)}.
     * Then as a reaction to this event a single event should be fired as part of the pair by
     * {@link TaskAggregate#on(AggTaskAssigned) TaskAggregate#on(AggTaskAssigned)}.
     */
    @Test
    public void create_single_event_for_a_pair_of_events_with_empty_for_an_event_react() {
        BlackBoxBoundedContext
                .with(new TaskAggregateRepository())
                .receivesCommand(assignTask())
                .verify(acked(1).withoutErrorsOrRejections())
                .verify(emitted(2))
                .verify(emitted(AggTaskAssigned.class))
                .verify(emitted(AggUserNotified.class))
                .close();
    }

    /**
     * Ensures that a {@linkplain io.spine.server.tuple.Pair pair} with an empty second optional
     * value returned from a reaction on a rejection stores a single event.
     *
     * <p>The rejection is fired by the {@link TaskAggregate#handle(AggReassignTask)
     * TaskAggregate.handle(AggReassignTask)}
     * and handled by the {@link TaskAggregate#on(Rejections.AggCannotReassignUnassignedTask)
     * TaskAggregate.on(AggCannotReassignUnassignedTask)}.
     */
    @Test
    public void create_single_event_for_a_pair_of_events_with_empty_for_a_rejection_react() {
        BlackBoxBoundedContext
                .with(new TaskAggregateRepository())
                .receivesCommand(reassignTask())
                .verify(acked(1).withoutErrorsOrRejections())
                .verify(emitted(1))
                .verify(emitted(AggUserNotified.class))
                .close();
    }
}
