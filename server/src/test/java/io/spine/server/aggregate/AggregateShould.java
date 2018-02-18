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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.Identifier;
import io.spine.base.Error;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Commands;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateTestEnv;
import io.spine.server.aggregate.given.AggregateTestEnv.AggregateWithMissingApplier;
import io.spine.server.aggregate.given.AggregateTestEnv.FaultyAggregate;
import io.spine.server.aggregate.given.AggregateTestEnv.IntAggregate;
import io.spine.server.aggregate.given.Given;
import io.spine.server.command.Assign;
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.InvalidEntityStateException;
import io.spine.server.model.Model;
import io.spine.server.model.ModelTests;
import io.spine.test.TimeTests;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggAssignTask;
import io.spine.test.aggregate.command.AggCancelProject;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggPauseProject;
import io.spine.test.aggregate.command.AggReassignTask;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.command.ImportEvents;
import io.spine.test.aggregate.event.AggProjectCancelled;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectPaused;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.test.aggregate.event.AggTaskAssigned;
import io.spine.test.aggregate.event.AggUserNotified;
import io.spine.test.aggregate.rejection.Rejections;
import io.spine.test.aggregate.user.User;
import io.spine.time.Time;
import io.spine.type.TypeUrl;
import io.spine.validate.ConstraintViolation;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.given.GivenVersion.withNumber;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.TestCommandClasses.assertContains;
import static io.spine.server.TestEventClasses.assertContains;
import static io.spine.server.TestEventClasses.getEventClasses;
import static io.spine.server.aggregate.AggregateMessageDispatcher.dispatchCommand;
import static io.spine.server.aggregate.given.AggregateTestEnv.assignTask;
import static io.spine.server.aggregate.given.AggregateTestEnv.createTask;
import static io.spine.server.aggregate.given.AggregateTestEnv.newTaskBoundedContext;
import static io.spine.server.aggregate.given.AggregateTestEnv.readAllEvents;
import static io.spine.server.aggregate.given.AggregateTestEnv.reassignTask;
import static io.spine.server.aggregate.given.AggregateTestEnv.typeUrlOf;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCancelled;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static io.spine.server.aggregate.given.Given.EventMessage.projectPaused;
import static io.spine.server.aggregate.given.Given.EventMessage.projectStarted;
import static io.spine.server.aggregate.given.Given.EventMessage.taskAdded;
import static io.spine.server.entity.given.Given.aggregateOfClass;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukkov
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
public class AggregateShould {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(AggregateShould.class);
    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("prj-01")
                                                 .build();

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(Identifier.pack(ID), requestFactory);

    private static final AggCreateProject createProject = Given.CommandMessage.createProject(ID);
    private static final AggPauseProject pauseProject = Given.CommandMessage.pauseProject(ID);
    private static final AggCancelProject cancelProject = Given.CommandMessage.cancelProject(ID);
    private static final AggAddTask addTask = Given.CommandMessage.addTask(ID);
    private static final AggStartProject startProject = Given.CommandMessage.startProject(ID);

    private TestAggregate aggregate;

    private AmishAggregate amishAggregate;

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

    private static CommandEnvelope env(Message commandMessage) {
        return CommandEnvelope.of(requestFactory.command()
                                                .create(commandMessage));
    }

    private static Command command(Message commandMessage) {
        return requestFactory.command()
                             .create(commandMessage);
    }

    private static Command command(Message commandMessage, TenantId tenantId) {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(AggregateShould.class, tenantId);
        return requestFactory.command()
                             .create(commandMessage);
    }

    private static void failNotThrows() {
        fail("Should have thrown RuntimeException.");
    }

