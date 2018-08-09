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

package io.spine.server.procman.given.pm;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.React;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.server.procman.ProcessManager;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmReviewBacklog;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmNotificationSent;
import io.spine.test.procman.event.PmOwnerChanged;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.validate.AnyVBuilder;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testdata.Sample.builderForType;
import static io.spine.testdata.Sample.messageOfType;

/**
 * A test Process Manager which remembers past message as its state.
 */
public class TestProcessManager
        extends ProcessManager<ProjectId, Any, AnyVBuilder> {

    public static final ProjectId ID = messageOfType(ProjectId.class);

    public TestProcessManager(ProjectId id) {
        super(id);
    }

    /** Updates the state with putting incoming message.*/
    void remember(Message incoming) {
        getBuilder().mergeFrom(pack(incoming));
    }

    /*
     * Handled commands
     ********************/

    @Assign
    PmProjectCreated handle(PmCreateProject command) {
        remember(command);
        return ((PmProjectCreated.Builder) builderForType(PmProjectCreated.class))
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    PmTaskAdded handle(PmAddTask command) {
        remember(command);
        return ((PmTaskAdded.Builder) builderForType(PmTaskAdded.class))
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    PmNotificationSent handle(PmReviewBacklog command) {
        remember(command);
        return ((PmNotificationSent.Builder) builderForType(PmNotificationSent.class))
                .setProjectId(command.getProjectId())
                .build();
    }

    /*
     * Command generation
     *************************************/

    @Command
    PmAddTask transform(PmStartProject command) {
        remember(command);
        PmAddTask addTask = ((PmAddTask.Builder)
                builderForType(PmAddTask.class))
                .setProjectId(command.getProjectId())
                .build();
        return addTask;
    }

    @Command
    PmReviewBacklog on(PmOwnerChanged event) {
        remember(event);
        return messageOfType(PmReviewBacklog.class);
    }

    /*
     * Reactions on events
     ************************/

    @React
    public Empty on(PmProjectCreated event) {
        remember(event);
        return Empty.getDefaultInstance();
    }

    @React
    public Empty on(PmTaskAdded event) {
        remember(event);
        return Empty.getDefaultInstance();
    }

    @React
    public PmNotificationSent on(PmProjectStarted event) {
        remember(event);
        return messageOfType(PmNotificationSent.class);
    }

    /*
     * Reactions on rejections
     **************************/

    @React
    Empty on(EntityAlreadyArchived rejection, PmAddTask command) {
        remember(command); // We check the command in the test.
        return Empty.getDefaultInstance();
    }

    @React
    Empty on(EntityAlreadyArchived rejection) {
        remember(rejection);
        return Empty.getDefaultInstance();
    }
}
