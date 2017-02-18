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
import com.google.common.base.Throwables;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.protobuf.Timestamps2;
import org.spine3.server.aggregate.storage.AggregateEvents;
import org.spine3.server.aggregate.storage.Snapshot;
import org.spine3.server.command.Assign;
import org.spine3.server.type.CommandClass;
import org.spine3.test.Tests;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.command.AddTask;
import org.spine3.test.aggregate.command.CreateProject;
import org.spine3.test.aggregate.command.ImportEvents;
import org.spine3.test.aggregate.command.StartProject;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.ProjectStarted;
import org.spine3.test.aggregate.event.TaskAdded;
import org.spine3.testdata.Sample;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.server.command.CommandHandlingEntity.getCommandClasses;
import static org.spine3.test.aggregate.Project.newBuilder;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"TypeMayBeWeakened", "ClassWithTooManyMethods"})
public class AggregateShould {

    private static final ProjectId ID = Sample.messageOfType(ProjectId.class);

    private static final CommandContext COMMAND_CONTEXT = createCommandContext();
    private static final EventContext EVENT_CONTEXT = createEventContext(ID);

    private final CreateProject createProject = Given.CommandMessage.createProject(ID);
    private final AddTask addTask = Given.CommandMessage.addTask(ID);
    private final StartProject startProject = Given.CommandMessage.startProject(ID);

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

