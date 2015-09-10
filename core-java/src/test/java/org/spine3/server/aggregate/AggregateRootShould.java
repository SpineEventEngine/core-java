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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.CommandClass;
import org.spine3.EventClass;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.base.UserId;
import org.spine3.server.Assign;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.AddTask;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.test.project.event.TaskAdded;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newLinkedList;
import static junit.framework.Assert.*;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.server.aggregate.AggregateRoot.*;
import static org.spine3.server.internal.CommandHandler.isCommandHandlerPredicate;
import static org.spine3.testutil.ContextFactory.getCommandContext;
import static org.spine3.testutil.ContextFactory.getEventContext;

@SuppressWarnings({"TypeMayBeWeakened", "InstanceMethodNamingConvention", "MethodMayBeStatic", "StaticNonFinalField",
"ResultOfObjectAllocationIgnored", "MagicNumber", "ClassWithTooManyMethods", "ReturnOfNull"})
public class AggregateRootShould {

    private static final ProjectId PROJECT_ID;
    private static final UserId USER_ID;
    private static final CommandContext COMMAND_CONTEXT;
    private static final EventContext EVENT_CONTEXT;
    private ProjectRoot root;

    static {
        PROJECT_ID = ProjectId.newBuilder().setId("project_1").build();
        USER_ID = UserId.newBuilder().setValue("user_1").build();
        COMMAND_CONTEXT = getCommandContext(USER_ID);
        EVENT_CONTEXT = getEventContext(0);
    }

    @Before
    public void setUp() {
        root = new ProjectRoot(PROJECT_ID);
    }