    private static Event event(Message eventMessage, int versionNumber) {
        return eventFactory.createEvent(eventMessage, withNumber(versionNumber));
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
        final Event event = aggregate.getUncommittedEvents()
                                     .get(0);

        assertEquals(aggregate.getVersion(), event.getContext()
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
        aggregate.play(aggregateStateRecord);
        tx.commit();

        assertTrue(aggregate.isProjectCreatedEventApplied);
        assertTrue(aggregate.isTaskAddedEventApplied);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void restore_snapshot_during_play() {
        dispatchCommand(aggregate, env(createProject));

        final Snapshot snapshot = aggregate.toSnapshot();

        final TestAggregate anotherAggregate = newAggregate(aggregate.getId());

        final AggregateTransaction tx = AggregateTransaction.start(anotherAggregate);
        anotherAggregate.play(AggregateStateRecord.newBuilder()
                                                  .setSnapshot(snapshot)
                                                  .build());
        tx.commit();

        assertEquals(aggregate, anotherAggregate);
    }

    @Test
    public void not_return_any_uncommitted_event_records_by_default() {
        final List<Event> events = aggregate.getUncommittedEvents();

        assertTrue(events.isEmpty());
    }

    @Test
    public void return_uncommitted_event_records_after_dispatch() {
        aggregate.dispatchCommands(command(createProject),
                                   command(addTask),
                                   command(startProject));

        final List<Event> events = aggregate.getUncommittedEvents();

        assertContains(getEventClasses(events),
                       AggProjectCreated.class, AggTaskAdded.class, AggProjectStarted.class);
    }

    @Test
    public void not_return_any_event_records_when_commit_by_default() {
        final List<Event> events = aggregate.commitEvents();

        assertTrue(events.isEmpty());
    }

    @Test
    public void return_events_when_commit_after_dispatch() {
        aggregate.dispatchCommands(command(createProject),
                                   command(addTask),
                                   command(startProject));

        final List<Event> events = aggregate.commitEvents();

        assertContains(getEventClasses(events),
                       AggProjectCreated.class, AggTaskAdded.class, AggProjectStarted.class);
    }

    @Test
    public void clear_event_records_when_commit_after_dispatch() {
        aggregate.dispatchCommands(command(createProject),
                                   command(addTask),
                                   command(startProject));

        final List<Event> events = aggregate.commitEvents();
        assertFalse(events.isEmpty());

        final List<Event> emptyList = aggregate.commitEvents();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void transform_current_state_to_snapshot_event() {

        dispatchCommand(aggregate, env(createProject));

        final Snapshot snapshot = aggregate.toSnapshot();
        final Project state = unpack(snapshot.getState());

        assertEquals(ID, state.getId());
        assertEquals(Status.CREATED, state.getStatus());
    }

    @Test
    public void restore_state_from_snapshot() {

        dispatchCommand(aggregate, env(createProject));

        final Snapshot snapshotNewProject = aggregate.toSnapshot();

        final TestAggregate anotherAggregate = newAggregate(aggregate.getId());

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
    public void record_modification_timestamp() throws InterruptedException {
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
            AggregatePlayer.play(faultyAggregate, AggregateStateRecord.newBuilder()
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
            aggregateOfClass(AggregateTestEnv.UserAggregate.class).withId(getClass().getName())
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
        aggregateOfClass(AggregateTestEnv.UserAggregate.class).withId(getClass().getName())
                                                              .withVersion(1)
                                                              .withState(user)
                                                              .build();
    }

    /**
     * Ensures that a {@link io.spine.server.tuple.Pair} with an empty second optional value
     * returned from a command handler stores a single event.
     *
     * <p>The command handler that should return a pair is
     * {@link io.spine.server.aggregate.given.AggregateTestEnv.TaskAggregate#handle(AggAssignTask, CommandContext)}.
     */
    @Test
    public void create_single_event_for_a_pair_of_events_with_empty_for_a_command_dispatch() {
        final BoundedContext boundedContext = newTaskBoundedContext();

        final TenantId tenantId = AggregateTestEnv.newTenantId();
        final Command command = command(createTask(), tenantId);
        final MemoizingObserver<Ack> observer = memoizingObserver();

        boundedContext.getCommandBus()
                      .post(command, observer);

        assertNull(observer.getError());

        final List<Ack> responses = observer.responses();
        assertSize(1, responses);

        final Ack response = responses.get(0);
        final io.spine.core.Status status = response.getStatus();
        final Error emptyError = Error.getDefaultInstance();
        assertEquals(emptyError, status.getError());

        final Rejection emptyRejection = Rejection.getDefaultInstance();
        assertEquals(emptyRejection, status.getRejection());

        final List<Event> events = readAllEvents(boundedContext, tenantId);
        assertSize(1, events);
    }

    /**
     * Ensures that a {@link io.spine.server.tuple.Pair} with an empty second optional value
     * returned from a reaction on an event stores a single event.
     *
     * <p>The first event is produced while handling a command by the
     * {@link io.spine.server.aggregate.given.AggregateTestEnv.TaskAggregate#handle(AggAssignTask, CommandContext)}.
     * Then as a reaction to this event a single event should be fired as part of the pair by
     * {@link io.spine.server.aggregate.given.AggregateTestEnv.TaskAggregate#on(AggTaskAssigned)}.
     */
    @Test
    public void create_single_event_for_a_pair_of_events_with_empty_for_an_event_react() {
        final BoundedContext boundedContext = newTaskBoundedContext();

        final TenantId tenantId = AggregateTestEnv.newTenantId();
        final Command command = command(assignTask(), tenantId);
        final MemoizingObserver<Ack> observer = memoizingObserver();

        boundedContext.getCommandBus()
                      .post(command, observer);

        assertNull(observer.getError());

        final List<Ack> responses = observer.responses();
        assertSize(1, responses);

        final Ack response = responses.get(0);
        final io.spine.core.Status status = response.getStatus();
        final Error emptyError = Error.getDefaultInstance();
        assertEquals(emptyError, status.getError());

        final Rejection emptyRejection = Rejection.getDefaultInstance();
        assertEquals(emptyRejection, status.getRejection());

        final List<Event> events = readAllEvents(boundedContext, tenantId);
        assertSize(2, events);

        final Event sourceEvent = events.get(0);
        final TypeUrl taskAssignedType = TypeUrl.from(AggTaskAssigned.getDescriptor());
        assertEquals(typeUrlOf(sourceEvent), taskAssignedType);

        final Event reactionEvent = events.get(1);
        final TypeUrl userNotifiedType = TypeUrl.from(AggUserNotified.getDescriptor());
        assertEquals(typeUrlOf(reactionEvent), userNotifiedType);
    }

    /**
     * Ensures that a {@link io.spine.server.tuple.Pair} with an empty second optional value
     * returned from a reaction on a rejection stores a single event.
     *
     * <p>The rejection is fired by the {@link io.spine.server.aggregate.given.AggregateTestEnv.TaskAggregate#handle(AggReassignTask, CommandContext)}
     * and handled by the {@link io.spine.server.aggregate.given.AggregateTestEnv.TaskAggregate#on(Rejections.AggCannotReassignUnassignedTask)}.
     */
    @Test
    public void create_single_event_for_a_pair_of_events_with_empty_for_a_rejection_react() {
        final BoundedContext boundedContext = newTaskBoundedContext();

        final TenantId tenantId = AggregateTestEnv.newTenantId();
        final Command command = command(reassignTask(), tenantId);
        final MemoizingObserver<Ack> observer = memoizingObserver();

        boundedContext.getCommandBus()
                      .post(command, observer);

        assertNull(observer.getError());

        final List<Ack> responses = observer.responses();
        assertSize(1, responses);

        final Ack response = responses.get(0);
        final io.spine.core.Status status = response.getStatus();
        final Error emptyError = Error.getDefaultInstance();
        assertEquals(emptyError, status.getError());

        final Rejection emptyRejection = Rejection.getDefaultInstance();
        assertEquals(emptyRejection, status.getRejection());

        final List<Event> events = readAllEvents(boundedContext, tenantId);
        assertSize(1, events);

        final Event reactionEvent = events.get(0);
        final TypeUrl userNotifiedType = TypeUrl.from(AggUserNotified.getDescriptor());
        assertEquals(typeUrlOf(reactionEvent), userNotifiedType);
    }

    /**
     * Helper class for invoking {@link Aggregate#play(AggregateStateRecord)}.
     */
    private static class AggregatePlayer {
        private static void play(Aggregate aggregate, AggregateStateRecord record) {
            aggregate.play(record);
        }
    }

    /**
     * An aggregate class with handlers and appliers.
     *
     * <p>This class is declared here instead of being inner class of {@link AggregateTestEnv}
     * because it is heavily connected with internals of this test suite.
     */
    private static class TestAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private boolean isCreateProjectCommandHandled = false;
        private boolean isAddTaskCommandHandled = false;
        private boolean isStartProjectCommandHandled = false;

        private boolean isProjectCreatedEventApplied = false;
        private boolean isTaskAddedEventApplied = false;
        private boolean isProjectStartedEventApplied = false;

        private TestAggregate(ProjectId id) {
            super(id);
        }

        /**
         * Overrides to expose the method to the text.
         */
        @VisibleForTesting
        @Override
        public void init() {
            super.init();
        }

        @Assign
        AggProjectCreated handle(AggCreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            final AggProjectCreated event = projectCreated(cmd.getProjectId(),
                                                           cmd.getName());
            return event;
        }

        @Assign
        AggTaskAdded handle(AggAddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            final AggTaskAdded event = taskAdded(cmd.getProjectId());
            return event.toBuilder()
                        .setTask(cmd.getTask())
                        .build();
        }

        @Assign
        List<AggProjectStarted> handle(AggStartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            final AggProjectStarted message = projectStarted(cmd.getProjectId());
            return newArrayList(message);
        }

        @Assign
        List<Event> handle(ImportEvents command, CommandContext ctx) {
            return command.getEventList();
        }

        @Apply
        private void event(AggProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Status.CREATED);

            isProjectCreatedEventApplied = true;
        }

        @Apply
        private void event(AggTaskAdded event) {
            isTaskAddedEventApplied = true;
            getBuilder().addTask(event.getTask());
        }

        @Apply
        private void event(AggProjectStarted event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Status.STARTED);

            isProjectStartedEventApplied = true;
        }

        private void dispatchCommands(Command... commands) {
            for (Command cmd : commands) {
                final Message commandMessage = Commands.getMessage(cmd);
                AggregateMessageDispatcher.dispatchCommand(this, env(commandMessage));
            }
        }
    }

    /**
     * A test-only aggregate, that handles some commands fine, but does not change own state
     * in any of event appliers.
     *
     * <p>One might say, this aggregate sticks to its roots and denies changes. Hence the name.
     */
    private static class AmishAggregate extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private AmishAggregate(ProjectId id) {
            super(id);
        }

        /**
         * Overrides to expose the method to the text.
         */
        @VisibleForTesting
        @Override
        public void init() {
            super.init();
        }

        @Assign
        AggProjectPaused handle(AggPauseProject cmd, CommandContext ctx) {
            final AggProjectPaused event = projectPaused(cmd.getProjectId());
            return event;
        }

        @Assign
        List<Message> handle(AggCancelProject cmd, CommandContext ctx) {
            final AggProjectPaused firstPaused = projectPaused(cmd.getProjectId());
            final AggProjectCancelled thenCancelled = projectCancelled(cmd.getProjectId());
            return Lists.<Message>newArrayList(firstPaused, thenCancelled);
        }

        @Apply
        private void on(AggProjectPaused event) {
            // do nothing.
        }

        @Apply
        private void on(AggProjectCancelled event) {
            // do nothing.
        }
    }
}
