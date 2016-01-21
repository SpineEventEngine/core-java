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
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.base.UserId;
import org.spine3.server.Assign;
import org.spine3.server.util.Classes;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.testdata.TestCommandFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static org.junit.Assert.*;
import static org.spine3.client.Commands.createContext;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.aggregate.EventApplier.IS_EVENT_APPLIER;
import static org.spine3.test.project.Project.getDefaultInstance;
import static org.spine3.test.project.Project.newBuilder;
import static org.spine3.testdata.TestAggregateIdFactory.createProjectId;
import static org.spine3.testdata.TestCommandFactory.createProject;
import static org.spine3.testdata.TestCommandFactory.startProject;
import static org.spine3.testdata.TestContextFactory.createEventContext;
import static org.spine3.testdata.TestEventFactory.*;
import static org.spine3.testdata.TestEventRecordFactory.*;
import static org.spine3.time.ZoneOffsets.UTC;
import static org.spine3.util.Tests.currentTimeSeconds;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"TypeMayBeWeakened", "InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public class AggregateShould {

    private static final ProjectId PROJECT_ID = createProjectId("testProjectId");
    private final ProjectAggregate aggregate = new ProjectAggregate(PROJECT_ID);

    private final CreateProject createProject = createProject(PROJECT_ID);
    private final AddTask addTask = TestCommandFactory.addTask(PROJECT_ID);
    private final StartProject startProject = startProject(PROJECT_ID);

    private static final UserId USER_ID = newUserId("user_id_test");
    private static final CommandContext COMMAND_CONTEXT = createContext(USER_ID, UTC);
    private static final EventContext EVENT_CONTEXT = createEventContext(PROJECT_ID);

    @Test
    public void accept_Message_id_to_constructor() {
        try {
            final ProjectAggregate a = new ProjectAggregate(PROJECT_ID);
            assertEquals(PROJECT_ID, a.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_String_id_to_constructor() {
        try {
            final String id = "string_id";
            final TestAggregateWithIdString a = new TestAggregateWithIdString(id);
            assertEquals(id, a.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_Integer_id_to_constructor() {
        try {
            final Integer id = 12;
            final TestAggregateWithIdInteger a = new TestAggregateWithIdInteger(id);
            assertEquals(id, a.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_Long_id_to_constructor() {
        try {
            final Long id = 12L;
            final TestAggregateWithIdLong a = new TestAggregateWithIdLong(id);
            assertEquals(id, a.getId());
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
    public void handle_one_command_and_apply_appropriate_event() throws InvocationTargetException {

        aggregate.dispatch(createProject, COMMAND_CONTEXT);

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);
    }

    @Test
    public void handle_only_appropriate_command() throws InvocationTargetException {

        aggregate.dispatch(createProject, COMMAND_CONTEXT);

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        assertFalse(aggregate.isAddTaskCommandHandled);
        assertFalse(aggregate.isTaskAddedEventApplied);

        assertFalse(aggregate.isStartProjectCommandHandled);
        assertFalse(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void handle_appropriate_commands_sequentially() throws InvocationTargetException {

        aggregate.dispatch(createProject, COMMAND_CONTEXT);
        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        aggregate.dispatch(addTask, COMMAND_CONTEXT);
        assertTrue(aggregate.isAddTaskCommandHandled);
        assertTrue(aggregate.isTaskAddedEventApplied);

        aggregate.dispatch(startProject, COMMAND_CONTEXT);
        assertTrue(aggregate.isStartProjectCommandHandled);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_command_handler() throws InvocationTargetException {

        final TestAggregateForCaseMissingHandlerOrApplier r = new TestAggregateForCaseMissingHandlerOrApplier(PROJECT_ID);
        r.dispatch(addTask, COMMAND_CONTEXT);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_event_applier_for_non_state_neutral_event() throws InvocationTargetException {
        final TestAggregateForCaseMissingHandlerOrApplier r = new TestAggregateForCaseMissingHandlerOrApplier(PROJECT_ID);
        try {
            r.dispatch(createProject, COMMAND_CONTEXT);
        } catch (IllegalStateException e) { // expected exception
            assertTrue(r.isCreateProjectCommandHandled);
            throw e;
        }
    }

    @Test
    public void not_throw_exception_if_missing_event_applier_for_state_neutral_event() throws InvocationTargetException {
        final TestAggregateWithStateNeutralEvents r = new TestAggregateWithStateNeutralEvents(PROJECT_ID);
        try {
            r.dispatch(addTask, COMMAND_CONTEXT);
            assertTrue(r.isTaskAddedCommandHandled);
        } catch (IllegalStateException e) {
            fail("Method must not throw 'missing event applier exception' because this event is state neutral and " +
                    "the applier is not required.");
            throw e;
        }
    }

    @Test
    public void return_command_classes_which_are_handled_by_aggregate() {

        final Set<Class<? extends Message>> classes = Aggregate.getCommandClasses(ProjectAggregate.class);

        assertTrue(classes.size() == 3);
        assertTrue(classes.contains(CreateProject.class));
        assertTrue(classes.contains(AddTask.class));
        assertTrue(classes.contains(StartProject.class));
    }

    @Test
    public void return_message_classes_which_are_handled_by_aggregate_case_event_classes() {
        final Set<Class<? extends Message>> classes = Classes.getHandledMessageClasses(ProjectAggregate.class, IS_EVENT_APPLIER);

        assertContains(classes,
                ProjectCreated.class, TaskAdded.class, ProjectStarted.class);
    }

    @Test
    public void return_default_state_by_default() {

        final Project state = aggregate.getState();
        assertEquals(aggregate.getDefaultState(), state);
    }

    @Test
    public void return_current_state_after_dispatch() throws InvocationTargetException {

        aggregate.dispatch(createProject, COMMAND_CONTEXT);

        final Project state = aggregate.getState();

        assertEquals(PROJECT_ID, state.getProjectId());
        assertEquals(ProjectAggregate.STATUS_NEW, state.getStatus());
    }

    @Test
    public void return_current_state_after_several_dispatches() throws InvocationTargetException {

        aggregate.dispatch(createProject, COMMAND_CONTEXT);
        assertEquals(ProjectAggregate.STATUS_NEW, aggregate.getState().getStatus());

        aggregate.dispatch(startProject, COMMAND_CONTEXT);
        assertEquals(ProjectAggregate.STATUS_STARTED, aggregate.getState().getStatus());
    }

    @Test
    public void return_non_null_time_when_was_last_modified() {

        final Timestamp creationTime = new ProjectAggregate(PROJECT_ID).whenModified();
        assertNotNull(creationTime);
    }

    @Test
    public void return_time_when_was_last_modified() throws InvocationTargetException {

        aggregate.dispatch(createProject, COMMAND_CONTEXT);
        final long expectedTimeSec = currentTimeSeconds();

        final Timestamp whenLastModified = aggregate.whenModified();

        assertEquals(expectedTimeSec, whenLastModified.getSeconds());
    }

    @Test
    public void play_events() throws InvocationTargetException {

        final List<EventRecord> events = getProjectEventRecords();
        aggregate.play(events);
        assertProjectEventsApplied(aggregate);
    }

    @Test
    public void play_snapshot_event_and_restore_state() throws InvocationTargetException {

        aggregate.dispatch(createProject, COMMAND_CONTEXT);

        final Snapshot snapshotNewProject = aggregate.toSnapshot();

        aggregate.dispatch(startProject, COMMAND_CONTEXT);
        assertEquals(ProjectAggregate.STATUS_STARTED, aggregate.getState().getStatus());

        final List<EventRecord> eventRecords = newArrayList(snapshotToEventRecord(snapshotNewProject));
        aggregate.play(eventRecords);
        assertEquals(ProjectAggregate.STATUS_NEW, aggregate.getState().getStatus());
    }

    @Test
    public void not_return_any_uncommitted_event_records_by_default() {

        final List<EventRecord> events = aggregate.getUncommittedEvents();
        assertTrue(events.isEmpty());
    }

    @Test
    public void return_uncommitted_event_records_after_dispatch() throws InvocationTargetException {
        aggregate.dispatchCommands(createProject, addTask, startProject);

        final List<EventRecord> events = aggregate.getUncommittedEvents();

        assertContains(eventRecordsToClasses(events),
                ProjectCreated.class, TaskAdded.class, ProjectStarted.class);
    }

    @Test
    public void return_only_state_changing_uncommitted_event_records_after_dispatch() throws InvocationTargetException {
        final TestAggregateWithStateNeutralEvents aggregate = new TestAggregateWithStateNeutralEvents(PROJECT_ID);

        aggregate.dispatchCommands(createProject, addTask);

        final Collection<EventRecord> events = aggregate.getStateChangingUncommittedEvents();
        assertContains(eventRecordsToClasses(events), ProjectCreated.class);
    }

    @Test
    public void not_return_any_event_records_when_commit_by_default() {

        final List<EventRecord> events = aggregate.commitEvents();
        assertTrue(events.isEmpty());
    }

    @Test
    public void return_event_records_when_commit_after_dispatch() throws InvocationTargetException {
        aggregate.dispatchCommands(createProject, addTask, startProject);

        final List<EventRecord> events = aggregate.commitEvents();

        assertContains(eventRecordsToClasses(events),
                ProjectCreated.class, TaskAdded.class, ProjectStarted.class);
    }

    @Test
    public void clear_event_records_when_commit_after_dispatch() throws InvocationTargetException {
        aggregate.dispatchCommands(createProject, addTask, startProject);

        final List<EventRecord> events = aggregate.commitEvents();
        assertFalse(events.isEmpty());

        final List<EventRecord> emptyList = aggregate.commitEvents();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void transform_current_state_to_snapshot_event() throws InvocationTargetException {

        aggregate.dispatch(createProject, COMMAND_CONTEXT);

        final Snapshot snapshot = aggregate.toSnapshot();
        final Project state = fromAny(snapshot.getState());

        assertEquals(PROJECT_ID, state.getProjectId());
        assertEquals(ProjectAggregate.STATUS_NEW, state.getStatus());
    }

    @Test
    public void restore_state_from_snapshot_event() throws InvocationTargetException {

        aggregate.dispatch(createProject, COMMAND_CONTEXT);

        final Snapshot snapshotNewProject = aggregate.toSnapshot();

        aggregate.dispatch(startProject, COMMAND_CONTEXT);
        assertEquals(ProjectAggregate.STATUS_STARTED, aggregate.getState().getStatus());

        aggregate.restore(snapshotNewProject);
        assertEquals(ProjectAggregate.STATUS_NEW, aggregate.getState().getStatus());
    }

    private static Collection<Class<? extends Message>> eventRecordsToClasses(Collection<EventRecord> events) {

        return transform(events, new Function<EventRecord, Class<? extends Message>>() {
            @Nullable // return null because an exception won't be propagated in this case
            @Override
            public Class<? extends Message> apply(@Nullable EventRecord record) {
                if (record == null) {
                    return null;
                }
                return fromAny(record.getEvent()).getClass();
            }
        });
    }

    private static void assertContains(Collection<Class<? extends Message>> actualClasses,
                                       Class... expectedClasses) {
        assertTrue(actualClasses.containsAll(newArrayList(expectedClasses)));
        assertEquals(expectedClasses.length, actualClasses.size());
    }

    private static List<EventRecord> getProjectEventRecords() {
        final List<EventRecord> events = newLinkedList();
        events.add(projectCreated(PROJECT_ID, EVENT_CONTEXT));
        events.add(taskAdded(PROJECT_ID, EVENT_CONTEXT));
        events.add(projectStarted(PROJECT_ID, EVENT_CONTEXT));
        return events;
    }

    private static void assertProjectEventsApplied(ProjectAggregate a) {

        assertTrue(a.isProjectCreatedEventApplied);
        assertTrue(a.isTaskAddedEventApplied);
        assertTrue(a.isProjectStartedEventApplied);
    }

    private static EventRecord snapshotToEventRecord(Snapshot snapshot) {
        return EventRecord.newBuilder().setContext(EVENT_CONTEXT).setEvent(toAny(snapshot)).build();
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class ProjectAggregate extends Aggregate<ProjectId, Project> {

        private static final String STATUS_NEW = "NEW";
        private static final String STATUS_STARTED = "STARTED";

        private boolean isCreateProjectCommandHandled = false;
        private boolean isAddTaskCommandHandled = false;
        private boolean isStartProjectCommandHandled = false;

        private boolean isProjectCreatedEventApplied = false;
        private boolean isTaskAddedEventApplied = false;
        private boolean isProjectStartedEventApplied = false;

        public ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Override
        protected Project getDefaultState() {
            return getDefaultInstance();
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return projectCreatedEvent(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            return taskAddedEvent(cmd.getProjectId());
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            final ProjectStarted message = projectStartedEvent(cmd.getProjectId());
            return newArrayList(message);
        }

        @Apply
        private void event(ProjectCreated event) {

            final Project newState = newBuilder(getState())
                    .setProjectId(event.getProjectId())
                    .setStatus(STATUS_NEW)
                    .build();

            incrementState(newState);

            isProjectCreatedEventApplied = true;
        }

        @Apply
        private void event(TaskAdded event) {
            isTaskAddedEventApplied = true;
        }

        @Apply
        private void event(ProjectStarted event) {

            final Project newState = newBuilder(getState())
                    .setProjectId(event.getProjectId())
                    .setStatus(STATUS_STARTED)
                    .build();

            incrementState(newState);

            isProjectStartedEventApplied = true;
        }

        public void dispatchCommands(Message... commands) throws InvocationTargetException {
            for (Message cmd : commands) {
                dispatch(cmd, COMMAND_CONTEXT);
            }
        }
    }

    /*
     * Class only for test cases: exception if missing command handler or missing event applier.
     */
    public static class TestAggregateForCaseMissingHandlerOrApplier extends Aggregate<ProjectId, Project> {

        private boolean isCreateProjectCommandHandled = false;

        public TestAggregateForCaseMissingHandlerOrApplier(ProjectId id) {
            super(id);
        }

        @Override
        protected Project getDefaultState() {
            return getDefaultInstance();
        }

        /**
         * There is no event applier for ProjectCreated event (intentionally).
         */
        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return projectCreatedEvent(cmd.getProjectId());
        }
    }

    /*
     * Class only for test case: applier is not required for state-neutral event.
     */
    public static class TestAggregateWithStateNeutralEvents extends Aggregate<ProjectId, Project> {

        private static final ImmutableSet<Class<? extends Message>> STATE_NEUTRAL_EVENT_CLASSES =
                ImmutableSet.<Class<? extends Message>>of(TaskAdded.class);

        private boolean isTaskAddedCommandHandled = false;

        public TestAggregateWithStateNeutralEvents(ProjectId id) {
            super(id);
        }

        @Override
        protected Project getDefaultState() {
            return getDefaultInstance();
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            return projectCreatedEvent(cmd.getProjectId());
        }

        @Apply
        private void event(ProjectCreated event) {
        }

        /**
         * There is no event applier for TaskAdded event because this event is state-neutral.
         */
        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            isTaskAddedCommandHandled = true;
            return taskAddedEvent(cmd.getProjectId());
        }

        @Override
        @SuppressWarnings("RefusedBequest") // the method from superclass returns nothing
        protected Set<Class<? extends Message>> getStateNeutralEventClasses() {
            return STATE_NEUTRAL_EVENT_CLASSES;
        }

        public void dispatchCommands(Message... commands) throws InvocationTargetException {
            for (Message cmd : commands) {
                dispatch(cmd, COMMAND_CONTEXT);
            }
        }
    }

    public static class TestAggregateWithIdString extends Aggregate<String, Project> {
        protected TestAggregateWithIdString(String id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class TestAggregateWithIdInteger extends Aggregate<Integer, Project> {
        protected TestAggregateWithIdInteger(Integer id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class TestAggregateWithIdLong extends Aggregate<Long, Project> {
        protected TestAggregateWithIdLong(Long id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class TestAggregateWithIdUnsupported extends Aggregate<UnsupportedClassVersionError, Project> {
        protected TestAggregateWithIdUnsupported(UnsupportedClassVersionError id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }
}
