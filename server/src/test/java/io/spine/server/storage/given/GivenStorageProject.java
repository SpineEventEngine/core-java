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

import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;
import io.spine.test.storage.StgTask;

import static java.lang.String.format;
import static java.lang.System.nanoTime;

public final class GivenStorageProject {

    /**
     * Prevents this utility from instantiation.
     */
    private GivenStorageProject() {
    }

    /**
     * Creates an unique {@code EntityState} with the specified ID.
     *
     * <p>Two calls for the same ID should return messages, which are not equal.
     *
     * @param id
     *         the ID for the message
     * @return the unique {@code EntityState}
     */
    public static StgProject newState(StgProjectId id) {
        String uniqueName = format("record-storage-test-%s-%s", id.getId(), nanoTime());
        StgProject project = StgProject
                .newBuilder()
                .setId(id)
                .setStatus(StgProject.Status.CREATED)
                .setName(uniqueName)
                .addTask(StgTask.getDefaultInstance())
                .build();
        return project;
    }
}
