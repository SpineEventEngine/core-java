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

import com.google.protobuf.Any;
import io.spine.base.CommandMessage;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.server.commandbus.Given;
import io.spine.server.entity.rejection.EntityAlreadyArchived;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCancelIteration;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.command.PmThrowEntityAlreadyArchived;
import io.spine.test.procman.command.PmThrowRuntimeException;
import io.spine.test.procman.event.PmIterationPlanned;
import io.spine.test.procman.event.PmOwnerChanged;
import io.spine.test.procman.quiz.PmQuizId;
import io.spine.test.procman.quiz.event.PmQuizStarted;

import static io.spine.server.procman.given.pm.TestProcessManager.ID;
import static io.spine.testdata.Sample.messageOfType;

/**
 * Factory of messages to be sent to the Process Manager.
 */
public class GivenMessages {

    private GivenMessages() {
    }

    public static PmCreateProject createProject() {
        return PmCreateProject
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static PmStartProject startProject() {
        return PmStartProject
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static PmAddTask addTask() {
        return PmAddTask
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static PmCancelIteration cancelIteration() {
        return PmCancelIteration
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static PmIterationPlanned iterationPlanned(boolean budgetAllocated) {
        return PmIterationPlanned
                .newBuilder()
                .setProjectId(ID)
                .setBudgetAllocated(budgetAllocated)
                .build();
    }

    public static RejectionEnvelope
    entityAlreadyArchived(Class<? extends CommandMessage> commandClass) {
        Any id = Identifier.pack(TestProcessManager.class.getName());
        Command command = Given.ACommand.withMessage(messageOfType(commandClass));
        RejectionEnvelope result = RejectionEnvelope.from(CommandEnvelope.of(command),
                                                          EntityAlreadyArchived
                                                                  .newBuilder()
                                                                  .setEntityId(id)
                                                                  .build());
        return result;
    }

    public static PmOwnerChanged ownerChanged() {
        return PmOwnerChanged
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static PmQuizStarted quizStarted() {
        return PmQuizStarted
                .newBuilder()
                .setQuizId(messageOfType(PmQuizId.class))
                .build();
    }

    public static PmThrowEntityAlreadyArchived throwEntityAlreadyArchived() {
        return PmThrowEntityAlreadyArchived
                .newBuilder()
                .setProjectId(ID)
                .build();
    }

    public static PmThrowRuntimeException throwRuntimeException() {
        return PmThrowRuntimeException
                .newBuilder()
                .setProjectId(ID)
                .build();
    }
}
