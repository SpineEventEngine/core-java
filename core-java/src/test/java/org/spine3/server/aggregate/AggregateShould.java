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
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.protobuf.ZoneOffsets;
import org.spine3.server.Assign;
import org.spine3.server.Snapshot;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;
import org.spine3.util.UserIds;

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
import static org.spine3.server.aggregate.Aggregate.getCommandClasses;
import static org.spine3.server.aggregate.Aggregate.getHandledMessageClasses;
import static org.spine3.server.aggregate.EventApplier.isEventApplierPredicate;
import static org.spine3.test.project.Project.getDefaultInstance;
import static org.spine3.test.project.Project.newBuilder;
import static org.spine3.testutil.ContextFactory.getEventContext;
import static org.spine3.util.Commands.createCommandContext;

@SuppressWarnings({"TypeMayBeWeakened", "InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber",
"ResultOfObjectAllocationIgnored", "ClassWithTooManyMethods", "ReturnOfNull", "DuplicateStringLiteralInspection"})
public class AggregateShould {

    private ProjectId projectId;
    private CommandContext commandContext;
    private EventContext eventContext;
    private CreateProject createProject;
    private AddTask addTask;
    private StartProject startProject;
    private ProjectRoot root;

    @Before
    public void setUp() {
        projectId = ProjectId.newBuilder().setId("project_id").build();
        commandContext = createCommandContext(UserIds.create("user_id"), ZoneOffsets.UTC);
        eventContext = getEventContext(0);
        createProject = CreateProject.newBuilder().setProjectId(projectId).build();
        addTask = AddTask.newBuilder().setProjectId(projectId).build();
        startProject = StartProject.newBuilder().setProjectId(projectId).build();
        root = new ProjectRoot(projectId);
    }

