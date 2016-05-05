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

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.command.Assign;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.testdata.TestAggregateIdFactory;
import org.spine3.testdata.TestCommands;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.test.Tests.currentTimeSeconds;
import static org.spine3.test.project.Project.newBuilder;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;
import static org.spine3.testdata.TestEventFactory.*;
import static org.spine3.testdata.TestEventMessageFactory.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"TypeMayBeWeakened", "InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public class AggregateShould {

    private static final ProjectId ID = TestAggregateIdFactory.newProjectId();

    private static final CommandContext COMMAND_CONTEXT = createCommandContext();
    private static final EventContext EVENT_CONTEXT = createEventContext(ID);

    private final CreateProject createProject = TestCommands.createProjectMsg(ID);
    private final AddTask addTask = TestCommands.addTaskMsg(ID);
    private final StartProject startProject = TestCommands.startProjectMsg(ID);

    private TestAggregate aggregate;

    @Before
    public void setUpTest() {
        aggregate = new TestAggregate(ID);
    }

    @Test
    public void accept_Message_id_to_constructor() {
        try {
            final TestAggregate aggregate = new TestAggregate(ID);
            assertEquals(ID, aggregate.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_String_id_to_constructor() {
        try {
            final String id = "string_id";
            final TestAggregateWithIdString aggregate = new TestAggregateWithIdString(id);
            assertEquals(id, aggregate.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_Integer_id_to_constructor() {
        try {
            final Integer id = 12;
            final TestAggregateWithIdInteger aggregate = new TestAggregateWithIdInteger(id);
            assertEquals(id, aggregate.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_Long_id_to_constructor() {
        try {
            final Long id = 12L;
            final TestAggregateWithIdLong aggregate = new TestAggregateWithIdLong(id);
            assertEquals(id, aggregate.getId());
        } catch (Throwable e) {
            fail();
        }
    }


    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void not_accept_to_constructor_id_of_unsupported_type() {
        new TestAggregateWithIdUnsupported(new UnsupportedClassVersionError());
    }

    @Test
    public void handle_one_command_and_apply_appropriate_event() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);
    }

    @Test
    public void handle_only_appropriate_command() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        assertFalse(aggregate.isAddTaskCommandHandled);
        assertFalse(aggregate.isTaskAddedEventApplied);

        assertFalse(aggregate.isStartProjectCommandHandled);
        assertFalse(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void handle_appropriate_commands_sequentially() {
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
        final Set<Class<? extends Message>> classes = Aggregate.getCommandClasses(TestAggregate.class);

        assertTrue(classes.size() == 3);
        assertTrue(classes.contains(CreateProject.class));
        assertTrue(classes.contains(AddTask.class));
        assertTrue(classes.contains(StartProject.class));
    }

    @Test
    public void return_event_classes_which_represent_the_state_of_the_aggregate() {
        final Set<Class<? extends Message>> classes = Aggregate.getEventClasses(TestAggregate.class);

        assertContains(classes,
                ProjectCreated.class, TaskAdded.class, ProjectStarted.class);
    }

    @Test
    public void return_default_state_by_default() {
        final Project state = aggregate.getState();

        assertEquals(aggregate.getDefaultState(), state);
    }

    @Test
    public void return_current_state_after_dispatch() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        final Project state = aggregate.getState();

        assertEquals(ID, state.getProjectId());
        assertEquals(TestAggregate.STATUS_NEW, state.getStatus());
    }

    @Test
    public void return_current_state_after_several_dispatches() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);
        assertEquals(TestAggregate.STATUS_NEW, aggregate.getState().getStatus());

        aggregate.dispatchForTest(startProject, COMMAND_CONTEXT);
        assertEquals(TestAggregate.STATUS_STARTED, aggregate.getState().getStatus());
    }

    @Test
    public void return_non_null_time_when_was_last_modified() {
        final Timestamp creationTime = new TestAggregate(ID).whenModified();
        assertNotNull(creationTime);
    }

    @Test
    public void return_time_when_was_last_modified() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);
        final long expectedTimeSec = currentTimeSeconds();

        final Timestamp whenLastModified = aggregate.whenModified();

        assertEquals(expectedTimeSec, whenLastModified.getSeconds());
    }

    @Test
    public void play_events() {
        final List<Event> events = getProjectEvents();
        
        aggregate.play(events);
        
        assertProjectEventsApplied(aggregate);
    }

    @Test
    public void play_snapshot_event_and_restore_state() {
        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        final Snapshot snapshotNewProject = aggregate.toSnapshot();

        aggregate.dispatchForTest(startProject, COMMAND_CONTEXT);
        assertEquals(TestAggregate.STATUS_STARTED, aggregate.getState().getStatus());

        final List<Event> events = newArrayList(snapshotToEvent(snapshotNewProject));
        aggregate.play(events);
        assertEquals(TestAggregate.STATUS_NEW, aggregate.getState().getStatus());
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
        final Project state = fromAny(snapshot.getState());

        assertEquals(ID, state.getProjectId());
        assertEquals(TestAggregate.STATUS_NEW, state.getStatus());
    }

    @Test
    public void restore_state_from_snapshot() {

        aggregate.dispatchForTest(createProject, COMMAND_CONTEXT);

        final Snapshot snapshotNewProject = aggregate.toSnapshot();

        aggregate.dispatchForTest(startProject, COMMAND_CONTEXT);
        assertEquals(TestAggregate.STATUS_STARTED, aggregate.getState().getStatus());

        aggregate.restore(snapshotNewProject);
        assertEquals(TestAggregate.STATUS_NEW, aggregate.getState().getStatus());
    }

    private static Collection<Class<? extends Message>> eventsToClasses(Collection<Event> events) {
        return transform(events, new Function<Event, Class<? extends Message>>() {
            @Nullable // return null because an exception won't be propagated in this case
            @Override
            public Class<? extends Message> apply(@Nullable Event record) {
                if (record == null) {
                    return null;
                }
                return fromAny(record.getMessage()).getClass();
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
        events.add(projectCreatedEvent(ID, EVENT_CONTEXT));
        events.add(taskAddedEvent(ID, EVENT_CONTEXT));
        events.add(projectStartedEvent(ID, EVENT_CONTEXT));
        return events;
    }
    
    private static void assertProjectEventsApplied(TestAggregate a) {
        assertTrue(a.isProjectCreatedEventApplied);
        assertTrue(a.isTaskAddedEventApplied);
        assertTrue(a.isProjectStartedEventApplied);
    }

    private static Event snapshotToEvent(Snapshot snapshot) {
        return Event.newBuilder().setContext(EVENT_CONTEXT).setMessage(toAny(snapshot)).build();
    }

    @SuppressWarnings("unused")
    private static class TestAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        private static final String STATUS_NEW = "NEW";
        private static final String STATUS_STARTED = "STARTED";

        private boolean isCreateProjectCommandHandled = false;
        private boolean isAddTaskCommandHandled = false;
        private boolean isStartProjectCommandHandled = false;

        private boolean isProjectCreatedEventApplied = false;
        private boolean isTaskAddedEventApplied = false;
        private boolean isProjectStartedEventApplied = false;

        public TestAggregate(ProjectId id) {
            super(id);
        }

        @Override
        protected Project getDefaultState() {
            return super.getDefaultState();
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return projectCreatedMsg(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            return taskAddedMsg(cmd.getProjectId());
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            final ProjectStarted message = projectStartedMsg(cmd.getProjectId());
            return newArrayList(message);
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setProjectId(event.getProjectId())
                    .setStatus(STATUS_NEW);

            isProjectCreatedEventApplied = true;
        }

        @Apply
        private void event(TaskAdded event) {
            isTaskAddedEventApplied = true;
        }

        @Apply
        private void event(ProjectStarted event) {
            getBuilder()
                    .setProjectId(event.getProjectId())
                    .setStatus(STATUS_STARTED);

            isProjectStartedEventApplied = true;
        }

        public void dispatchCommands(Message... commands) {
            for (Message cmd : commands) {
                dispatchForTest(cmd, COMMAND_CONTEXT);
            }
        }
    }

    /*
     * Class only for test cases: exception if missing command handler or missing event applier.
     */
    private static class TestAggregateForCaseMissingHandlerOrApplier extends Aggregate<ProjectId, Project, Project.Builder> {

        private boolean isCreateProjectCommandHandled = false;

        public TestAggregateForCaseMissingHandlerOrApplier(ProjectId id) {
            super(id);
        }

        /**
         * There is no event applier for ProjectCreated event (intentionally).
         */
        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return projectCreatedMsg(cmd.getProjectId());
        }
    }

    private static class TestAggregateWithIdString extends Aggregate<String, Project, Project.Builder> {
        private TestAggregateWithIdString(String id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdInteger extends Aggregate<Integer, Project, Project.Builder> {
        private TestAggregateWithIdInteger(Integer id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdLong extends Aggregate<Long, Project, Project.Builder> {
        private TestAggregateWithIdLong(Long id) {
            super(id);
        }
    }

    private static class TestAggregateWithIdUnsupported extends Aggregate<UnsupportedClassVersionError, Project, Project.Builder> {
        private TestAggregateWithIdUnsupported(UnsupportedClassVersionError id) {
            super(id);
        }
    }


    @Test
    public void increment_version_when_applying_state_changing_event() {
        final int version = aggregate.getVersion();
        // Dispatch two commands that cause events that modify aggregate state.
        aggregate.dispatchCommands(createProject, startProject);

        assertEquals(version + 2, aggregate.getVersion());
    }

    @Test
    public void record_modification_timestamp() throws InterruptedException {
        final Timestamp start = aggregate.whenModified();
        aggregate.dispatchCommands(createProject);
        Thread.sleep(100);

        final Timestamp afterCreate = aggregate.whenModified();
        assertTrue(Timestamps.isAfter(afterCreate, start));
        Thread.sleep(100);

        aggregate.dispatchCommands(startProject);
        Thread.sleep(100);

        final Timestamp afterStart = aggregate.whenModified();
        assertTrue(Timestamps.isAfter(afterStart, afterCreate));
    }

    /**
     * The class to check raising and catching exceptions.
     */
    @SuppressWarnings("unused")
    private static class FaultyAggregate extends Aggregate<ProjectId, Project, Project.Builder>  {

        /* package */ static final String BROKEN_HANDLER = "broken_handler";
        /* package */ static final String BROKEN_APPLIER = "broken_applier";

        private final boolean brokenHandler;
        private final boolean brokenApplier;

        public FaultyAggregate(ProjectId id, boolean brokenHandler, boolean brokenApplier) {
            super(id);
            this.brokenHandler = brokenHandler;
            this.brokenApplier = brokenApplier;
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            if (brokenHandler) {
                throw new IllegalStateException(BROKEN_HANDLER);
            }
            return projectCreatedMsg(cmd.getProjectId());
        }

        @Apply
        private void event(ProjectCreated event) {
            if (brokenApplier) {
                throw new IllegalStateException(BROKEN_APPLIER);
            }

            final Project newState = newBuilder(getState())
                    .setProjectId(event.getProjectId())
                    .build();

            incrementState(newState);
        }
    }

    @Test
    public void propagate_RuntimeException_when_handler_throws() {
        final FaultyAggregate faultyAggregate = new FaultyAggregate(ID, true, false);

        final Command command = TestCommands.createProjectCmd();
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

        final Command command = TestCommands.createProjectCmd();
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
            faultyAggregate.play(ImmutableList.of(projectCreatedEvent()));
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
}
