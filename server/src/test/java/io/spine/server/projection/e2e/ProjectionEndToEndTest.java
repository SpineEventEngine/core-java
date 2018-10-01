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

package io.spine.server.projection.e2e;

import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.projection.given.EntitySubscriberProjection;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.GivenEventMessage;
import io.spine.server.projection.given.TestProjection;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjTaskAdded;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.testing.server.TestEventFactory.newInstance;
import static io.spine.testing.server.blackbox.VerifyState.exactly;

@DisplayName("Projection should")
class ProjectionEndToEndTest {

    @Test
    @DisplayName("receive entity state updates from other entities")
    void receiveUpdates() {
        PrjProjectCreated created = GivenEventMessage.projectCreated();
        PrjTaskAdded firstTaskAdded = GivenEventMessage.taskAdded();
        PrjTaskAdded secondTaskAdded = GivenEventMessage.taskAdded();
        ProjectId id = created.getProjectId();
        BlackBoxBoundedContext
                .newInstance()
                .with(new EntitySubscriberProjection.Repository(),
                      new TestProjection.Repository())
                .receivesEvents(event(id, created),
                                event(id, firstTaskAdded),
                                event(id, secondTaskAdded))
                .assertThat(exactly(ProjectTaskNames.class,
                                    of(ProjectTaskNames
                                               .newBuilder()
                                               .setProjectId(id)
                                               .setProjectName(created.getName())
                                               .addTaskName(firstTaskAdded.getTask().getTitle())
                                               .addTaskName(secondTaskAdded.getTask().getTitle())
                                               .build())));
    }

    private static Event event(ProjectId producer, EventMessage eventMessage) {
        TestEventFactory eventFactory = newInstance(producer, ProjectionEndToEndTest.class);
        Event result = eventFactory.createEvent(eventMessage);
        return result;
    }
}
