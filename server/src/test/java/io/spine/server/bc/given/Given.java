/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.bc.given;

import io.spine.test.bc.ProjectId;
import io.spine.test.bc.event.BcProjectCreated;
import io.spine.test.bc.event.BcProjectStarted;
import io.spine.test.bc.event.BcTaskAdded;

public class Given {

    private Given() {
    }

    public static class EventMessage {

        private EventMessage() {
        }

        public static BcProjectCreated projectCreated(ProjectId id) {
            return BcProjectCreated.newBuilder()
                                   .setProjectId(id)
                                   .build();
        }

        public static BcTaskAdded taskAdded(ProjectId id) {
            return BcTaskAdded.newBuilder()
                              .setProjectId(id)
                              .build();
        }

        public static BcProjectStarted projectStarted(ProjectId id) {
            return BcProjectStarted.newBuilder()
                                   .setProjectId(id)
                                   .build();
        }
    }
}
