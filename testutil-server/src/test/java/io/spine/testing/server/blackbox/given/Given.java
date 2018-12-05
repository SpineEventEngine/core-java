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

package io.spine.testing.server.blackbox.given;

import com.google.common.collect.Lists;
import io.spine.core.UserId;
import io.spine.testing.server.blackbox.BbProjectId;
import io.spine.testing.server.blackbox.BbReportId;
import io.spine.testing.server.blackbox.BbTask;
import io.spine.testing.server.blackbox.command.BbAddProjectAssignee;
import io.spine.testing.server.blackbox.command.BbAddTask;
import io.spine.testing.server.blackbox.command.BbCreateProject;
import io.spine.testing.server.blackbox.command.BbCreateReport;
import io.spine.testing.server.blackbox.event.BbTaskAdded;
import io.spine.testing.server.blackbox.event.BbUserDeleted;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;

/**
 * @author Mykhailo Drachuk
 */
public class Given {

    /** Prevents instantiation of this utility class. */
    private Given() {
    }

    public static BbAddTask addTask(BbProjectId projectId) {
        return BbAddTask.newBuilder()
                        .setProjectId(projectId)
                        .setTask(newTask())
                        .build();
    }

    public static BbTaskAdded taskAdded(BbProjectId projectId) {
        return BbTaskAdded.newBuilder()
                           .setProjectId(projectId)
                           .setTask(newTask())
                           .build();
    }

    private static BbTask newTask() {
        return BbTask.newBuilder()
                   .setTitle(newUuid())
                   .build();
    }

    public static BbCreateReport createReport(BbProjectId projectId) {
        return BbCreateReport.newBuilder()
                             .setReportId(newReportId())
                             .addProjectId(projectId)
                             .build();
    }

    private static BbReportId newReportId() {
        return BbReportId.newBuilder()
                       .setId(newUuid())
                       .build();
    }

    public static BbCreateProject createProject() {
        return createProject(newProjectId());
    }

    public static BbCreateProject createProject(BbProjectId projectId) {
        return BbCreateProject.newBuilder()
                               .setProjectId(projectId)
                               .build();
    }

    public static BbProjectId newProjectId() {
        return BbProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    public static BbAddProjectAssignee addProjectAssignee(BbProjectId projectId, UserId id) {
        return BbAddProjectAssignee
                .newBuilder()
                .setId(projectId)
                .setUserId(id)
                .build();
    }

    public static BbUserDeleted userDeleted(UserId id, BbProjectId... projectIds) {
        return BbUserDeleted
                .newBuilder()
                .setId(id)
                .addAllProject(newArrayList(projectIds))
                .build();
    }
}
