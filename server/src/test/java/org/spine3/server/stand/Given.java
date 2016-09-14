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

package org.spine3.server.stand;

import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.BoundedContext;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.event.ProjectCreated;

/**
 * @author Dmytro Dashenkov
 */
/*package*/ class Given {

    /*package*/static class StandTestProjectionRepository extends ProjectionRepository<ProjectId, StandTestProjection, Project> {
        /*package*/ StandTestProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class StandTestProjection extends Projection<ProjectId, Project> {
        /**
         * Creates a new instance.
         *
         * Required to be public.
         *
         * @param id the ID for the new instance
         * @throws IllegalArgumentException if the ID is not of one of the supported types
         */
        public StandTestProjection(ProjectId id) {
            super(id);
        }
    }

    /*package*/ static Event validEvent() {
        return Event.newBuilder()
                    .setMessage(AnyPacker.pack(ProjectCreated.newBuilder()
                                                             .setProjectId(ProjectId.newBuilder().setId("12345AD0"))
                                                             .build())
                    .toBuilder().setTypeUrl(ProjectCreated.getDescriptor().getFullName()).build())
                    .setContext(EventContext.getDefaultInstance())
                    .build();
    }
}
