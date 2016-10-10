/*
 *
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
 *
 */
package org.spine3.server.stand;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Queries;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.client.Query;
import org.spine3.client.Target;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.EntityRepository;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.StandStorage;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Query processing helper utility.
 *
 * @author Alex Tymchenko
 */
/* package */
@SuppressWarnings("UtilityClass")
class QueryProcessor {

    private QueryProcessor() {
    }

    /**
     * Performs query processing as a part of {@link Stand#execute(Query, StreamObserver)}.
     *
     * @param query               an instance of {@code Query} to process
     * @param standStorage        internal storage of {@link Stand}
     * @param aggregateTypes      {@link org.spine3.server.aggregate.Aggregate} types exposed by the {@code Stand}
     * @param typeToRepositoryMap map of exposed types and related repositories registered in the {@code Stand}
     * @return the query result
     */
    /* package */
    static ImmutableCollection<Any> processQuery(Query query,
                                                 StandStorage standStorage,
                                                 Set<TypeUrl> aggregateTypes,
                                                 Map<TypeUrl, EntityRepository<?, ? extends Entity, ? extends Message>> typeToRepositoryMap) {
        final ImmutableList.Builder<Any> resultBuilder = ImmutableList.builder();

        final TypeUrl typeUrl = Queries.typeOf(query);
        checkNotNull(typeUrl, "Target type unknown");

        final EntityRepository<?, ? extends Entity, ? extends Message> repository = typeToRepositoryMap.get(typeUrl);
        if (repository != null) {

            // the target references an entity state
            final ImmutableCollection<? extends Entity> entities = fetchFromEntityRepository(query, repository);
            feedEntitiesToBuilder(resultBuilder, entities);

        } else if (aggregateTypes.contains(typeUrl)) {

            // the target relates to an {@code Aggregate} state
            final ImmutableCollection<EntityStorageRecord> stateRecords = fetchFromStandStorage(query, standStorage, typeUrl);
            feedStateRecordsToBuilder(resultBuilder, stateRecords);
        }

        final ImmutableList<Any> result = resultBuilder.build();
        return result;
    }

    private static ImmutableCollection<? extends Entity> fetchFromEntityRepository(
            Query query,
            EntityRepository<?, ? extends Entity, ?> repository) {

        final ImmutableCollection<? extends Entity> result;
        final Target target = query.getTarget();
        final FieldMask fieldMask = query.getFieldMask();

        if (target.getIncludeAll() && fieldMask.getPathsList()
                                               .isEmpty()) {
            result = repository.loadAll();
        } else {
            final EntityFilters filters = target.getFilters();
            result = repository.find(filters, fieldMask);
        }
        return result;
    }

    private static void feedEntitiesToBuilder(ImmutableList.Builder<Any> resultBuilder,
                                              ImmutableCollection<? extends Entity> all) {
        for (Entity record : all) {
            final Message state = record.getState();
            final Any packedState = AnyPacker.pack(state);
            resultBuilder.add(packedState);
        }
    }

    private static void feedStateRecordsToBuilder(ImmutableList.Builder<Any> resultBuilder,
                                                  ImmutableCollection<EntityStorageRecord> all) {
        for (EntityStorageRecord record : all) {
            final Any state = record.getState();
            resultBuilder.add(state);
        }
    }

    private static ImmutableCollection<EntityStorageRecord> fetchFromStandStorage(Query query,
                                                                                  StandStorage standStorage,
                                                                                  final TypeUrl typeUrl) {
        ImmutableCollection<EntityStorageRecord> result;
        final Target target = query.getTarget();
        final FieldMask fieldMask = query.getFieldMask();
        final boolean shouldApplyFieldMask = !fieldMask.getPathsList()
                                                       .isEmpty();
        if (target.getIncludeAll()) {
            result = shouldApplyFieldMask ?
                     standStorage.readAllByType(typeUrl, fieldMask) :
                     standStorage.readAllByType(typeUrl);
        } else {
            result = doFetchWithFilters(standStorage, typeUrl, target, fieldMask);
        }

        return result;
    }

    private static ImmutableCollection<EntityStorageRecord> doFetchWithFilters(StandStorage standStorage,
                                                                               TypeUrl typeUrl,
                                                                               Target target,
                                                                               FieldMask fieldMask) {
        ImmutableCollection<EntityStorageRecord> result;
        final EntityFilters filters = target.getFilters();
        final boolean shouldApplyFieldMask = !fieldMask.getPathsList()
                                                       .isEmpty();
        final boolean idsAreDefined = !filters.getIdFilter()
                                              .getIdsList()
                                              .isEmpty();
        if (idsAreDefined) {
            final EntityIdFilter idFilter = filters.getIdFilter();
            final Collection<AggregateStateId> stateIds = Collections2.transform(idFilter.getIdsList(),
                                                                                 aggregateStateIdTransformer(typeUrl));
            if (stateIds.size() == 1) {
                // no need to trigger bulk reading.
                // may be more effective, as bulk reading implies additional time and performance expenses.
                final AggregateStateId singleId = stateIds.iterator()
                                                          .next();
                final EntityStorageRecord singleResult = shouldApplyFieldMask ?
                                                         standStorage.read(singleId, fieldMask) :
                                                         standStorage.read(singleId);
                result = ImmutableList.of(singleResult);
            } else {
                result = handleBulkRead(standStorage, stateIds, fieldMask, shouldApplyFieldMask);
            }
        } else {
            result = ImmutableList.of();
        }
        return result;
    }

    private static ImmutableCollection<EntityStorageRecord> handleBulkRead(StandStorage standStorage,
                                                                           Collection<AggregateStateId> stateIds,
                                                                           FieldMask fieldMask,
                                                                           boolean applyFieldMask) {
        ImmutableCollection<EntityStorageRecord> result;
        final Iterable<EntityStorageRecord> bulkReadResults = applyFieldMask ?
                                                              standStorage.readBulk(stateIds, fieldMask) :
                                                              standStorage.readBulk(stateIds);
        result = FluentIterable.from(bulkReadResults)
                               .filter(new Predicate<EntityStorageRecord>() {
                                   @Override
                                   public boolean apply(@Nullable EntityStorageRecord input) {
                                       return input != null;
                                   }
                               })
                               .toList();
        return result;
    }

    private static Function<EntityId, AggregateStateId> aggregateStateIdTransformer(final TypeUrl typeUrl) {
        return new Function<EntityId, AggregateStateId>() {
            @Nullable
            @Override
            public AggregateStateId apply(@Nullable EntityId input) {
                checkNotNull(input);

                final Any rawId = input.getId();
                final Message unpackedId = AnyPacker.unpack(rawId);
                final AggregateStateId stateId = AggregateStateId.of(unpackedId, typeUrl);
                return stateId;
            }
        };
    }
}