    @Test
    public void handle_one_command_and_apply_appropriate_event() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);
    }

    @Test
    public void advances_the_version_by_one_upon_handling_command_with_one_event() {
        final int version = aggregate.versionNumber();
        
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        assertEquals(version + 1, aggregate.versionNumber());
    }

    @Test
    public void writes_its_version_into_event_context() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        final Event event = aggregate.getUncommittedEvents()
                                     .get(0);
        assertEquals(aggregate.getVersion()
                              .getNumber(),
                     event.getContext()
                          .getVersion());
    }

    @Test
    public void handle_only_dispatched_command() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        assertFalse(aggregate.isAddTaskCommandHandled);
        assertFalse(aggregate.isTaskAddedEventApplied);

        assertFalse(aggregate.isStartProjectCommandHandled);
        assertFalse(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void invoke_applier_after_command_handler() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);
        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        aggregate.dispatchForTest(addTask, COMMAND_CONTEXT);
        assertTrue(aggregate.isAddTaskCommandHandled);
        assertTrue(aggregate.isTaskAddedEventApplied);

        aggregate.dispatchForTest(startProject, COMMAND_CONTEXT);
        assertTrue(aggregate.isStartProjectCommandHandled);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_command_handler() {
        final TestAggregateForCaseMissingHandlerOrApplier aggregate =
                new TestAggregateForCaseMissingHandlerOrApplier(ID);

        aggregate.dispatchForTest(addTask, COMMAND_CONTEXT);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_event_applier_for_non_state_neutral_event() {
        final TestAggregateForCaseMissingHandlerOrApplier aggregate =
                new TestAggregateForCaseMissingHandlerOrApplier(ID);
        try {
            aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);
        } catch (IllegalStateException e) { // expected exception
            assertTrue(aggregate.isCreateProjectCommandHandled);
            throw e;
        }
    }

    @Test
    public void return_command_classes_which_are_handled_by_aggregate() {
        final Set<CommandClass> classes = getCommandClasses(TestAggregate.class);

        assertTrue(classes.size() == 4);
        assertTrue(classes.contains(CommandClass.of(CreateProject.class)));
        assertTrue(classes.contains(CommandClass.of(AddTask.class)));
        assertTrue(classes.contains(CommandClass.of(StartProject.class)));
        assertTrue(classes.contains(CommandClass.of(ImportEvents.class)));
    }

    @Test
    public void return_default_state_by_default() {
        final Project state = aggregate.getState();

        assertEquals(aggregate.getDefaultState(), state);
    }

    @Test
    public void update_state_when_the_command_is_handled() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        final Project state = aggregate.getState();

        assertEquals(ID, state.getId());
        assertEquals(Project.Status.CREATED, state.getStatus());
    }

    @Test
    public void return_current_state_after_several_dispatches() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);
        assertEquals(Project.Status.CREATED, aggregate.getState()
                                                      .getStatus());

        aggregate.dispatchForTest(startProject, COMMAND_CONTEXT);
        assertEquals(Project.Status.STARTED, aggregate.getState()
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
            final Timestamp frozenTime = Timestamps2.getCurrentTime();
            Timestamps2.setProvider(new Tests.FrozenMadHatterParty(frozenTime));

            aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

            assertEquals(frozenTime, aggregate.whenModified());
        } finally {
            Timestamps2.resetProvider();
        }
    }

    @Test
    public void advance_version_on_command_handled() {
        final int version = aggregate.versionNumber();

        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);
        aggregate.dispatchForTest(startProject, COMMAND_CONTEXT);
        aggregate.dispatchForTest(addTask, COMMAND_CONTEXT);

        assertEquals(version + 3, aggregate.versionNumber());
    }

    @Test
    public void play_events() {
        final List<Event> events = getProjectEvents();
        final AggregateEvents aggregateEvents =
                AggregateEvents.newBuilder()
                               .addAllEvent(events)
                               .build();
        
        aggregate.play(aggregateEvents);

        assertTrue(aggregate.isProjectCreatedEventApplied);
        assertTrue(aggregate.isTaskAddedEventApplied);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void restore_snapshot_during_play() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        final Snapshot snapshot = aggregate.toSnapshot();

        final TestAggregate anotherAggregate = newAggregate(aggregate.getId());

        anotherAggregate.play(AggregateEvents.newBuilder()
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
        aggregate.dispatchCommands(createProject, addTask, startProject);

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
        aggregate.dispatchCommands(createProject, addTask, startProject);

        final List<Event> events = aggregate.commitEvents();

        assertContains(eventsToClasses(events),
                       ProjectCreated.class, TaskAdded.class, ProjectStarted.class);
    }

    @Test
    public void clear_event_records_when_commit_after_dispatch() {
        aggregate.dispatchCommands(createProject, addTask, startProject);

        final List<Event> events = aggregate.commitEvents();
        assertFalse(events.isEmpty());

        final List<Event> emptyList = aggregate.commitEvents();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void transform_current_state_to_snapshot_event() {

        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        final Snapshot snapshot = aggregate.toSnapshot();
        final Project state = unpack(snapshot.getState());

        assertEquals(ID, state.getId());
        assertEquals(Project.Status.CREATED, state.getStatus());
    }

    @Test
    public void restore_state_from_snapshot() {

        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        final Snapshot snapshotNewProject = aggregate.toSnapshot();

        final TestAggregate anotherAggregate = newAggregate(aggregate.getId());

        anotherAggregate.restore(snapshotNewProject);
        assertEquals(aggregate.getState(), anotherAggregate.getState());
        assertEquals(aggregate.getVersion(), anotherAggregate.getVersion());
        assertEquals(aggregate.getStatus(), anotherAggregate.getStatus());
    }

    @Test
    public void import_events() {
        final ImportEvents importCmd =
                ImportEvents.newBuilder()
                            .addEvent(Given.Event.projectCreated(aggregate.getId()))
                            .addEvent(Given.Event.taskAdded(aggregate.getId()))
                            .build();
        aggregate.dispatchCommands(importCmd);

        assertTrue(aggregate.isProjectCreatedEventApplied);
        assertTrue(aggregate.isTaskAddedEventApplied);
    }

    @SuppressWarnings("unused")
    private static class TestAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

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
            final ProjectCreated event = Given.EventMessage.projectCreated(cmd.getProjectId(), cmd.getName());
            return event;
        }

        @Assign
        TaskAdded handle(AddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            final TaskAdded event = Given.EventMessage.taskAdded(cmd.getProjectId());
            return event;
        }

        @Assign
        List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            final ProjectStarted message = Given.EventMessage.projectStarted(cmd.getProjectId());
            return newArrayList(message);
        }

        @Assign
        List<Event> handle(ImportEvents command, CommandContext ctx) {
            return command.getEventList();
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED);

            isProjectCreatedEventApplied = true;
        }

        @Apply
        private void event(TaskAdded event) {
            isTaskAddedEventApplied = true;
        }

        @Apply
        private void event(ProjectStarted event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.STARTED);

            isProjectStartedEventApplied = true;
        }

        public void dispatchCommands(Message... commands) {
            for (Message cmd : commands) {
                dispatchForTest(cmd, COMMAND_CONTEXT);
            }
        }
    }

    /** Class only for test cases: exception if missing command handler or missing event applier. */
    private static class TestAggregateForCaseMissingHandlerOrApplier extends Aggregate<ProjectId, Project, Project.Builder> {

        private boolean isCreateProjectCommandHandled = false;

        public TestAggregateForCaseMissingHandlerOrApplier(ProjectId id) {
            super(id);
        }

        /** There is no event applier for ProjectCreated event (intentionally). */
        @Assign
        ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return Given.EventMessage.projectCreated(cmd.getProjectId(), cmd.getName());
        }
    }

    private static class TestAggregateWithIdInteger
            extends Aggregate<Integer, Project, Project.Builder> {
        private TestAggregateWithIdInteger(Integer id) {
            super(id);
        }
    }

    @Test
    public void increment_version_when_applying_state_changing_event() {
        final int version = aggregate.getVersion()
                                     .getNumber();
        // Dispatch two commands that cause events that modify aggregate state.
        aggregate.dispatchCommands(createProject, startProject);

        assertEquals(version + 2, aggregate.getVersion()
                                           .getNumber());
    }

    @Test
    public void record_modification_timestamp() throws InterruptedException {
        try {
            final Tests.BackToTheFuture provider = new Tests.BackToTheFuture();
            Timestamps2.setProvider(provider);

            Timestamp currentTime = Timestamps2.getCurrentTime();

            aggregate.dispatchCommands(createProject);

            assertEquals(currentTime, aggregate.whenModified());

            currentTime = provider.forward(10);

            aggregate.dispatchCommands(startProject);

            assertEquals(currentTime, aggregate.whenModified());
        } finally {
            Timestamps2.resetProvider();
        }
    }

    /** The class to check raising and catching exceptions. */
    @SuppressWarnings("unused")
    private static class FaultyAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        static final String BROKEN_HANDLER = "broken_handler";
        static final String BROKEN_APPLIER = "broken_applier";

        private final boolean brokenHandler;
        private final boolean brokenApplier;

        public FaultyAggregate(ProjectId id, boolean brokenHandler, boolean brokenApplier) {
            super(id);
            this.brokenHandler = brokenHandler;
            this.brokenApplier = brokenApplier;
        }

        @Assign
        ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            if (brokenHandler) {
                throw new IllegalStateException(BROKEN_HANDLER);
            }
            return Given.EventMessage.projectCreated(cmd.getProjectId(), cmd.getName());
        }

        @Apply
        private void event(ProjectCreated event) {
            if (brokenApplier) {
                throw new IllegalStateException(BROKEN_APPLIER);
            }

            final Project newState = newBuilder(getState())
                    .setId(event.getProjectId())
                    .build();

            incrementState(newState);
        }
    }

    @Test
    public void propagate_RuntimeException_when_handler_throws() {
        final FaultyAggregate faultyAggregate = new FaultyAggregate(ID, true, false);

        final Command command = Given.Command.createProject();
        try {
            faultyAggregate.dispatchForTest(command.getMessage(), command.getContext());
        } catch (RuntimeException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored") // We need it for checking.
            final Throwable cause = Throwables.getRootCause(e);
            assertTrue(cause instanceof IllegalStateException);
            assertEquals(FaultyAggregate.BROKEN_HANDLER, cause.getMessage());
        }
    }

    @Test
    public void propagate_RuntimeException_when_applier_throws() {
        final FaultyAggregate faultyAggregate = new FaultyAggregate(ID, false, true);

        final Command command = Given.Command.createProject();
        try {
            faultyAggregate.dispatchForTest(command.getMessage(), command.getContext());
        } catch (RuntimeException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored") // ... because we need it for checking.
            final Throwable cause = Throwables.getRootCause(e);
            assertTrue(cause instanceof IllegalStateException);
            assertEquals(FaultyAggregate.BROKEN_APPLIER, cause.getMessage());
        }
    }

    @Test
    public void propagate_RuntimeException_when_play_raises_exception() {
        final FaultyAggregate faultyAggregate = new FaultyAggregate(ID, false, true);
        try {
            faultyAggregate.play(AggregateEvents.newBuilder()
                                                .addEvent(Given.Event.projectCreated())
                                                .build());
        } catch (RuntimeException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored") // ... because we need it for checking.
            final Throwable cause = Throwables.getRootCause(e);
            assertTrue(cause instanceof IllegalStateException);
            assertEquals(FaultyAggregate.BROKEN_APPLIER, cause.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void do_not_allow_getting_state_builder_from_outside_the_event_applier() {
        new TestAggregateWithIdInteger(100).getBuilder();
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

    private static List<Event> getProjectEvents() {
        final List<Event> events = newLinkedList();
        events.add(Given.Event.projectCreated(ID, EVENT_CONTEXT.toBuilder()
                                                               .setVersion(2)
                                                               .build()));
        events.add(Given.Event.taskAdded(ID, EVENT_CONTEXT.toBuilder()
                                                          .setVersion(3)
                                                          .build()));
        events.add(Given.Event.projectStarted(ID, EVENT_CONTEXT.toBuilder()
                                                               .setVersion(4)
                                                               .build()));
        return events;
    }
}
