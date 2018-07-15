/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.aggregate.given.aggregate;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Commands;
import io.spine.core.Event;
import io.spine.core.React;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.command.ImportEvents;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.testing.server.aggregate.AggregateMessageDispatcher;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static io.spine.server.aggregate.given.Given.EventMessage.projectStarted;
import static io.spine.server.aggregate.given.Given.EventMessage.taskAdded;
import static io.spine.server.aggregate.given.aggregate.AggregateTestEnv.env;

/**
 * An aggregate class with handlers and appliers.
 *
 * <p>This class is declared here instead of being inner class of {@link AggregateTestEnv}
 * because it is heavily connected with internals of this test suite.
 *
 * @author Alexander Yevsyukkov
 */
@SuppressWarnings("PublicField") /* For inspection in tests. */
public class TestAggregate
        extends Aggregate<ProjectId, Project, ProjectVBuilder> {

    @VisibleForTesting
    public boolean isCreateProjectCommandHandled = false;
    @VisibleForTesting
    public boolean isAddTaskCommandHandled = false;
    @VisibleForTesting
    public boolean isStartProjectCommandHandled = false;
    @VisibleForTesting
    public boolean isProjectCreatedEventApplied = false;
    @VisibleForTesting
    public boolean isTaskAddedEventApplied = false;
    @VisibleForTesting
    public boolean isProjectStartedEventApplied = false;
    @VisibleForTesting
    public boolean isRejectionHandled = false;
    @VisibleForTesting
    public boolean isRejectionWithCmdHandled = false;

    public TestAggregate(ProjectId id) {
        super(id);
    }

    /**
     * Overrides to expose the method to the test.
     */
    @VisibleForTesting
    @Override
    public void init() {
        super.init();
    }

    @Assign
    AggProjectCreated handle(AggCreateProject cmd, CommandContext ctx) {
        isCreateProjectCommandHandled = true;
        final AggProjectCreated event = projectCreated(cmd.getProjectId(),
                                                       cmd.getName());
        return event;
    }

    @Assign
    AggTaskAdded handle(AggAddTask cmd, CommandContext ctx) {
        isAddTaskCommandHandled = true;
        final AggTaskAdded event = taskAdded(cmd.getProjectId());
        return event.toBuilder()
                    .setTask(cmd.getTask())
                    .build();
    }

    @Assign
    List<AggProjectStarted> handle(AggStartProject cmd, CommandContext ctx) {
        isStartProjectCommandHandled = true;
        final AggProjectStarted message = projectStarted(cmd.getProjectId());
        return newArrayList(message);
    }

    @Assign
    List<Event> handle(ImportEvents command, CommandContext ctx) {
        return command.getEventList();
    }

    @Apply
    void event(AggProjectCreated event) {
        getBuilder()
                .setId(event.getProjectId())
                .setStatus(Status.CREATED);

        isProjectCreatedEventApplied = true;
    }

    @Apply
    void event(AggTaskAdded event) {
        isTaskAddedEventApplied = true;
        getBuilder().addTask(event.getTask());
    }

    @Apply
    void event(AggProjectStarted event) {
        getBuilder()
                .setId(event.getProjectId())
                .setStatus(Status.STARTED);

        isProjectStartedEventApplied = true;
    }

    @React
    Empty on(StandardRejections.CannotModifyDeletedEntity rejection, AggAddTask command) {
        isRejectionWithCmdHandled = true;
        return Empty.getDefaultInstance();
    }

    @React
    Empty on(StandardRejections.CannotModifyDeletedEntity rejection) {
        isRejectionHandled = true;
        return Empty.getDefaultInstance();
    }

    @VisibleForTesting
    public void dispatchCommands(Command... commands) {
        for (Command cmd : commands) {
            final Message commandMessage = Commands.getMessage(cmd);
            AggregateMessageDispatcher.dispatchCommand(this, env(commandMessage));
        }
    }
}
