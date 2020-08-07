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

package io.spine.server.aggregate.given.repo;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.core.EventContext;
import io.spine.core.MessageId;
import io.spine.core.Origin;
import io.spine.server.Identity;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.AggregateStorage;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.EntityRecordChange;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.test.aggregate.AggProject;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.event.AggProjectArchived;
import io.spine.test.aggregate.event.AggProjectDeleted;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.Set;

import static io.spine.protobuf.AnyPacker.pack;

/**
 * The repository of positive scenarios
 * {@linkplain io.spine.server.aggregate.given.repo.ProjectAggregate aggregates}.
 *
 * <p>It also widens visibility of the
 * {@link io.spine.server.aggregate.AggregateRepository#store(io.spine.server.aggregate.Aggregate)}
 * and
 * {@link io.spine.server.aggregate.AggregateRepository#aggregateStorage()} methods so they can be
 * used in this test env.
 */
public class ProjectAggregateRepository
        extends AggregateRepository<ProjectId, ProjectAggregate, AggProject> {

    public static final ProjectId troublesome = ProjectId.newBuilder()
                                                         .setUuid("INVALID_ID")
                                                         .build();

    private @Nullable AggregateStorage<ProjectId, AggProject> customStorage;

    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    @Override
    protected void setupEventRouting(EventRouting<ProjectId> routing) {
        super.setupEventRouting(routing);
        routing.route(AggProjectArchived.class,
                      new EventRoute<ProjectId, AggProjectArchived>() {
                          private static final long serialVersionUID = 0L;

                          @Override
                          public Set<ProjectId> apply(AggProjectArchived msg, EventContext ctx) {
                              return ImmutableSet.copyOf(msg.getChildProjectIdList());
                          }
                      })
               .route(AggProjectDeleted.class,
                      new EventRoute<ProjectId, AggProjectDeleted>() {
                          private static final long serialVersionUID = 0L;

                          @Override
                          public Set<ProjectId> apply(AggProjectDeleted msg, EventContext ctx) {
                              return ImmutableSet.copyOf(msg.getChildProjectIdList());
                          }
                      });
    }

    @Override
    public Optional<ProjectAggregate> find(ProjectId id) {
        if (id.equals(troublesome)) {
            return Optional.empty();
        }
        return super.find(id);
    }

    /**
     * Returns the storage for this repository.
     *
     * <p>The returning result may be customized by {@linkplain #injectStorage(AggregateStorage)
     * injecting} the custom storage.
     */
    @Override
    public AggregateStorage<ProjectId, AggProject> aggregateStorage() {
        if (customStorage != null) {
            return customStorage;
        }
        return super.aggregateStorage();
    }

    /**
     * Injects a storage to use for this repository.
     */
    public void injectStorage(AggregateStorage<ProjectId, AggProject> storage) {
        this.customStorage = storage;
    }

    void storeAggregate(ProjectAggregate aggregate) {
        store(aggregate);
        postStateUpdate(aggregate);
    }

    private void postStateUpdate(ProjectAggregate aggregate) {
        Any id = Identifier.pack(aggregate.id());
        Any state = pack(aggregate.state());
        EntityRecord previousRecord = EntityRecord
                .newBuilder()
                .setEntityId(id)
                .setState(Any.getDefaultInstance())
                .build();
        EntityRecord newRecord = previousRecord
                .toBuilder()
                .setEntityId(id)
                .setState(state)
                .build();
        EntityRecordChange change = EntityRecordChange
                .newBuilder()
                .setPreviousValue(previousRecord)
                .setNewValue(newRecord)
                .build();
        MessageId origin = Identity.byString("some-random-origin");
        lifecycleOf(aggregate.id())
                .onStateChanged(change, ImmutableSet.of(origin), Origin.getDefaultInstance());
    }
}
