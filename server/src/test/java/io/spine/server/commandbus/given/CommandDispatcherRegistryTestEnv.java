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

package io.spine.server.commandbus.given;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.annotation.Subscribe;
import io.spine.base.CommandContext;
import io.spine.envelope.CommandEnvelope;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.TestEntityWithStringColumn;
import io.spine.server.event.EventBus;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.test.command.AddTask;
import io.spine.test.command.CreateProject;
import io.spine.test.command.Project;
import io.spine.test.command.ProjectId;
import io.spine.test.command.ProjectVBuilder;
import io.spine.test.command.StartProject;
import io.spine.test.command.Task;
import io.spine.test.command.event.ProjectCreated;
import io.spine.test.command.event.ProjectStarted;
import io.spine.test.command.event.TaskAdded;
import io.spine.type.CommandClass;

import java.util.Collections;
import java.util.Set;

// Test data imports

/**
 * @author Alexander Yevsyukov
 */
public class CommandDispatcherRegistryTestEnv {

    private CommandDispatcherRegistryTestEnv() {
    }

    /*
     * Test command dispatchers.
     ***************************/

    public static class NoCommandsProcessManager
            extends ProcessManager<ProjectId, Project, ProjectVBuilder>
            implements TestEntityWithStringColumn {

        /** The event message we store for inspecting in delivery tests. */
        private static final Multimap<ProjectId, Message> messagesDelivered = HashMultimap.create();

        public NoCommandsProcessManager(ProjectId id) {
            super(id);
        }

        private void keep(Message commandOrEventMsg) {
            messagesDelivered.put(getState().getId(), commandOrEventMsg);
        }

        @Subscribe
        public void on(ProjectCreated event) {
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
        public void on(TaskAdded event) {
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
        public void on(ProjectStarted event) {
            keep(event);

            handleProjectStarted();
        }

        private void handleProjectStarted() {
            final Project newState = getState().toBuilder()
                                               .setStatus(Project.Status.STARTED)
                                               .build();
            getBuilder().mergeFrom(newState);
        }

        @Override
        public String getIdString() {
            return getId().toString();
        }
    }

    public static class EmptyDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getMessageClasses() {
            return Collections.emptySet();
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
        }
    }

    public static class NoCommandsDispatcherRepo
            extends ProcessManagerRepository<ProjectId, NoCommandsProcessManager, Project> {

        /**
         * Always returns an empty set of command classes forwarded by this repository.
         */
//        @SuppressWarnings("MethodDoesntCallSuperMethod")
//        @Override
//        public Set<CommandClass> getCommandClasses() {
//            return newHashSet();
//        }
    }

    public static class AllCommandDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CreateProject.class,
                                      StartProject.class,
                                      AddTask.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
        }
    }

    public static class CreateProjectDispatcher implements CommandDispatcher {
        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CreateProject.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
        }
    }

    public static class AddTaskDispatcher implements CommandDispatcher {

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(AddTask.class);
        }

        @Override
        public void dispatch(CommandEnvelope envelope) {
            // Do nothing.
        }
    }

    /*
     * Test command handlers.
     ************************/

    public static class CreateProjectHandler extends CommandHandler {

        public CreateProjectHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Assign
        ProjectCreated handle(CreateProject command, CommandContext ctx) {
            return ProjectCreated.getDefaultInstance();
        }
    }

    public static class AllCommandHandler extends CommandHandler {

        public AllCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Assign
        ProjectCreated handle(CreateProject command, CommandContext ctx) {
            return ProjectCreated.getDefaultInstance();
        }

        @Assign
        TaskAdded handle(AddTask command) {
            return TaskAdded.getDefaultInstance();
        }

        @Assign
        ProjectStarted handle(StartProject command) {
            return ProjectStarted.getDefaultInstance();
        }
    }

    public static class EmptyCommandHandler extends CommandHandler {

        public EmptyCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Override
        public Set<CommandClass> getMessageClasses() {
            return ImmutableSet.of();
        }
    }
}
