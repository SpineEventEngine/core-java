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

import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmArchiveProcess;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmDeleteProcess;
import io.spine.test.procman.command.PmDoNothing;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testdata.Sample;

/**
 * Factory methods for command messages.
 */
public class GivenCommandMessage {

    public static final ProjectId ID = Sample.messageOfType(ProjectId.class);

    /** Prevents instantiation of this utility class. */
    private GivenCommandMessage() {
    }

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

    public static PmArchiveProcess archiveProcess() {
        return PmArchiveProcess.newBuilder()
                               .setProjectId(ID)
                               .build();
    }

    public static PmDeleteProcess deleteProcess() {
        return PmDeleteProcess.newBuilder()
                              .setProjectId(ID)
                              .build();
    }

    public static PmDoNothing doNothing() {
        return ((PmDoNothing.Builder) Sample.builderForType(PmDoNothing.class))
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
