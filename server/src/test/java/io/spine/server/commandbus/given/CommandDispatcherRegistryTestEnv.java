/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.Subscribe;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.TestEntityWithStringColumn;
import io.spine.server.event.EventBus;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.CmdStartProject;
import io.spine.test.command.Project;
import io.spine.test.command.ProjectId;
import io.spine.test.command.ProjectVBuilder;
import io.spine.test.command.Task;
import io.spine.test.command.event.CmdProjectCreated;
import io.spine.test.command.event.CmdProjectStarted;
import io.spine.test.command.event.CmdTaskAdded;

import java.util.Collections;
import java.util.Set;

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
            messagesDelivered.put(state().getId(), commandOrEventMsg);
        }

        @Subscribe
        public void on(CmdProjectCreated event) {
            // Keep the event message for further inspection in tests.
            keep(event);

            handleProjectCreated(event.getProjectId());
        }

        private void handleProjectCreated(ProjectId projectId) {
            Project newState = state().toBuilder()
                                      .setId(projectId)
                                      .setStatus(Project.Status.CREATED)
                                      .build();
            builder().mergeFrom(newState);
        }

        @Subscribe
        public void on(CmdTaskAdded event) {
            keep(event);

            Task task = event.getTask();
            handleTaskAdded(task);
        }

        private void handleTaskAdded(Task task) {
            Project newState = state().toBuilder()
                                      .addTask(task)
                                      .build();
            builder().mergeFrom(newState);
        }

        @Subscribe
        public void on(CmdProjectStarted event) {
            keep(event);

            handleProjectStarted();
        }

        private void handleProjectStarted() {
            Project newState = state().toBuilder()
                                      .setStatus(Project.Status.STARTED)
                                      .build();
            builder().mergeFrom(newState);
        }

        @Override
        public String getIdString() {
            return id().toString();
        }
    }

    public static class EmptyDispatcher implements CommandDispatcher<Message> {

        @Override
        public Set<CommandClass> getMessageClasses() {
            return Collections.emptySet();
        }

        @Override
        public Message dispatch(CommandEnvelope envelope) {
            return Empty.getDefaultInstance();
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }
    }

    public static class NoCommandsDispatcherRepo
            extends ProcessManagerRepository<ProjectId, NoCommandsProcessManager, Project> {

    }

    public static class AllCommandDispatcher implements CommandDispatcher<Message> {

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CmdCreateProject.class,
                                      CmdStartProject.class,
                                      CmdAddTask.class);
        }

        @Override
        public Message dispatch(CommandEnvelope envelope) {
            return Empty.getDefaultInstance();
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }
    }

    public static class CreateProjectDispatcher implements CommandDispatcher<Message> {

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CmdCreateProject.class);
        }

        @Override
        public Message dispatch(CommandEnvelope envelope) {
            return Empty.getDefaultInstance();
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }
    }

    public static class AddTaskDispatcher implements CommandDispatcher<Message> {

        @Override
        public Set<CommandClass> getMessageClasses() {
            return CommandClass.setOf(CmdAddTask.class);
        }

        @Override
        public Message dispatch(CommandEnvelope envelope) {
            // Do nothing.
            return Empty.getDefaultInstance();
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }
    }

    /*
     * Test command handlers.
     ************************/

    public static class CreateProjectHandler extends AbstractCommandHandler {

        public CreateProjectHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Assign
        CmdProjectCreated handle(CmdCreateProject command, CommandContext ctx) {
            return CmdProjectCreated.getDefaultInstance();
        }
    }

    public static class AllCommandHandler extends AbstractCommandHandler {

        public AllCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Assign
        CmdProjectCreated handle(CmdCreateProject command, CommandContext ctx) {
            return CmdProjectCreated.getDefaultInstance();
        }

        @Assign
        CmdTaskAdded handle(CmdAddTask command) {
            return CmdTaskAdded.getDefaultInstance();
        }

        @Assign
        CmdProjectStarted handle(CmdStartProject command) {
            return CmdProjectStarted.getDefaultInstance();
        }
    }

    public static class EmptyCommandHandler extends AbstractCommandHandler {

        public EmptyCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Override
        public Set<CommandClass> getMessageClasses() {
            return ImmutableSet.of();
        }
    }
}
