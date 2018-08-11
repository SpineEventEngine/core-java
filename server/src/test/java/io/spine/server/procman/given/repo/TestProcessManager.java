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

package io.spine.server.procman.given.repo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.entity.TestEntityWithStringColumn;
import io.spine.server.entity.rejection.EntityAlreadyArchived;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.server.event.React;
import io.spine.server.procman.ProcessManager;
import io.spine.server.tuple.Pair;
import io.spine.test.procman.Project;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.ProjectVBuilder;
import io.spine.test.procman.Task;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmArchiveProcess;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmDeleteProcess;
import io.spine.test.procman.command.PmDoNothing;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.command.PmThrowEntityAlreadyArchived;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testdata.Sample;

import java.util.List;

import static io.spine.protobuf.AnyPacker.pack;

@SuppressWarnings({
        "OverlyCoupledClass",
        "UnusedParameters" /* The parameter left to show that a projection subscriber can have
                            two parameters. */})
public class TestProcessManager
        extends ProcessManager<ProjectId, Project, ProjectVBuilder>
        implements TestEntityWithStringColumn {

    /** The event message we store for inspecting in delivery tests. */
    private static final Multimap<ProjectId, Message> messagesDelivered = HashMultimap.create();

    public TestProcessManager(ProjectId id) {
        super(id);
    }

    public static boolean processed(Message message) {
        boolean result = messagesDelivered.containsValue(message);
        return result;
    }

    public static void clearMessageDeliveryHistory() {
        messagesDelivered.clear();
    }

    /** Keeps the event message for further inspection in tests. */
    private void keep(Message commandOrEventMsg) {
        messagesDelivered.put(getState().getId(), commandOrEventMsg);
    }

    private static Empty withNothing() {
        return Empty.getDefaultInstance();
    }

    private void handleProjectCreated(ProjectId projectId) {
        Project newState = getState().toBuilder()
                                     .setId(projectId)
                                     .setStatus(Project.Status.CREATED)
                                     .build();
        getBuilder().mergeFrom(newState);
    }

    private void handleTaskAdded(Task task) {
        Project newState = getState().toBuilder()
                                     .addTask(task)
                                     .build();
        getBuilder().mergeFrom(newState);
    }

    private void handleProjectStarted() {
        Project newState = getState().toBuilder()
                                     .setStatus(Project.Status.STARTED)
                                     .build();
        getBuilder().mergeFrom(newState);
    }

    @Assign
    PmProjectCreated handle(PmCreateProject command, CommandContext ignored) {
        keep(command);

        PmProjectCreated event = ((PmProjectCreated.Builder)
                Sample.builderForType(PmProjectCreated.class))
                      .setProjectId(command.getProjectId())
                      .build();
        return event;
    }

    @Assign
    PmTaskAdded handle(PmAddTask command, CommandContext ignored) {
        keep(command);

        PmTaskAdded event = ((PmTaskAdded.Builder)
                Sample.builderForType(PmTaskAdded.class))
                      .setProjectId(command.getProjectId())
                      .build();
        return event;
    }

    @Command
    Pair<PmAddTask, PmDoNothing> handle(PmStartProject command, CommandContext context) {
        keep(command);

        ProjectId projectId = command.getProjectId();
        PmAddTask addTask = ((PmAddTask.Builder)
                Sample.builderForType(PmAddTask.class))
                .setProjectId(projectId)
                .build();
        PmDoNothing doNothing = ((PmDoNothing.Builder)
                Sample.builderForType(PmDoNothing.class))
                .setProjectId(projectId)
                .build();
        return Pair.of(addTask, doNothing);
    }

    @Assign
    Empty handle(PmArchiveProcess command) {
        keep(command);
        setArchived(true);
        return withNothing();
    }

    @Assign
    Empty handle(PmDeleteProcess command) {
        keep(command);
        setDeleted(true);
        return withNothing();
    }

    @Assign
    Empty handle(PmDoNothing command, CommandContext ignored) {
        keep(command);
        return withNothing();
    }

    @Assign
    List<Message> handle(PmThrowEntityAlreadyArchived command) throws EntityAlreadyArchived {
        keep(command);
        throw new EntityAlreadyArchived(pack(command.getProjectId()));
    }

    @React
    Empty on(StandardRejections.EntityAlreadyArchived rejection) {
        keep(rejection);
        return withNothing();
    }

    @React
    Empty on(StandardRejections.EntityAlreadyDeleted rejection) {
        keep(rejection);
        return withNothing();
    }

    @React
    public Empty on(PmTaskAdded event) {
        keep(event);

        Task task = event.getTask();
        handleTaskAdded(task);
        return withNothing();
    }

    @React
    public Empty on(PmProjectStarted event) {
        keep(event);

        handleProjectStarted();
        return withNothing();
    }

    @React
    public Empty on(PmProjectCreated event, EventContext ignored) {
        keep(event);

        handleProjectCreated(event.getProjectId());
        return withNothing();
    }

    @Override
    public String getIdString() {
        return getId().toString();
    }
}
