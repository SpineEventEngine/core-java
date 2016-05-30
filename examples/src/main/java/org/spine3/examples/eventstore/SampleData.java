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
import org.spine3.examples.eventstore.events.ProjectCreated;
import org.spine3.examples.eventstore.events.TaskAssigned;
import org.spine3.examples.eventstore.events.TaskCreated;
import org.spine3.examples.eventstore.events.TaskDone;
import org.spine3.users.UserId;

import java.util.concurrent.ThreadLocalRandom;

import static org.spine3.client.UserUtil.newUserId;

/**
 * @author Alexander Yevsyukov
 */

public class SampleData {

    static final ImmutableList<GeneratedMessage> events = generateEvents();

    static final ImmutableList<UserId> userIds = ImmutableList.of(
            newUserId("diane.riley@spine3.org"),
            newUserId("r.edwards@spine3.org"),
            newUserId("n.richards@spine3.org"),
            newUserId("a.soto@spine3.org"),
            newUserId("j.aguilar@spine3.org"),
            newUserId("s.herrera@spine3.org"),
            newUserId("a.reid@spine3.org")
    );

    private static Project newProject(String projectId, String name, String description) {
        return Project.newBuilder()
                .setId(projectId(projectId))
                .setName(name)
                .setDescription(description)
                .build();
    }

    private static ProjectId projectId(String value) {
        return ProjectId.newBuilder().setCode(value).build();
    }

    private static class TaskIdFactory {
        private int lastValue = 1;

        TaskId generate() {
            ++lastValue;
            final TaskId result = TaskId.newBuilder().setNumber(lastValue).build();
            return result;
        }

        private static final TaskIdFactory INSTANCE = new TaskIdFactory();
    }

    private static TaskId generate() {
        return TaskIdFactory.INSTANCE.generate();
    }

    private static Task newTask(ProjectId projectId, String name, String description) {
        return Task.newBuilder()
                .setId(generate())
                .setProjectId(projectId)
                .setName(name)
                .setDescription(description)
                .build();
    }

    private static final ProjectId ALPHA = projectId("alpha");

    static UserId randomSelectUser() {
        final int id = ThreadLocalRandom.current().nextInt(0, userIds.size());
        return userIds.get(id);
    }

    private static ImmutableList<GeneratedMessage> generateEvents() {
        final Task task1 = newTask(ALPHA, "Annotate internal API", "Use @Internal annotation.");
        final TaskId taskId1 = task1.getId();

        return ImmutableList.of(
                ProjectCreated.newBuilder().setProject(newProject(ALPHA.getCode(), "Alpha", "Initial public release.")).build(),
                TaskCreated.newBuilder().setTask(task1).build(),
                TaskAssigned.newBuilder()
                        .setTaskId(taskId1)
                        .setAssignee(randomSelectUser()).build(),
                TaskCreated.newBuilder().setTask(newTask(ALPHA, "Check code coverage", "")).build(),
                TaskCreated.newBuilder().setTask(newTask(ALPHA, "Verify JavaDocs", "")).build(),
                TaskDone.newBuilder().setTaskId(taskId1).build(),
                TaskCreated.newBuilder().setTask(newTask(ALPHA, "Blog post", "Announce the release at the blog.")).build()
        );
    }

    //@formatter:off
    private SampleData() {}
}
