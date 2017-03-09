/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Enrichment;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Identifiers;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.server.entity.idfunc.IdSetEventFunction;
import org.spine3.server.outbus.Subscribe;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.command.CreateProject;
import org.spine3.test.projection.event.ProjectCreated;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.spine3.protobuf.AnyPacker.pack;

/**
 * @author Dmytro Dashenkov
 */
class Given {

    static final int THREADS_COUNT_IN_POOL_EXECUTOR = 10;
    static final int SEVERAL = THREADS_COUNT_IN_POOL_EXECUTOR;
    static final int AWAIT_SECONDS = 6;
    private static final String PROJECT_UUID = Identifiers.newUuid();

    private Given() {
    }

    static Event validEvent() {
        return Event.newBuilder()
                    .setMessage(
                            pack(ProjectCreated.newBuilder()
                                               .setProjectId(ProjectId.newBuilder()
                                                                      .setId("12345AD0"))
                                               .build())
                                    .toBuilder()
                                    .setTypeUrl(TypeUrl.SPINE_TYPE_URL_PREFIX + '/' +
                                                ProjectCreated.getDescriptor()
                                                              .getFullName())
                                    .build())
                    .setContext(EventContext.newBuilder()
                                            .setEnrichment(Enrichment.newBuilder()
                                                                     .setDoNotEnrich(true))
                                            .setCommandContext(CommandContext.getDefaultInstance())
                                            .setEventId(EventId.newBuilder()
                                                               .setUuid(Identifiers.newUuid())
                                                               .build()))
                    .build();
    }

    static Command validCommand() {
        return Command.newBuilder()
                      .setMessage(pack(CreateProject.getDefaultInstance()))
                      .setContext(CommandContext.getDefaultInstance())
                      .build();
    }

    static ProjectionRepository<?, ?, ?> projectionRepo(BoundedContext context) {
        return new StandTestProjectionRepository(context);
    }

    static AggregateRepository<ProjectId, StandTestAggregate>
    aggregateRepo(BoundedContext context) {
        return new StandTestAggregateRepository(context);
    }

    static AggregateRepository<ProjectId, StandTestAggregate> aggregateRepo() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        return aggregateRepo(boundedContext);
    }

    static BoundedContext boundedContext(Stand stand, StandUpdateDelivery standUpdateDelivery) {
        return boundedContextBuilder(stand)
                .setStandUpdateDelivery(standUpdateDelivery)
                .build();
    }

    private static BoundedContext.Builder boundedContextBuilder(Stand stand) {
        return BoundedContext.newBuilder()
                             .setStand(stand);
    }

    static class StandTestProjectionRepository
            extends ProjectionRepository<ProjectId, StandTestProjection, Project> {
        StandTestProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext);
            addIdSetFunction(ProjectCreated.class, new IdSetEventFunction<ProjectId, ProjectCreated>() {
                @Override
                public Set<ProjectId> apply(ProjectCreated message, EventContext context) {
                    return ImmutableSet.of(ProjectId.newBuilder()
                                                    .setId(PROJECT_UUID)
                                                    .build());
                }
            });
        }
    }

    static class StandTestAggregateRepository
            extends AggregateRepository<ProjectId, StandTestAggregate> {

        /**
         * Creates a new repository instance.
         *
         * @param boundedContext the bounded context to which this repository belongs
         */
        StandTestAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    static class StandTestAggregate
            extends Aggregate<ProjectId, StringValue, StringValue.Builder> {

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
        List<? extends Message> handle(CreateProject createProject, CommandContext context) {
            // In real life we would return a list with at least one element
            // populated with real data.
            return Collections.emptyList();
        }

        @Apply
        public void handle(ProjectCreated event) {
            // Do nothing
        }
    }

    static class StandTestProjection extends Projection<ProjectId, Project> {

        public StandTestProjection(ProjectId id) {
            super(id);
        }

        @SuppressWarnings("unused") // OK for test class.
        @Subscribe
        public void handle(ProjectCreated event, EventContext context) {
            // Do nothing
        }
    }
}
