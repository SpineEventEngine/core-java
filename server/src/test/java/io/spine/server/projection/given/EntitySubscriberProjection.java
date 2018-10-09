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

package io.spine.server.projection.given;

import com.google.protobuf.Message;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.system.server.EntityStateChanged;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.ProjectTaskNamesVBuilder;
import io.spine.test.projection.Task;

import java.util.List;

import static com.google.common.collect.ImmutableSet.of;
import static io.spine.protobuf.AnyPacker.unpack;
import static java.util.stream.Collectors.toList;

public final class EntitySubscriberProjection
        extends Projection<ProjectId, ProjectTaskNames, ProjectTaskNamesVBuilder> {

    public EntitySubscriberProjection(ProjectId id) {
        super(id);
    }

    @Subscribe
    public void onUpdate(Project aggregateState) {
        List<String> taskNames = aggregateState.getTaskList()
                                               .stream()
                                               .map(Task::getTitle)
                                               .collect(toList());
        getBuilder().setProjectId(aggregateState.getId())
                    .setProjectName(aggregateState.getName())
                    .clearTaskName()
                    .addAllTaskName(taskNames);
    }

    public static final class Repository
            extends ProjectionRepository<ProjectId, EntitySubscriberProjection, ProjectTaskNames> {

        @Override
        public void onRegistered() {
            super.onRegistered();
            getEventRouting().route(EntityStateChanged.class,
                                    (event, context) -> {
                                        Message state = unpack(event.getNewState());
                                        if (state instanceof Project) {
                                            Project project = (Project) state;
                                            return of(project.getId());
                                        } else {
                                            return of();
                                        }
                                    });
        }
    }
}
