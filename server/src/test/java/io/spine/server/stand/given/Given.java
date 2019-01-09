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

package io.spine.server.stand.given;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.EventFactory;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.test.shared.EmptyAggregate;
import io.spine.server.test.shared.EmptyAggregateVBuilder;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectVBuilder;
import io.spine.test.projection.command.PrjCreateProject;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.testing.client.TestActorRequestFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.Tests.nullRef;

public class Given {

    public static final int THREADS_COUNT_IN_POOL_EXECUTOR = 10;
    public static final int SEVERAL = THREADS_COUNT_IN_POOL_EXECUTOR;
    public static final int AWAIT_SECONDS = 6;
    private static final String PROJECT_UUID = newUuid();

    private Given() {
    }

    public static Command validCommand() {
        TestActorRequestFactory requestFactory = TestActorRequestFactory.newInstance(Given.class);
        return requestFactory.command()
                             .create(PrjCreateProject.getDefaultInstance());
    }

    public static Event validEvent() {
        Command cmd = validCommand();
        ProjectId.Builder projectIdBuilder = ProjectId.newBuilder()
                                                      .setId("12345AD0");
        PrjProjectCreated eventMessage = PrjProjectCreated.newBuilder()
                                                          .setProjectId(projectIdBuilder)
                                                          .build();
        StringValue producerId = StringValue.of(Given.class.getSimpleName());
        EventFactory eventFactory = EventFactory.on(CommandEnvelope.of(cmd),
                                                    Identifier.pack(producerId));
        Event event = eventFactory.createEvent(eventMessage, nullRef());
        Event result = event.toBuilder()
                            .setContext(event.getContext()
                                             .toBuilder()
                                             .setEnrichment(Enrichment.newBuilder()
                                                                      .setDoNotEnrich(true))
                                             .build())
                            .build();
        return result;
    }

    public static ProjectionRepository<?, ?, ?> projectionRepo() {
        return new StandTestProjectionRepository();
    }

    public static AggregateRepository<ProjectId, StandTestAggregate> aggregateRepo() {
        return new StandTestAggregateRepository();
    }

    public static class StandTestProjectionRepository
            extends ProjectionRepository<ProjectId, StandTestProjection, Project> {

        private static final EventRoute<ProjectId, PrjProjectCreated> EVENT_TARGETS_FN =
                new EventRoute<ProjectId, PrjProjectCreated>() {
                    private static final long serialVersionUID = 0L;

                    @Override
                    public Set<ProjectId> apply(PrjProjectCreated message, EventContext context) {
                        return ImmutableSet.of(ProjectId.newBuilder()
                                                        .setId(PROJECT_UUID)
                                                        .build());
                    }
                };

        public StandTestProjectionRepository() {
            super();
            getEventRouting().route(PrjProjectCreated.class, EVENT_TARGETS_FN);
        }
    }

    public static class StandTestAggregateRepository
            extends AggregateRepository<ProjectId, StandTestAggregate> {
        private StandTestAggregateRepository() {
            super();
        }
    }

    public static class StandTestAggregate
            extends Aggregate<ProjectId, EmptyAggregate, EmptyAggregateVBuilder> {

        /**
         * Creates a new aggregate instance.
         *
         * @param id the ID for the new aggregate
         * @throws IllegalArgumentException if the ID is not of one of the supported types
         */
        public StandTestAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        List<? extends Message> handle(PrjCreateProject createProject, CommandContext context) {
            // In real life we would return a list with at least one element
            // populated with real data.
            return Collections.emptyList();
        }

        @Apply
        public void handle(PrjProjectCreated event) {
            // Do nothing
        }
    }

    public static class StandTestProjection
            extends Projection<ProjectId, Project, ProjectVBuilder> {

        public StandTestProjection(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("unused") // OK for test class.
        @Subscribe
        public void handle(PrjProjectCreated event, EventContext context) {
            getBuilder().setId(event.getProjectId());
        }
    }
}
