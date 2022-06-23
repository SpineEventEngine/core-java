/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.core.External;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.entity.rejection.EntityAlreadyArchived;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;
import io.spine.server.procman.ProcessManager;
import io.spine.server.test.shared.AnyProcess;
import io.spine.server.tuple.Pair;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCancelIteration;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmPlanIteration;
import io.spine.test.procman.command.PmReviewBacklog;
import io.spine.test.procman.command.PmScheduleRetrospective;
import io.spine.test.procman.command.PmStartIteration;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.command.PmThrowEntityAlreadyArchived;
import io.spine.test.procman.command.PmThrowRuntimeException;
import io.spine.test.procman.event.PmIterationCompleted;
import io.spine.test.procman.event.PmIterationPlanned;
import io.spine.test.procman.event.PmIterationStarted;
import io.spine.test.procman.event.PmNotificationSent;
import io.spine.test.procman.event.PmOwnerChanged;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.test.procman.quiz.event.PmQuestionAnswered;
import io.spine.test.procman.quiz.event.PmQuizStarted;

import java.util.List;
import java.util.Optional;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testdata.Sample.builderForType;
import static io.spine.testdata.Sample.messageOfType;

/**
 * A test Process Manager which remembers past message as its state.
 */
public class TestProcessManager
        extends ProcessManager<ProjectId, AnyProcess, AnyProcess.Builder> {

    public static final ProjectId ID = messageOfType(ProjectId.class);

    public TestProcessManager(ProjectId id) {
        super(id);
    }

    /** Updates the state with putting incoming message. */
    private void remember(Message incoming) {
        builder().setAny(pack(incoming));
    }

    /*
     * Handled commands
     ********************/

    @Assign
    PmProjectCreated handle(PmCreateProject command) {
        remember(command);
        return PmProjectCreated
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    PmTaskAdded handle(PmAddTask command) {
        remember(command);
        return PmTaskAdded
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    PmNotificationSent handle(PmReviewBacklog command) {
        remember(command);
        return PmNotificationSent
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    PmNotificationSent handle(PmScheduleRetrospective command) {
        remember(command);
        return PmNotificationSent
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    PmIterationPlanned handle(PmPlanIteration command) {
        remember(command);
        return PmIterationPlanned
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    PmIterationStarted handle(PmStartIteration command) {
        remember(command);
        return PmIterationStarted
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    List<EventMessage> handle(PmThrowEntityAlreadyArchived command) throws EntityAlreadyArchived {
        remember(command);
        throw EntityAlreadyArchived
                .newBuilder()
                .setEntityId(Identifier.pack(command.getProjectId()))
                .build();
    }

    @Assign
    List<EventMessage> handle(PmThrowRuntimeException command) {
        remember(command);
        throw new RuntimeException("that triggers transaction rollback");
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
     * Generation of more than one command
     **************************************/

    @Command
    Pair<PmScheduleRetrospective, PmPlanIteration> split(PmCancelIteration command) {
        remember(command);
        ProjectId pid = command.getProjectId();
        return Pair.of(PmScheduleRetrospective
                               .newBuilder()
                               .setProjectId(pid)
                               .build(),
                       PmPlanIteration
                               .newBuilder()
                               .setProjectId(pid)
                               .build());
    }

    @Command
    Pair<PmScheduleRetrospective, PmPlanIteration> on(PmIterationCompleted event) {
        remember(event);
        ProjectId pid = event.getProjectId();
        return Pair.of(PmScheduleRetrospective
                               .newBuilder()
                               .setProjectId(pid)
                               .build(),
                       PmPlanIteration
                               .newBuilder()
                               .setProjectId(pid)
                               .build());
    }

    /*
     * Optional generation of a command
     **********************************/

    @Command
    Optional<PmStartIteration> on(PmIterationPlanned event) {
        remember(event);
        if (event.getBudgetAllocated()) {
            return Optional.of(PmStartIteration.newBuilder()
                                               .setProjectId(event.getProjectId())
                                               .build());
        }
        return Optional.empty();
    }

    /*
     * Reactions on events
     ************************/

    @React
    Nothing on(PmProjectCreated event) {
        remember(event);
        return nothing();
    }

    @React
    Nothing on(PmTaskAdded event) {
        remember(event);
        return nothing();
    }

    @React
    PmNotificationSent on(PmProjectStarted event) {
        remember(event);
        return messageOfType(PmNotificationSent.class);
    }

    /*
     * Reactions (including commanders) on external events
     **********************************************/

    @Command
    PmCreateProject on(@External PmQuizStarted event) {
        return messageOfType(PmCreateProject.class);
    }

    @React
    Nothing on(@External PmQuestionAnswered event) {
        return nothing();
    }

    /*
     * Reactions on rejections
     **************************/

    @React
    Nothing on(StandardRejections.EntityAlreadyArchived rejection, PmAddTask command) {
        remember(command); // We check the command in the test.
        return nothing();
    }

    @React
    Nothing on(StandardRejections.EntityAlreadyArchived rejection) {
        remember(rejection);
        return nothing();
    }
}