    @Test
    public void accept_Message_id_to_constructor() {
        try {
            final ProjectRoot r = new ProjectRoot(projectId);
            assertEquals(projectId, r.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_String_id_to_constructor() {
        try {
            final String id = "string_id";
            final TestRootWithIdString r = new TestRootWithIdString(id);
            assertEquals(id, r.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_Integer_id_to_constructor() {
        try {
            final Integer id = 12;
            final TestRootWithIdInteger r = new TestRootWithIdInteger(id);
            assertEquals(id, r.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_Long_id_to_constructor() {
        try {
            final Long id = 12L;
            final TestRootWithIdLong r = new TestRootWithIdLong(id);
            assertEquals(id, r.getId());
        } catch (Throwable e) {
            fail();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_to_constructor_id_of_unsupported_type() {
        new TestRootWithIdUnsupported(new UnsupportedClassVersionError());
    }

    @Test
    public void handle_one_command_and_apply_appropriate_event() throws InvocationTargetException {

        root.dispatch(createProject, commandContext);

        assertTrue(root.isCreateProjectCommandHandled);
        assertTrue(root.isProjectCreatedEventApplied);
    }

    @Test
    public void handle_only_appropriate_command() throws InvocationTargetException {

        root.dispatch(createProject, commandContext);

        assertTrue(root.isCreateProjectCommandHandled);
        assertTrue(root.isProjectCreatedEventApplied);

        assertFalse(root.isAddTaskCommandHandled);
        assertFalse(root.isTaskAddedEventApplied);

        assertFalse(root.isStartProjectCommandHandled);
        assertFalse(root.isProjectStartedEventApplied);
    }

    @Test
    public void handle_appropriate_commands_sequentially() throws InvocationTargetException {

        root.dispatch(createProject, commandContext);
        assertTrue(root.isCreateProjectCommandHandled);
        assertTrue(root.isProjectCreatedEventApplied);

        root.dispatch(addTask, commandContext);
        assertTrue(root.isAddTaskCommandHandled);
        assertTrue(root.isTaskAddedEventApplied);

        root.dispatch(startProject, commandContext);
        assertTrue(root.isStartProjectCommandHandled);
        assertTrue(root.isProjectStartedEventApplied);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_command_handler() throws InvocationTargetException {
        final TestRootForCaseMissingHandlerOrApplier r = new TestRootForCaseMissingHandlerOrApplier(projectId);
        r.dispatch(addTask, commandContext);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_missing_event_applier() throws InvocationTargetException {
        final TestRootForCaseMissingHandlerOrApplier r = new TestRootForCaseMissingHandlerOrApplier(projectId);
        try {
            r.dispatch(createProject, commandContext);
        } catch (IllegalStateException e) { // expected exception
            assertTrue(r.isCreateProjectCommandHandled);
            throw e;
        }
    }

    @Test
    public void return_command_classes_which_are_handled_by_root() {

        final Set<CommandClass> classes = getCommandClasses(ProjectRoot.class);

        assertTrue(classes.size() == 3);
        assertTrue(classes.contains(CommandClass.of(CreateProject.class)));
        assertTrue(classes.contains(CommandClass.of(AddTask.class)));
        assertTrue(classes.contains(CommandClass.of(StartProject.class)));
    }

    @Test
    public void return_message_classes_which_are_handled_by_root_case_event_classes() {

        final Set<Class<? extends Message>> classes = getHandledMessageClasses(ProjectRoot.class, isEventApplierPredicate);
        assertContainsAllProjectEvents(classes);
    }

    @Test
    public void return_default_state_by_default() throws InvocationTargetException {

        final Project state = root.getState();
        assertEquals(root.getDefaultState(), state);
    }

    @Test
    public void return_current_state_after_dispatch() throws InvocationTargetException {

        root.dispatch(createProject, commandContext);

        final Project state = root.getState();

        assertEquals(projectId, state.getProjectId());
        assertEquals(ProjectRoot.STATUS_NEW, state.getStatus());
    }

    @Test
    public void return_current_state_after_several_dispatches() throws InvocationTargetException {

        root.dispatch(createProject, commandContext);
        assertEquals(ProjectRoot.STATUS_NEW, root.getState().getStatus());

        root.dispatch(startProject, commandContext);
        assertEquals(ProjectRoot.STATUS_STARTED, root.getState().getStatus());
    }

    @Test
    public void return_non_null_time_when_was_last_modified() {

        final Timestamp creationTime = new ProjectRoot(projectId).whenLastModified();
        assertNotNull(creationTime);
    }

    @Test
    public void return_time_when_was_last_modified() throws InvocationTargetException {

        root.dispatch(createProject, commandContext);
        final long expectedTimeSec = currentTimeMillis() / 1000L;

        final Timestamp whenLastModified = root.whenLastModified();

        assertEquals(expectedTimeSec, whenLastModified.getSeconds());
    }

    @Test
    public void play_events() throws InvocationTargetException {

        final List<EventRecord> events = getProjectEventRecords();
        root.play(events);
        assertProjectEventsApplied(root);
    }

    @Test
    public void play_snapshot_event_and_restore_state() throws InvocationTargetException {

        root.dispatch(createProject, commandContext);

        final Snapshot snapshotNewProject = root.toSnapshot();

        root.dispatch(startProject, commandContext);
        assertEquals(ProjectRoot.STATUS_STARTED, root.getState().getStatus());

        final List<EventRecord> eventRecords = newArrayList(snapshotToEventRecord(snapshotNewProject));
        root.play(eventRecords);
        assertEquals(ProjectRoot.STATUS_NEW, root.getState().getStatus());
    }

    @Test
    public void not_return_any_uncommitted_event_records_by_default() throws InvocationTargetException {

        final List<EventRecord> events = root.getUncommittedEvents();
        assertTrue(events.isEmpty());
    }

    @Test
    public void return_uncommitted_event_records_after_dispatch() throws InvocationTargetException {

        dispatchAllProjectCommands(root);
        final List<EventRecord> events = root.getUncommittedEvents();
        assertContainsAllProjectEvents(eventRecordsToClasses(events));
    }

    @Test
    public void not_return_any_event_records_when_commit_by_default() throws InvocationTargetException {

        final List<EventRecord> events = root.commitEvents();
        assertTrue(events.isEmpty());
    }

    @Test
    public void return_event_records_when_commit_after_dispatch() throws InvocationTargetException {

        dispatchAllProjectCommands(root);
        final List<EventRecord> events = root.commitEvents();
        assertContainsAllProjectEvents(eventRecordsToClasses(events));
    }

    @Test
    public void clear_event_records_when_commit_after_dispatch() throws InvocationTargetException {

        dispatchAllProjectCommands(root);

        final List<EventRecord> events = root.commitEvents();
        assertFalse(events.isEmpty());

        final List<EventRecord> emptyList = root.commitEvents();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void transform_current_state_to_snapshot_event() throws InvocationTargetException, InvalidProtocolBufferException {

        root.dispatch(createProject, commandContext);

        final Snapshot snapshot = root.toSnapshot();
        final Project state = fromAny(snapshot.getState());

        assertEquals(projectId, state.getProjectId());
        assertEquals(ProjectRoot.STATUS_NEW, state.getStatus());
    }

    @Test
    public void restore_state_from_snapshot_event() throws InvocationTargetException {

        root.dispatch(createProject, commandContext);

        final Snapshot snapshotNewProject = root.toSnapshot();

        root.dispatch(startProject, commandContext);
        assertEquals(ProjectRoot.STATUS_STARTED, root.getState().getStatus());

        root.restore(snapshotNewProject);
        assertEquals(ProjectRoot.STATUS_NEW, root.getState().getStatus());
    }


    private void dispatchAllProjectCommands(Aggregate r) throws InvocationTargetException {
        r.dispatch(createProject, commandContext);
        r.dispatch(addTask, commandContext);
        r.dispatch(startProject, commandContext);
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
        List<EventRecord> events = newLinkedList();
        events.add(getProjectCreatedEventRecord());
        events.add(getTaskAddedEventRecord());
        events.add(getProjectStartedEventRecord());
        return events;
    }

    private void assertProjectEventsApplied(ProjectRoot r) {
        assertTrue(r.isProjectCreatedEventApplied);
        assertTrue(r.isTaskAddedEventApplied);
        assertTrue(r.isProjectStartedEventApplied);
    }

    private EventRecord getProjectCreatedEventRecord() {
        final Any event = toAny(ProjectCreated.newBuilder().setProjectId(projectId).build());
        return EventRecord.newBuilder().setContext(eventContext).setEvent(event).build();
    }

    private EventRecord getTaskAddedEventRecord() {
        final Any event = toAny(TaskAdded.newBuilder().setProjectId(projectId).build());
        return EventRecord.newBuilder().setContext(eventContext).setEvent(event).build();
    }

    private EventRecord getProjectStartedEventRecord() {
        final Any event = toAny(ProjectStarted.newBuilder().setProjectId(projectId).build());
        return EventRecord.newBuilder().setContext(eventContext).setEvent(event).build();
    }

    private EventRecord snapshotToEventRecord(Snapshot snapshot) {
        return EventRecord.newBuilder().setContext(eventContext).setEvent(toAny(snapshot)).build();
    }

    //TODO:2015-09-18:alexander.yevsyukov: Rename protobuf-generated class to ProjectState and this class to Project.
    public static class ProjectRoot extends Aggregate<ProjectId, Project> {

        private static final String STATUS_NEW = "NEW";
        private static final String STATUS_STARTED = "STARTED";

        private boolean isCreateProjectCommandHandled = false;
        private boolean isAddTaskCommandHandled = false;
        private boolean isStartProjectCommandHandled = false;

        private boolean isProjectCreatedEventApplied = false;
        private boolean isTaskAddedEventApplied = false;
        private boolean isProjectStartedEventApplied = false;

        public ProjectRoot(ProjectId id) {
            super(id);
        }

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

            Project newState = newBuilder(getState())
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

            Project newState = newBuilder(getState())
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
    public static class TestRootForCaseMissingHandlerOrApplier extends Aggregate<ProjectId, Project> {

        private boolean isCreateProjectCommandHandled = false;

        public TestRootForCaseMissingHandlerOrApplier(ProjectId id) {
            super(id);
        }

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

    public static class TestRootWithIdString extends Aggregate<String, Project> {
        protected TestRootWithIdString(String id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return null;
        }
    }

    public static class TestRootWithIdInteger extends Aggregate<Integer, Project> {
        protected TestRootWithIdInteger(Integer id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return null;
        }
    }

    public static class TestRootWithIdLong extends Aggregate<Long, Project> {
        protected TestRootWithIdLong(Long id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return null;
        }
    }

    public static class TestRootWithIdUnsupported extends Aggregate<UnsupportedClassVersionError, Project> {
        protected TestRootWithIdUnsupported(UnsupportedClassVersionError id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return null;
        }
    }
}
