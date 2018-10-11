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
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.entity.TestEntityWithStringColumn;
import io.spine.server.entity.rejection.EntityAlreadyArchived;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.server.entity.storage.Column;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
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

import java.util.List;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testdata.Sample.builderForType;

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
        PmProjectCreated.Builder event = builderForType(PmProjectCreated.class);
        return event.setProjectId(command.getProjectId())
                    .build();
    }

    @Assign
    PmTaskAdded handle(PmAddTask command, CommandContext ignored) {
        keep(command);
        PmTaskAdded.Builder event = builderForType(PmTaskAdded.class);
        return event.setProjectId(command.getProjectId())
                    .build();
    }

    @Command
    Pair<PmAddTask, PmDoNothing> handle(PmStartProject command, CommandContext context) {
        keep(command);
        ProjectId projectId = command.getProjectId();
        PmAddTask.Builder addTask = builderForType(PmAddTask.class);
        addTask.setProjectId(projectId);
        PmDoNothing.Builder doNothing = builderForType(PmDoNothing.class);
        doNothing.setProjectId(projectId);
        return Pair.of(addTask.build(), doNothing.build());
    }

    @Assign
    Nothing handle(PmArchiveProcess command) {
        keep(command);
        setArchived(true);
        return nothing();
    }

    @Assign
    Nothing handle(PmDeleteProcess command) {
        keep(command);
        setDeleted(true);
        return nothing();
    }

    @Assign
    Nothing handle(PmDoNothing command, CommandContext ignored) {
        keep(command);
        return nothing();
    }

    @Assign
    List<Message> handle(PmThrowEntityAlreadyArchived command) throws EntityAlreadyArchived {
        keep(command);
        throw new EntityAlreadyArchived(pack(command.getProjectId()));
    }

    @React
    Nothing on(StandardRejections.EntityAlreadyArchived rejection) {
        keep(rejection);
        return nothing();
    }

    @React
    Nothing on(StandardRejections.EntityAlreadyDeleted rejection) {
        keep(rejection);
        return nothing();
    }

    @React
    Nothing on(PmTaskAdded event) {
        keep(event);

        Task task = event.getTask();
        handleTaskAdded(task);
        return nothing();
    }

    @React
    Nothing on(PmProjectStarted event) {
        keep(event);

        handleProjectStarted();
        return nothing();
    }

    @React
    Nothing on(PmProjectCreated event, EventContext ignored) {
        keep(event);

        handleProjectCreated(event.getProjectId());
        return nothing();
    }

    @Column
    public String getName() {
        return getState().getName();
    }

    @Override
    public String getIdString() {
        return getId().toString();
    }
}
