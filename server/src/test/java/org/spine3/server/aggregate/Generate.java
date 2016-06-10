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

package org.spine3.server.aggregate;


import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.test.aggregate.event.TaskAdded;

import static org.spine3.base.Identifiers.newUuid;

public class Generate {
    public static class AggregateId {

        private AggregateId() {
        }

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder().setId(uuid).build();
        }

        /**
         * Creates a new ProjectId with the given UUID value.
         *
         * @param uuid the project id
         * @return ProjectId instance
         */
        public static ProjectId newProjectId(String uuid) {
            return ProjectId.newBuilder().setId(uuid).build();
        }

    }


    public static class EventMessage {

        private EventMessage() {
        }

        public static ProjectCreated projectCreatedMsg(ProjectId id) {
            return ProjectCreated.newBuilder().setProjectId(id).build();
        }

        public static TaskAdded taskAddedMsg(ProjectId id) {
            return TaskAdded.newBuilder().setProjectId(id).build();
        }
    }
}
