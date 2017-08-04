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

package io.spine.server.procman.given;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.server.command.Assign;
import io.spine.server.entity.TestEntityWithStringColumn;
import io.spine.server.entity.rejection.Rejections.EntityAlreadyArchived;
import io.spine.server.entity.rejection.Rejections.EntityAlreadyDeleted;
import io.spine.server.procman.CommandRouted;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.ProjectVBuilder;
import io.spine.test.procman.Task;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testdata.Sample;

public class ProcessManagerRepositoryTestEnv {

    /** Prevents instantiation of this utility class. */
    private ProcessManagerRepositoryTestEnv() {}

    public static class TestProcessManagerRepository
            extends ProcessManagerRepository<ProjectId, TestProcessManager, Project> {

        public TestProcessManagerRepository() {
            super();
        }
    }

    @SuppressWarnings({
            "OverlyCoupledClass",
            "UnusedParameters" /* The parameter left to show that a projection subscriber can have
                                two parameters. */})
    public static class TestProcessManager
            extends ProcessManager<ProjectId, Project, ProjectVBuilder>
            implements TestEntityWithStringColumn {

        /** The event message we store for inspecting in delivery tests. */
        private static final Multimap<ProjectId, Message> messagesDelivered = HashMultimap.create();

        public TestProcessManager(ProjectId id) {
            super(id);
        }

        public static boolean processed(Message message) {
            final boolean result = messagesDelivered.containsValue(message);
            return result;
        }

        public static void clearMessageDeliveryHistory() {
            messagesDelivered.clear();
        }

        /** Keeps the event message for further inspection in tests. */
        private void keep(Message commandOrEventMsg) {
            messagesDelivered.put(getState().getId(), commandOrEventMsg);
        }

        @React
        public Empty on(PmProjectCreated event, EventContext ignored) {
            keep(event);

            handleProjectCreated(event.getProjectId());
            return withNothing();
        }

        private static Empty withNothing() {
            return Empty.getDefaultInstance();
        }

        private void handleProjectCreated(ProjectId projectId) {
            final Project newState = getState().toBuilder()
                                               .setId(projectId)
                                               .setStatus(Project.Status.CREATED)
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        @React
        public Empty on(PmTaskAdded event) {
            keep(event);

            final Task task = event.getTask();
            handleTaskAdded(task);
            return withNothing();
        }

        private void handleTaskAdded(Task task) {
            final Project newState = getState().toBuilder()
                                               .addTask(task)
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        @React
        public Empty on(PmProjectStarted event) {
            keep(event);

            handleProjectStarted();
            return withNothing();
        }

        private void handleProjectStarted() {
            final Project newState = getState().toBuilder()
                                               .setStatus(Project.Status.STARTED)
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        @Assign
        PmProjectCreated handle(PmCreateProject command, CommandContext ignored) {
            keep(command);

            handleProjectCreated(command.getProjectId());
            final PmProjectCreated event = ((PmProjectCreated.Builder)
                    Sample.builderForType(PmProjectCreated.class))
                          .setProjectId(command.getProjectId())
                          .build();
            return event;
        }

        @Assign
        PmTaskAdded handle(PmAddTask command, CommandContext ignored) {
            keep(command);

            handleTaskAdded(command.getTask());
            final PmTaskAdded event = ((PmTaskAdded.Builder)
                    Sample.builderForType(PmTaskAdded.class))
                          .setProjectId(command.getProjectId())
                          .build();
            return event;
        }

        @Assign
        CommandRouted handle(PmStartProject command, CommandContext context) {
            keep(command);

            handleProjectStarted();
            final Message addTask = ((PmAddTask.Builder)
                    Sample.builderForType(PmAddTask.class))
                          .setProjectId(command.getProjectId())
                          .build();
            return newRouterFor(command, context)
                    .add(addTask)
                    .routeAll();
        }

        @React
        Empty on(EntityAlreadyArchived rejection) {
            keep(rejection);
            return withNothing();
        }

        @React
        Empty on(EntityAlreadyDeleted rejection) {
            keep(rejection);
            return withNothing();
        }

        @Override
        public String getIdString() {
            return getId().toString();
        }
    }

    public static class GivenCommandMessage {

        public static final ProjectId ID = Sample.messageOfType(ProjectId.class);

        /** Prevents instantiation on this utility class. */
        private GivenCommandMessage() {}

        public static PmCreateProject createProject() {
            return ((PmCreateProject.Builder) Sample.builderForType(PmCreateProject.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmStartProject startProject() {
            return ((PmStartProject.Builder) Sample.builderForType(PmStartProject.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmAddTask addTask() {
            return ((PmAddTask.Builder) Sample.builderForType(PmAddTask.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmProjectStarted projectStarted() {
            return ((PmProjectStarted.Builder) Sample.builderForType(PmProjectStarted.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmProjectCreated projectCreated() {
            return ((PmProjectCreated.Builder) Sample.builderForType(PmProjectCreated.class))
                    .setProjectId(ID)
                    .build();
        }

        public static PmTaskAdded taskAdded() {
            return ((PmTaskAdded.Builder) Sample.builderForType(PmTaskAdded.class))
                    .setProjectId(ID)
                    .build();
        }
    }
}
