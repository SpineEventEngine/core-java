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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.base.Identifiers;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.BoundedContext;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.server.event.Subscribe;
import org.spine3.server.projection.Projection;
import org.spine3.server.projection.ProjectionRepository;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.command.CreateProject;
import org.spine3.test.projection.event.ProjectCreated;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Dmytro Dashenkov
 */
/*package*/ class Given {

    /*package*/ static final int THREADS_COUNT_IN_POOL_EXECUTOR = 10;
    /*package*/ static final int SEVERAL = THREADS_COUNT_IN_POOL_EXECUTOR;

    private static final String PROJECT_UUID = Identifiers.newUuid();
    /*package*/ static final int AWAIT_SECONDS = 10;

    private Given() {
    }

    /*package*/ static class StandTestProjectionRepository extends ProjectionRepository<ProjectId, StandTestProjection, Project> {
        /*package*/ StandTestProjectionRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }

        @Override
        protected ProjectId getEntityId(Message event, EventContext context) {
            return ProjectId.newBuilder().setId(PROJECT_UUID).build();
        }
    }

    /*package*/ static class StandTestAggregateRepository extends AggregateRepository<ProjectId, StandTestAggregate> {

        /**
         * Creates a new repository instance.
         *
         * @param boundedContext the bounded context to which this repository belongs
         */
        /*package*/ StandTestAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class StandTestAggregate extends Aggregate<ProjectId, Any, Any.Builder> {

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
        public List<? extends Event> handle(CreateProject createProject, CommandContext context) {
            return null;
        }

        @Apply
        public void handle(ProjectCreated event) {
            // Do nothing
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

        @Subscribe
        public void handle(ProjectCreated event, EventContext context) {
            // Do nothing
        }
    }

     /*package*/ static Event validEvent() {
        return Event.newBuilder()
                    .setMessage(AnyPacker.pack(ProjectCreated.newBuilder()
                                                             .setProjectId(ProjectId.newBuilder().setId("12345AD0"))
                                                             .build())
                                         .toBuilder()
                                         .setTypeUrl(TypeUrl.SPINE_TYPE_URL_PREFIX + "/" + ProjectCreated.getDescriptor().getFullName())
                                         .build())
                    .setContext(EventContext.newBuilder()
                                            .setDoNotEnrich(true)
                                            .setCommandContext(CommandContext.getDefaultInstance())
                                            .setEventId(EventId.newBuilder()
                                                               .setUuid(Identifiers.newUuid())
                                                               .build()))
                    .build();
    }

    /*package*/ static Command validCommand() {
        return Command.newBuilder()
                      .setMessage(AnyPacker.pack(CreateProject.getDefaultInstance()))
                      .setContext(CommandContext.getDefaultInstance())
                      .build();
    }

    /*package*/ static ProjectionRepository<?, ?, ?> projectionRepo(BoundedContext context) {
        return new StandTestProjectionRepository(context);
    }

    /*package*/ static AggregateRepository<?, ?> aggregateRepo(BoundedContext context) {
        return new StandTestAggregateRepository(context);
    }

    /*package*/ static BoundedContext boundedContext(Stand stand, int concurrentThreads) {
        final Executor executor = concurrentThreads > 0 ? Executors.newFixedThreadPool(concurrentThreads) :
                                  MoreExecutors.directExecutor();

        return boundedContextBuilder(stand)
                             .setStandFunnelExecutor(executor)
                             .build();
    }

    private static BoundedContext.Builder boundedContextBuilder(Stand stand) {
        return BoundedContext.newBuilder()
                             .setStand(stand)
                             .setStorageFactory(InMemoryStorageFactory.getInstance());
    }
}
