/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.base.UserId;
import org.spine3.protobuf.ZoneOffsets;
import org.spine3.server.Assign;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.util.Classes;
import org.spine3.util.Users;
import org.spine3.util.testutil.AggregateIdFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.aggregate.EventApplier.isEventApplierPredicate;
import static org.spine3.test.project.Project.getDefaultInstance;
import static org.spine3.test.project.Project.newBuilder;
import static org.spine3.util.Commands.createContext;
import static org.spine3.util.testutil.ContextFactory.getEventContext;
import static org.spine3.util.testutil.EventRecordFactory.*;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"TypeMayBeWeakened", "InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber",
"ResultOfObjectAllocationIgnored", "ClassWithTooManyMethods", "ReturnOfNull", "DuplicateStringLiteralInspection"})
public class AggregateShould {

    private ProjectId projectId;
    private CommandContext commandContext;
    private EventContext eventContext;
    private CreateProject createProject;
    private AddTask addTask;
    private StartProject startProject;
    private ProjectAggregate aggregate;

    @Before
    public void setUp() {
        projectId = AggregateIdFactory.newProjectId();
        final UserId userId = Users.createId("user_id");
        commandContext = createContext(userId, ZoneOffsets.UTC);
        eventContext = getEventContext(userId, projectId);
        createProject = CreateProject.newBuilder().setProjectId(projectId).build();
        addTask = AddTask.newBuilder().setProjectId(projectId).build();
        startProject = StartProject.newBuilder().setProjectId(projectId).build();
        aggregate = new ProjectAggregate(projectId);
    }

