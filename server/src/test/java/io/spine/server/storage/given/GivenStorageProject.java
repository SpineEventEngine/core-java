/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage.given;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProject.Status;
import io.spine.test.storage.StgProjectId;
import io.spine.test.storage.StgTask;

import static io.spine.base.Time.currentTime;
import static java.lang.String.format;
import static java.lang.System.nanoTime;

public final class GivenStorageProject {

    /**
     * Prevents this utility from instantiation.
     */
    private GivenStorageProject() {
    }

    /**
     * Creates a unique {@code EntityState} with the specified ID.
     *
     * <p>Two calls for the same ID should return messages, which are not equal, as the project
     * name is generated using the {@link System#nanoTime() System.nanoTime()}.
     *
     * <p>The status of the returned project is set to {@code CREATED}.
     *
     * <p>The due date for the returned project is set to one day ahead of the current time.
     *
     * @param id
     *         the ID for the message
     * @return the unique {@code EntityState}
     */
    public static StgProject newState(StgProjectId id) {
        Status status = Status.CREATED;
        Timestamp dueDate = Timestamps.add(currentTime(), Durations.fromDays(1));
        return newState(id, status, dueDate);
    }

    /**
     * Creates an {@code StgProject} state with the specified ID, project status and due date.
     *
     * @param id
     *         the ID for the message
     * @param status
     *         the status for the created project
     * @param dueDate
     *         the due date of the project
     * @return a new instance of {@code StgProject}
     */
    public static StgProject newState(StgProjectId id, Status status, Timestamp dueDate) {
        String uniqueName = format("record-storage-test-%s-%s", id.getId(), nanoTime());
        StgProject project = StgProject
                .newBuilder()
                .setId(id)
                .setStatus(status)
                .setName(uniqueName)
                .setDueDate(dueDate)
                .addTask(StgTask.getDefaultInstance())
                .build();
        return project;
    }
}
