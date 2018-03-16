/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
package io.spine.server.projection.given;

import io.spine.Identifier;
import io.spine.core.Event;
import io.spine.core.Subscribe;
import io.spine.server.command.TestEventFactory;
import io.spine.server.projection.Projection;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectVBuilder;
import io.spine.test.projection.event.PrjProjectCreated;

import javax.annotation.Nullable;

import static io.spine.protobuf.AnyPacker.pack;

/**
 * @author Alex Tymchenko
 */
public class ProjectionEventDeliveryTestEnv {

    /** Prevents instantiation of this utility class. */
    private ProjectionEventDeliveryTestEnv() {}

    public static Event projectCreated() {
        final ProjectId projectId = projectId();
        final TestEventFactory eventFactory =
                TestEventFactory.newInstance(
                        pack(projectId),
                        ProjectionEventDeliveryTestEnv.class
                );

        final PrjProjectCreated msg = PrjProjectCreated.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build();

        final Event result = eventFactory.createEvent(msg);
        return result;
    }

    private static ProjectId projectId() {
        return ProjectId.newBuilder()
                        .setId(Identifier.newUuid())
                        .build();
    }

    public static class ProjectDetails extends Projection<ProjectId, Project, ProjectVBuilder> {

        private static PrjProjectCreated receivedEvent = null;

        protected ProjectDetails(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod") // It's fine in tests.
        @Subscribe
        public void on(PrjProjectCreated event) {
            receivedEvent = event;
        }

        @Nullable
        public static PrjProjectCreated getEventReceived() {
            return receivedEvent;
        }
    }

}
