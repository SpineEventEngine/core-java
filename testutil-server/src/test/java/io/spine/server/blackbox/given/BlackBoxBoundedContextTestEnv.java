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

package io.spine.server.blackbox.given;


import io.spine.testing.server.blackbox.BbAddTask;
import io.spine.testing.server.blackbox.BbCreateProject;
import io.spine.testing.server.blackbox.BbCreateReport;
import io.spine.testing.server.blackbox.BbTaskAdded;
import io.spine.testing.server.blackbox.ProjectId;
import io.spine.testing.server.blackbox.ReportId;
import io.spine.testing.server.blackbox.Task;

import static io.spine.base.Identifier.newUuid;

/**
 * @author Mykhailo Drachuk
 */
public class BlackBoxBoundedContextTestEnv {

    /** Prevents instantiation of this utility class. */
    private BlackBoxBoundedContextTestEnv() {
        // Does nothing.
    }

    public static BbAddTask addTask(ProjectId projectId) {
        return BbAddTask.newBuilder()
                         .setProjectId(projectId)
                         .setTask(newTask())
                         .build();
    }

    public static BbTaskAdded taskAdded(ProjectId projectId) {
        return BbTaskAdded.newBuilder()
                           .setProjectId(projectId)
                           .setTask(newTask())
                           .build();
    }

    private static Task newTask() {
        return Task.newBuilder()
                   .setTitle(newUuid())
                   .build();
    }

    public static BbCreateReport createReport(ProjectId projectId) {
        return BbCreateReport.newBuilder()
                              .setReportId(newReportId())
                              .addProjectId(projectId)
                              .build();
    }

    private static ReportId newReportId() {
        return ReportId.newBuilder()
                       .setId(newUuid())
                       .build();
    }

    public static BbCreateProject createProject() {
        return createProject(newProjectId());
    }

    public static BbCreateProject createProject(ProjectId projectId) {
        return BbCreateProject.newBuilder()
                               .setProjectId(projectId)
                               .build();
    }

    public static ProjectId newProjectId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }
}