    @Test
    public void accept_Message_id_to_constructor() {
        try {
            final ProjectAggregate a = new ProjectAggregate(projectId);
            assertEquals(projectId, a.getId());
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
    public void not_accept_to_constructor_id_of_unsupported_type() {
        new TestAggregateWithIdUnsupported(new UnsupportedClassVersionError());
    }

    @Test
    public void handle_one_command_and_apply_appropriate_event() throws InvocationTargetException {

        aggregate.dispatch(createProject, commandContext);

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);
    }

    @Test
    public void handle_only_appropriate_command() throws InvocationTargetException {

        aggregate.dispatch(createProject, commandContext);

        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        assertFalse(aggregate.isAddTaskCommandHandled);
        assertFalse(aggregate.isTaskAddedEventApplied);

        assertFalse(aggregate.isStartProjectCommandHandled);
        assertFalse(aggregate.isProjectStartedEventApplied);
    }

    @Test
    public void handle_appropriate_commands_sequentially() throws InvocationTargetException {

        aggregate.dispatch(createProject, commandContext);
        assertTrue(aggregate.isCreateProjectCommandHandled);
        assertTrue(aggregate.isProjectCreatedEventApplied);

        aggregate.dispatch(addTask, commandContext);
        assertTrue(aggregate.isAddTaskCommandHandled);
        assertTrue(aggregate.isTaskAddedEventApplied);

        aggregate.dispatch(startProject, commandContext);
        assertTrue(aggregate.isStartProjectCommandHandled);
        assertTrue(aggregate.isProjectStartedEventApplied);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_command_handler() throws InvocationTargetException {
        final TestAggregateForCaseMissingHandlerOrApplier r = new TestAggregateForCaseMissingHandlerOrApplier(projectId);
        r.dispatch(addTask, commandContext);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_event_applier() throws InvocationTargetException {
        final TestAggregateForCaseMissingHandlerOrApplier r = new TestAggregateForCaseMissingHandlerOrApplier(projectId);
        try {
            r.dispatch(createProject, commandContext);
        } catch (IllegalStateException e) { // expected exception
            assertTrue(r.isCreateProjectCommandHandled);
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

        final Set<Class<? extends Message>> classes = Classes.getHandledMessageClasses(ProjectAggregate.class, isEventApplierPredicate);
        assertContainsAllProjectEvents(classes);
    }

    @Test
    public void return_default_state_by_default() throws InvocationTargetException {

        final Project state = aggregate.getState();
        assertEquals(aggregate.getDefaultState(), state);
    }

    @Test
    public void return_current_state_after_dispatch() throws InvocationTargetException {

        aggregate.dispatch(createProject, commandContext);

        final Project state = aggregate.getState();

        assertEquals(projectId, state.getProjectId());
        assertEquals(ProjectAggregate.STATUS_NEW, state.getStatus());
    }

    @Test
    public void return_current_state_after_several_dispatches() throws InvocationTargetException {

        aggregate.dispatch(createProject, commandContext);
        assertEquals(ProjectAggregate.STATUS_NEW, aggregate.getState().getStatus());

        aggregate.dispatch(startProject, commandContext);
        assertEquals(ProjectAggregate.STATUS_STARTED, aggregate.getState().getStatus());
    }

    @Test
    public void return_non_null_time_when_was_last_modified() {

        final Timestamp creationTime = new ProjectAggregate(projectId).whenModified();
        assertNotNull(creationTime);
    }

    @Test
    public void return_time_when_was_last_modified() throws InvocationTargetException {

        aggregate.dispatch(createProject, commandContext);
        final long expectedTimeSec = currentTimeMillis() / 1000L;

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

        aggregate.dispatch(createProject, commandContext);

        final Snapshot snapshotNewProject = aggregate.toSnapshot();

        aggregate.dispatch(startProject, commandContext);
        assertEquals(ProjectAggregate.STATUS_STARTED, aggregate.getState().getStatus());

        final List<EventRecord> eventRecords = newArrayList(snapshotToEventRecord(snapshotNewProject));
        aggregate.play(eventRecords);
        assertEquals(ProjectAggregate.STATUS_NEW, aggregate.getState().getStatus());
    }

    @Test
    public void not_return_any_uncommitted_event_records_by_default() throws InvocationTargetException {

        final List<EventRecord> events = aggregate.getUncommittedEvents();
        assertTrue(events.isEmpty());
    }

    @Test
    public void return_uncommitted_event_records_after_dispatch() throws InvocationTargetException {

        dispatchAllProjectCommands(aggregate);
        final List<EventRecord> events = aggregate.getUncommittedEvents();
        assertContainsAllProjectEvents(eventRecordsToClasses(events));
    }

    @Test
    public void not_return_any_event_records_when_commit_by_default() throws InvocationTargetException {

        final List<EventRecord> events = aggregate.commitEvents();
        assertTrue(events.isEmpty());
    }

    @Test
    public void return_event_records_when_commit_after_dispatch() throws InvocationTargetException {

        dispatchAllProjectCommands(aggregate);
        final List<EventRecord> events = aggregate.commitEvents();
        assertContainsAllProjectEvents(eventRecordsToClasses(events));
    }

    @Test
    public void clear_event_records_when_commit_after_dispatch() throws InvocationTargetException {

        dispatchAllProjectCommands(aggregate);

        final List<EventRecord> events = aggregate.commitEvents();
        assertFalse(events.isEmpty());

        final List<EventRecord> emptyList = aggregate.commitEvents();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void transform_current_state_to_snapshot_event() throws InvocationTargetException, InvalidProtocolBufferException {

        aggregate.dispatch(createProject, commandContext);

        final Snapshot snapshot = aggregate.toSnapshot();
        final Project state = fromAny(snapshot.getState());

        assertEquals(projectId, state.getProjectId());
        assertEquals(ProjectAggregate.STATUS_NEW, state.getStatus());
    }

    @Test
    public void restore_state_from_snapshot_event() throws InvocationTargetException {

        aggregate.dispatch(createProject, commandContext);

        final Snapshot snapshotNewProject = aggregate.toSnapshot();

        aggregate.dispatch(startProject, commandContext);
        assertEquals(ProjectAggregate.STATUS_STARTED, aggregate.getState().getStatus());

        aggregate.restore(snapshotNewProject);
        assertEquals(ProjectAggregate.STATUS_NEW, aggregate.getState().getStatus());
    }


    private void dispatchAllProjectCommands(Aggregate a) throws InvocationTargetException {
        a.dispatch(createProject, commandContext);
        a.dispatch(addTask, commandContext);
        a.dispatch(startProject, commandContext);
    }

    private Collection<Class<? extends Message>> eventRecordsToClasses(Collection<EventRecord> events) {
        return transform(events, new Function<EventRecord, Class<? extends Message>>() {
            @SuppressWarnings("NullableProblems")
            @Override
            public Class<? extends Message> apply(EventRecord record) {
                return fromAny(record.getEvent()).getClass();
            }
        });
    }

    private void assertContainsAllProjectEvents(Collection<Class<? extends Message>> classes) {
        assertEquals(3, classes.size());
        assertTrue(classes.contains(ProjectCreated.class));
        assertTrue(classes.contains(TaskAdded.class));
        assertTrue(classes.contains(ProjectStarted.class));
    }

    private List<EventRecord> getProjectEventRecords() {
        final List<EventRecord> events = newLinkedList();
        events.add(projectCreated(projectId, eventContext));
        events.add(taskAdded(projectId, eventContext));
        events.add(projectStarted(projectId, eventContext));
        return events;
    }

    private void assertProjectEventsApplied(ProjectAggregate a) {
        assertTrue(a.isProjectCreatedEventApplied);
        assertTrue(a.isTaskAddedEventApplied);
        assertTrue(a.isProjectStartedEventApplied);
    }

    private EventRecord snapshotToEventRecord(Snapshot snapshot) {
        return EventRecord.newBuilder().setContext(eventContext).setEvent(toAny(snapshot)).build();
    }

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

        @Nonnull
        @Override
        protected Project getDefaultState() {
            return getDefaultInstance();
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return ProjectCreated.newBuilder().setProjectId(cmd.getProjectId()).build();
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            return TaskAdded.newBuilder().setProjectId(cmd.getProjectId()).build();
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            final ProjectStarted message = ProjectStarted.newBuilder().setProjectId(cmd.getProjectId()).build();
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
    }

    /*
     * Class only for test cases: missing command handler; missing event applier
     */
    public static class TestAggregateForCaseMissingHandlerOrApplier extends Aggregate<ProjectId, Project> {

        private boolean isCreateProjectCommandHandled = false;

        public TestAggregateForCaseMissingHandlerOrApplier(ProjectId id) {
            super(id);
        }

        @Nonnull
        @Override
        protected Project getDefaultState() {
            return getDefaultInstance();
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return ProjectCreated.newBuilder().setProjectId(cmd.getProjectId()).build();
        }
    }

    public static class TestAggregateWithIdString extends Aggregate<String, Project> {
        protected TestAggregateWithIdString(String id) {
            super(id);
        }
        @Nonnull
        @Override protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class TestAggregateWithIdInteger extends Aggregate<Integer, Project> {
        protected TestAggregateWithIdInteger(Integer id) {
            super(id);
        }
        @Nonnull
        @Override protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class TestAggregateWithIdLong extends Aggregate<Long, Project> {
        protected TestAggregateWithIdLong(Long id) {
            super(id);
        }
        @Nonnull
        @Override protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class TestAggregateWithIdUnsupported extends Aggregate<UnsupportedClassVersionError, Project> {
        protected TestAggregateWithIdUnsupported(UnsupportedClassVersionError id) {
            super(id);
        }
        @Nonnull
        @Override protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }
}
