/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.examples.eventstore;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.GeneratedMessage;
import org.spine3.base.EventId;
import org.spine3.base.UserId;
import org.spine3.util.Commands;

import static org.spine3.util.Users.newUserId;

/**
 * @author Alexander Yevsyukov
 */

@SuppressWarnings("UtilityClass")
public class SampleData {

    static final ImmutableList<UserId> userIds = ImmutableList.of(
            newUserId("diane.riley@spine3.org"),
            newUserId("r.edwards@spine3.org"),
            newUserId("n.richards@spine3.org"),
            newUserId("a.soto@spine3.org"),
            newUserId("j.aguilar@spine3.org"),
            newUserId("s.herrera@spine3.org"),
            newUserId("a.reid@spine3.org")
    );

    private static ProjectId newProjectId(String value) {
        return ProjectId.newBuilder().setCode(value).build();
    }

    private static Project newProject(String projectId, String name, String description) {
        return Project.newBuilder()
                .setId(newProjectId(projectId))
                .setName(name)
                .setDescription(description)
                .build();
    }

    private static Task newTask(int taskId, ProjectId projectId, String name, String description) {
        return Task.newBuilder()
                .setId(TaskId.newBuilder().setNumber(taskId).build())
                .setProjectId(projectId)
                .setName(name)
                .setDescription(description)
                .build();
    }

    private static final ProjectId p1 = newProjectId("p1");

    static final ImmutableList<GeneratedMessage> events = ImmutableList.of(

            //TODO:2016-01-12:alexander.yevsyukov: Write more events here.

            ProjectCreated.newBuilder().setProject(newProject(p1.getCode(), "Set Up", "Initial configuration")).build(),
            TaskCreated.newBuilder().setTask(newTask(1, p1, "Task 1", "Task 1 description")).build(),
            TaskAssigned.newBuilder()
                    .setTaskId(TaskId.newBuilder().setNumber(1).build())
                    .setAssignee(userIds.get(1)).build()
    );

    static EventId newEventId(UserId actor) {
        return org.spine3.util.Events.generateId(Commands.generateId(actor));
    }

    //@formatter:off
    private SampleData() {}
}
