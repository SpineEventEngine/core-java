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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Event;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.server.command.Assign;
import org.spine3.server.entity.InvalidEntityStateException;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.test.TestEventFactory;
import org.spine3.test.TimeTests;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.ProjectValidatingBuilder;
import org.spine3.test.aggregate.Status;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.ImportEvents;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.test.aggregate.user.User;
import org.spine3.test.aggregate.user.UserValidatingBuilder;
import org.spine3.time.Time;
import org.spine3.type.CommandClass;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.server.aggregate.Given.EventMessage.projectCreated;
import static org.spine3.server.aggregate.Given.EventMessage.projectStarted;
import static org.spine3.server.aggregate.Given.EventMessage.taskAdded;
import static org.spine3.test.Given.aggregateOfClass;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Tests.newVersionWithNumber;
import static org.spine3.test.Verify.assertSize;
import static org.spine3.test.aggregate.Project.newBuilder;

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
            TestEventFactory.newInstance(pack(ID), requestFactory);

    private static final CreateProject createProject = Given.CommandMessage.createProject(ID);
    private static final AddTask addTask = Given.CommandMessage.addTask(ID);
    private static final StartProject startProject = Given.CommandMessage.startProject(ID);

    private TestAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = newAggregate(ID);
    }

    private static TestAggregate newAggregate(ProjectId id) {
        final TestAggregate result = new TestAggregate(id);
        result.init();
        return result;
    }

    private static CommandEnvelope env(Message commandMessage) {
        return CommandEnvelope.of(requestFactory.command().create(commandMessage));
    }

    @Test
    public void handle_one_command_and_apply_appropriate_event() {
        aggregate.dispatchForTest(env(createProject));

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);
    }

    @Test
    public void advances_the_version_by_one_upon_handling_command_with_one_event() {
        final int version = aggregate.versionNumber();

        aggregate.dispatchForTest(env(createProject));

        assertEquals(version + 1, aggregate.versionNumber());
    }

    //TODO:5/13/17:alex.tymchenko: clarify this behaviour
