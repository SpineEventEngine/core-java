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
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.server.command.Assign;
import io.spine.server.entity.TestEntityWithStringColumn;
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

    private ProcessManagerRepositoryTestEnv() {
    }

    public static class TestProcessManagerRepository
            extends ProcessManagerRepository<ProjectId, TestProcessManager, Project> {

        public TestProcessManagerRepository() {
            super();
        }
    }

    @SuppressWarnings("OverlyCoupledClass")
    public static class TestProcessManager
            extends ProcessManager<ProjectId, Project, ProjectVBuilder>
            implements TestEntityWithStringColumn {

        /** The event message we store for inspecting in delivery tests. */
        private static final Multimap<ProjectId, Message> messagesDelivered = HashMultimap.create();

        public TestProcessManager(ProjectId id) {
            super(id);
        }

        public static boolean processed(Message eventMessage) {
            final boolean result = messagesDelivered.containsValue(eventMessage);
            return result;
        }

        public static void clearMessageDeliveryHistory() {
            messagesDelivered.clear();
        }

        private void keep(Message commandOrEventMsg) {
            messagesDelivered.put(getState().getId(), commandOrEventMsg);
        }

        @SuppressWarnings("UnusedParameters")
            /* The parameter left to show that a projection subscriber can have two parameters. */
        @Subscribe
        public void on(PmProjectCreated event, EventContext ignored) {
            // Keep the event message for further inspection in tests.
            keep(event);

            handleProjectCreated(event.getProjectId());
        }

        private void handleProjectCreated(ProjectId projectId) {
            final Project newState = getState().toBuilder()
                                               .setId(projectId)
                                               .setStatus(Project.Status.CREATED)
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        public void on(PmTaskAdded event) {
            keep(event);

            final Task task = event.getTask();
            handleTaskAdded(task);
        }

        private void handleTaskAdded(Task task) {
            final Project newState = getState().toBuilder()
                                               .addTask(task)
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        public void on(PmProjectStarted event) {
            keep(event);

            handleProjectStarted();
        }

        private void handleProjectStarted() {
            final Project newState = getState().toBuilder()
                                               .setStatus(Project.Status.STARTED)
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        @SuppressWarnings("UnusedParameters")
            /* The parameter left to show that a command subscriber can have two parameters. */
        @Assign
        PmProjectCreated handle(PmCreateProject command, CommandContext ignored) {
            keep(command);

            handleProjectCreated(command.getProjectId());
            final PmProjectCreated event = ((PmProjectCreated.Builder) Sample.builderForType(
                    PmProjectCreated.class))
                    .setProjectId(command.getProjectId())
                    .build();
            return event;
        }

        @SuppressWarnings("UnusedParameters")
            /* The parameter left to show that a command subscriber can have two parameters. */
        @Assign
        PmTaskAdded handle(PmAddTask command, CommandContext ignored) {
            keep(command);

            handleTaskAdded(command.getTask());
            final PmTaskAdded event = ((PmTaskAdded.Builder) Sample.builderForType(PmTaskAdded.class))
                    .setProjectId(command.getProjectId())
                    .build();
            return event;
        }

        @Assign
        CommandRouted handle(PmStartProject command, CommandContext context) {
            keep(command);

            handleProjectStarted();
            final Message addTask = ((PmAddTask.Builder) Sample.builderForType(PmAddTask.class))
                    .setProjectId(command.getProjectId())
                    .build();
            return newRouterFor(command, context)
                    .add(addTask)
                    .routeAll();
        }

        @Override
        public String getIdString() {
            return getId().toString();
        }
    }
}