    @Test
    public void accept_to_constructor_id_of_type_message() {
        try {
            new ProjectRoot(PROJECT_ID);
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_to_constructor_id_of_type_string() {
        try {
            new TestRootWithIdString("string");
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_to_constructor_id_of_type_integer() {
        try {
            new TestRootWithIdInteger(12);
        } catch (Throwable e) {
            fail();
        }
    }

    @Test
    public void accept_to_constructor_id_of_type_long() {
        try {
            new TestRootWithIdLong(12L);
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

        dispatchCreateProjectCommand(root);

        assertTrue(root.isCreateProjectCommandHandled);
        assertTrue(root.isProjectCreatedEventApplied);
    }

    @Test
    public void handle_only_appropriate_command() throws InvocationTargetException {

        dispatchCreateProjectCommand(root);

        assertFalse(root.isAddTaskCommandHandled);
        assertFalse(root.isTaskAddedEventApplied);

        assertFalse(root.isStartProjectCommandHandled);
        assertFalse(root.isProjectStartedEventApplied);
    }

    @Test
    public void handle_appropriate_commands_sequentially() throws InvocationTargetException {

        dispatchCreateProjectCommand(root);
        assertTrue(root.isCreateProjectCommandHandled);

        dispatchAddTaskCommand(root);
        assertTrue(root.isAddTaskCommandHandled);

        dispatchStartProjectCommand(root);
        assertTrue(root.isStartProjectCommandHandled);
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
    public void return_event_classes_which_are_handled_by_root() {

        final Set<EventClass> classes = getEventClasses(ProjectRoot.class);

        assertTrue(classes.size() == 3);
        assertTrue(classes.contains(EventClass.of(ProjectCreated.class)));
        assertTrue(classes.contains(EventClass.of(TaskAdded.class)));
        assertTrue(classes.contains(EventClass.of(ProjectStarted.class)));
    }

    @Test
    public void return_message_classes_which_are_handled_by_root_case_command_classes() {

        final Set classes = getHandledMessageClasses(ProjectRoot.class, isCommandHandlerPredicate);

        assertTrue(classes.size() == 3);
        assertTrue(classes.contains(CreateProject.class));
        assertTrue(classes.contains(AddTask.class));
        assertTrue(classes.contains(StartProject.class));
    }

    @Test
    public void return_current_state() throws InvocationTargetException {

        dispatchCreateProjectCommand(root);

        final Project state = root.getState();

        assertEquals(PROJECT_ID, state.getProjectId());
        assertEquals(ProjectRoot.STATUS_NEW, state.getStatus());
    }

    @Test
    public void play_events() throws InvocationTargetException {
        playEvents(root);
        assertEventsApplied(root);
    }

    private void playEvents(AggregateRoot aggregateRoot) throws InvocationTargetException {

        List<EventRecord> events = newLinkedList();
        events.add(getProjectCreatedEventRecord());
        events.add(getTaskAddedEventRecord());
        events.add(getProjectStartedEventRecord());

        aggregateRoot.play(events);
    }

    private void assertEventsApplied(ProjectRoot aggregateRoot) {
        assertTrue(aggregateRoot.isProjectCreatedEventApplied);
        assertTrue(aggregateRoot.isTaskAddedEventApplied);
        assertTrue(aggregateRoot.isProjectStartedEventApplied);
    }



    private EventRecord getProjectCreatedEventRecord() {
        final Any event = toAny(ProjectCreated.newBuilder().setProjectId(PROJECT_ID).build());
        return EventRecord.newBuilder().setContext(EVENT_CONTEXT).setEvent(event).build();
    }

    private EventRecord getTaskAddedEventRecord() {
        final Any event = toAny(TaskAdded.newBuilder().setProjectId(PROJECT_ID).build());
        return EventRecord.newBuilder().setContext(EVENT_CONTEXT).setEvent(event).build();
    }

    private EventRecord getProjectStartedEventRecord() {
        final Any event = toAny(ProjectStarted.newBuilder().setProjectId(PROJECT_ID).build());
        return EventRecord.newBuilder().setContext(EVENT_CONTEXT).setEvent(event).build();
    }

    private void dispatchCreateProjectCommand(ProjectRoot projectRoot) throws InvocationTargetException {
        final Message command = CreateProject.newBuilder().setProjectId(projectRoot.getId()).build();
        projectRoot.dispatch(command, COMMAND_CONTEXT);
    }

    private void dispatchAddTaskCommand(ProjectRoot projectRoot) throws InvocationTargetException {
        final Message command = AddTask.newBuilder().setProjectId(projectRoot.getId()).build();
        projectRoot.dispatch(command, COMMAND_CONTEXT);
    }

    private void dispatchStartProjectCommand(ProjectRoot projectRoot) throws InvocationTargetException {
        final Message command = StartProject.newBuilder().setProjectId(projectRoot.getId()).build();
        projectRoot.dispatch(command, COMMAND_CONTEXT);
    }


    public static class ProjectRoot extends AggregateRoot<ProjectId, Project> {

        private static final String STATUS_NEW = "NEW";

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
            return Project.getDefaultInstance();
        }

        @Assign
        public Message handle(CreateProject cmd, CommandContext ctx) {
            isCreateProjectCommandHandled = true;
            return ProjectCreated.newBuilder().setProjectId(cmd.getProjectId()).build();
        }

        @Assign
        public Message handle(AddTask cmd, CommandContext ctx) {
            isAddTaskCommandHandled = true;
            return TaskAdded.newBuilder().setProjectId(cmd.getProjectId()).build();
        }

        @Assign
        public Message handle(StartProject cmd, CommandContext ctx) {
            isStartProjectCommandHandled = true;
            return ProjectStarted.newBuilder().setProjectId(cmd.getProjectId()).build();
        }

        @Apply
        private void event(ProjectCreated event) {

            Project newState = Project.newBuilder(getState())
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
            isProjectStartedEventApplied = true;
        }
    }

    public static class TestRootWithIdString extends AggregateRoot<String, Project> {
        protected TestRootWithIdString(String id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return null;
        }
    }

    public static class TestRootWithIdInteger extends AggregateRoot<Integer, Project> {
        protected TestRootWithIdInteger(Integer id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return null;
        }
    }

    public static class TestRootWithIdLong extends AggregateRoot<Long, Project> {
        protected TestRootWithIdLong(Long id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return null;
        }
    }

    public static class TestRootWithIdUnsupported extends AggregateRoot<UnsupportedClassVersionError, Project> {
        protected TestRootWithIdUnsupported(UnsupportedClassVersionError id) {
            super(id);
        }
        @Override protected Project getDefaultState() {
            return null;
        }
    }
}