//    @Test
//    public void write_its_version_into_event_context() {
//        aggregate.dispatchForTest(env(createProject));
//
//        // Get the first event since the command handler produces only one event message.
//        final Event event = aggregate.getUncommittedEvents()
//                                     .get(0);
//
//        assertEquals(aggregate.getVersion(), event.getContext()
//                                                  .getVersion());
//    }

    @Test
    public void handle_only_dispatched_command() {
        aggregate.dispatchForTest(env(createProject));

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        assertFalse(aggregate.isAddTaskCommandHandled);
        assertFalse(aggregate.isTaskAddedEventApplied);

        assertFalse(aggregate.isStartProjectCommandHandled);
        assertFalse(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void invoke_applier_after_command_handler() {
        aggregate.dispatchForTest(env(createProject));
        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        aggregate.dispatchForTest(env(addTask));
        assertTrue(aggregate.isAddTaskCommandHandled);
        assertTrue(aggregate.isTaskAddedEventApplied);

        aggregate.dispatchForTest(env(startProject));
        assertTrue(aggregate.isStartProjectCommandHandled);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_command_handler() {
        final TestAggregateForCaseMissingHandlerOrApplier aggregate =
                new TestAggregateForCaseMissingHandlerOrApplier(ID);

        aggregate.dispatchForTest(env(addTask));
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_event_applier_for_non_state_neutral_event() {
        final TestAggregateForCaseMissingHandlerOrApplier aggregate =
                new TestAggregateForCaseMissingHandlerOrApplier(ID);
        try {
            aggregate.dispatchForTest(env(createProject));
        } catch (IllegalStateException e) { // expected exception
            assertTrue(aggregate.isCreateProjectCommandHandled);
            throw e;
        }
    }

    @Test
    public void return_command_classes_which_are_handled_by_aggregate() {
        final Set<CommandClass> commandClasses =
                Aggregate.TypeInfo.getCommandClasses(TestAggregate.class);

        assertTrue(commandClasses.size() == 4);
        assertTrue(commandClasses.contains(CommandClass.of(CreateProject.class)));
        assertTrue(commandClasses.contains(CommandClass.of(AddTask.class)));
        assertTrue(commandClasses.contains(CommandClass.of(StartProject.class)));
        assertTrue(commandClasses.contains(CommandClass.of(ImportEvents.class)));
    }

    @Test
    public void return_default_state_by_default() {
        final Project state = aggregate.getState();

        assertEquals(aggregate.getDefaultState(), state);
    }

    @Test
    public void update_state_when_the_command_is_handled() {
        aggregate.dispatchForTest(env(createProject));

        final Project state = aggregate.getState();

        assertEquals(ID, state.getId());
        assertEquals(Status.CREATED, state.getStatus());
    }

    @Test
    public void return_current_state_after_several_dispatches() {
        aggregate.dispatchForTest(env(createProject));
        assertEquals(Status.CREATED, aggregate.getState()
                                              .getStatus());

        aggregate.dispatchForTest(env(startProject));
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

            aggregate.dispatchForTest(env(createProject));

            assertEquals(frozenTime, aggregate.whenModified());
        } finally {
            Time.resetProvider();
        }
    }

    @Test
    public void advance_version_on_command_handled() {
        final int version = aggregate.versionNumber();

        aggregate.dispatchForTest(env(createProject));
        aggregate.dispatchForTest(env(startProject));
        aggregate.dispatchForTest(env(addTask));

        assertEquals(version + 3, aggregate.versionNumber());
    }

    @Test
    public void play_events() {
        final List<Event> events = generateProjectEvents();
        final AggregateStateRecord aggregateStateRecord =
                AggregateStateRecord.newBuilder()
                                    .addAllEvent(events)
                                    .build();

        aggregate.play(aggregateStateRecord);

        assertTrue(aggregate.isProjectCreatedEventApplied);
        assertTrue(aggregate.isTaskAddedEventApplied);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void restore_snapshot_during_play() {
        aggregate.dispatchForTest(env(createProject));

        final Snapshot snapshot = aggregate.toSnapshot();

        final TestAggregate anotherAggregate = newAggregate(aggregate.getId());

        anotherAggregate.play(AggregateStateRecord.newBuilder()
                                                  .setSnapshot(snapshot)
                                                  .build());

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

        assertContains(eventsToClasses(events),
                       ProjectCreated.class, TaskAdded.class, ProjectStarted.class);
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

        assertContains(eventsToClasses(events),
                       ProjectCreated.class, TaskAdded.class, ProjectStarted.class);
    }

    private static Command command(Message commandMessage) {
        return requestFactory.command().create(commandMessage);
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

        aggregate.dispatchForTest(env(createProject));

        final Snapshot snapshot = aggregate.toSnapshot();
        final Project state = unpack(snapshot.getState());

        assertEquals(ID, state.getId());
        assertEquals(Status.CREATED, state.getStatus());
    }

    @Test
    public void restore_state_from_snapshot() {

        aggregate.dispatchForTest(env(createProject));

        final Snapshot snapshotNewProject = aggregate.toSnapshot();

        final TestAggregate anotherAggregate = newAggregate(aggregate.getId());

        final AggregateTransaction<?, ?, ?> tx = AggregateTransaction.start(anotherAggregate);
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
                            .addEvent(event(projectCreated(id, projectName), 1))
                            .addEvent(event(taskAdded(id), 2))
                            .build();
        aggregate.dispatchCommands(command(importCmd));

        assertTrue(aggregate.isProjectCreatedEventApplied);
        assertTrue(aggregate.isTaskAddedEventApplied);
    }

    @SuppressWarnings("unused")
    private static class TestAggregate
            extends Aggregate<ProjectId, Project, ProjectValidatingBuilder> {

        private boolean isCreateProjectCommandHandled = false;
        private boolean isAddTaskCommandHandled = false;
        private boolean isStartProjectCommandHandled = false;

        private boolean isProjectCreatedEventApplied = false;
        private boolean isTaskAddedEventApplied = false;
        private boolean isProjectStartedEventApplied = false;

        protected TestAggregate(ProjectId id) {
            super(id);
        }

        /**
         * Overrides to expose the method to the text.
         */
        @VisibleForTesting
        @Override
        protected void init() {
            super.init();
        }

        @Assign
        ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            final ProjectCreated event = projectCreated(cmd.getProjectId(),
                                                                           cmd.getName());
            return event;
        }

        @Assign
        TaskAdded handle(AddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            final TaskAdded event = taskAdded(cmd.getProjectId());
            return event;
        }

        @Assign
        List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            final ProjectStarted message = projectStarted(cmd.getProjectId());
            return newArrayList(message);
        }

        @Assign
        List<Event> handle(ImportEvents command, CommandContext ctx) {
            return command.getEventList();
        }

        @Apply
        private void event(ProjectCreated event) throws ConstraintViolationThrowable {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Status.CREATED);

            isProjectCreatedEventApplied = true;
        }

        @Apply
        private void event(TaskAdded event) {
            isTaskAddedEventApplied = true;
        }

        @Apply
        private void event(ProjectStarted event) throws ConstraintViolationThrowable {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Status.STARTED);

            isProjectStartedEventApplied = true;
        }

        public void dispatchCommands(Command... commands) {
            for (Command cmd : commands) {
                final Message commandMessage = Commands.getMessage(cmd);
                dispatchForTest(env(commandMessage));
            }
        }
    }

    /** Class only for test cases: exception if missing command handler or missing event applier. */
    private static class TestAggregateForCaseMissingHandlerOrApplier
            extends Aggregate<ProjectId, Project, ProjectValidatingBuilder> {

        private boolean isCreateProjectCommandHandled = false;

        public TestAggregateForCaseMissingHandlerOrApplier(ProjectId id) {
            super(id);
        }

        /** There is no event applier for ProjectCreated event (intentionally). */
        @Assign
        ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return projectCreated(cmd.getProjectId(), cmd.getName());
        }
    }

    private static class TestAggregateWithIdInteger
            extends Aggregate<Integer, Project, ProjectValidatingBuilder> {
        private TestAggregateWithIdInteger(Integer id) {
            super(id);
        }
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

    /** The class to check raising and catching exceptions. */
    @SuppressWarnings("unused")
    private static class FaultyAggregate
            extends Aggregate<ProjectId, Project, ProjectValidatingBuilder> {

        private static final String BROKEN_HANDLER = "broken_handler";
        private static final String BROKEN_APPLIER = "broken_applier";

        private final boolean brokenHandler;
        private final boolean brokenApplier;

        private FaultyAggregate(ProjectId id, boolean brokenHandler, boolean brokenApplier) {
            super(id);
            this.brokenHandler = brokenHandler;
            this.brokenApplier = brokenApplier;
        }

        //TODO:5/15/17:alex.tymchenko: remove this method.

//        /**
//         * This method attempts to call {@link #updateState(Message, Version) setState()}
//         * directly, which should result in {@link IllegalStateException}.
//         */
//        void tryToUpdateStateDirectly() {
//            injectState(Project.getDefaultInstance(), Version.getDefaultInstance());
//        }

        @Assign
        ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            if (brokenHandler) {
                throw new IllegalStateException(BROKEN_HANDLER);
            }
            return projectCreated(cmd.getProjectId(), cmd.getName());
        }

        @Apply
        private void event(ProjectCreated event) {
            if (brokenApplier) {
                throw new IllegalStateException(BROKEN_APPLIER);
            }

            final Project newState = newBuilder(getState())
                    .setId(event.getProjectId())
                    .build();

            getBuilder().mergeFrom(newState);
        }
    }

    @Test
    public void propagate_RuntimeException_when_handler_throws() {
        final FaultyAggregate faultyAggregate =
                new FaultyAggregate(ID, true, false);

        final Command command = Given.ACommand.createProject();
        try {
            faultyAggregate.dispatchForTest(env(command.getMessage()));
        } catch (RuntimeException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored") // We need it for checking.
            final Throwable cause = getRootCause(e);
            assertTrue(cause instanceof IllegalStateException);
            assertEquals(FaultyAggregate.BROKEN_HANDLER, cause.getMessage());
        }
    }

    @Test
    public void propagate_RuntimeException_when_applier_throws() {
        final FaultyAggregate faultyAggregate =
                new FaultyAggregate(ID, false, true);

        final Command command = Given.ACommand.createProject();
        try {
            faultyAggregate.dispatchForTest(env(command.getMessage()));
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
        final FaultyAggregate faultyAggregate =
                new FaultyAggregate(ID, false, true);
        try {
            final Event event = event(projectCreated(ID, getClass().getSimpleName()), 1);
            faultyAggregate.play(AggregateStateRecord.newBuilder()
                                                     .addEvent(event)
                                                     .build());
        } catch (RuntimeException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            // because we need it for checking.
            final Throwable cause = getRootCause(e);
            assertTrue(cause instanceof IllegalStateException);
            assertEquals(FaultyAggregate.BROKEN_APPLIER, cause.getMessage());
        }
    }

    //TODO:5/13/17:alex.tymchenko: remove this test, as `updateState` is now hidden.
//    @Test(expected = IllegalStateException.class)
//    public void reject_direct_calls_to_setState() {
//        final FaultyAggregate faultyAggregate =
//                new FaultyAggregate(ID, false, false);
//
//        // This should throw ISE.
//        faultyAggregate.tryToUpdateStateDirectly();
//    }

    @Test(expected = IllegalStateException.class)
    public void do_not_allow_getting_state_builder_from_outside_the_event_applier() {
        new TestAggregateWithIdInteger(100).getBuilder();
    }

    @Test
    public void have_TypeInfo_utility_class() {
        assertHasPrivateParameterlessCtor(Aggregate.TypeInfo.class);
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

    private static class UserAggregate extends Aggregate<String, User, UserValidatingBuilder> {
        private UserAggregate(String id) {
            super(id);
        }
    }

    /*
     * Utility methods.
     ********************************/

    private static Collection<Class<? extends Message>> eventsToClasses(Collection<Event> events) {
        return transform(events, new Function<Event, Class<? extends Message>>() {
            @Nullable // return null because an exception won't be propagated in this case
            @Override
            public Class<? extends Message> apply(@Nullable Event record) {
                if (record == null) {
                    return null;
                }
                return unpack(record.getMessage()).getClass();
            }
        });
    }

    private static void assertContains(Collection<Class<? extends Message>> actualClasses,
                                       Class... expectedClasses) {
        assertTrue(actualClasses.containsAll(newArrayList(expectedClasses)));
        assertEquals(expectedClasses.length, actualClasses.size());
    }

    private static Event event(Message eventMessage, int versionNumber) {
        return eventFactory.createEvent(eventMessage, newVersionWithNumber(versionNumber));
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
}
