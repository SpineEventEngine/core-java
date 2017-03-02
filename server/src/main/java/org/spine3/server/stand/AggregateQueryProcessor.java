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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.client.Query;
import org.spine3.client.Target;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.entity.EntityRecord;

import javax.annotation.Nullable;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Processes the queries targeting {@link org.spine3.server.aggregate.Aggregate Aggregate} state.
 *
 * @author Alex Tymchenko
 */
class AggregateQueryProcessor implements QueryProcessor {

    private final StandStorage standStorage;
    private final TypeUrl type;

    AggregateQueryProcessor(StandStorage standStorage, TypeUrl type) {
        this.standStorage = standStorage;
        this.type = type;
    }

    private final Function<EntityId, AggregateStateId> stateIdTransformer = new Function<EntityId, AggregateStateId>() {
        @Nullable
        @Override
        public AggregateStateId apply(@Nullable EntityId input) {
            checkNotNull(input);

            final Any rawId = input.getId();
            final Message unpackedId = AnyPacker.unpack(rawId);
            final AggregateStateId stateId = AggregateStateId.of(unpackedId, type);
            return stateId;
        }
    };

    @Override
    public ImmutableCollection<Any> process(Query query) {

        final ImmutableList.Builder<Any> resultBuilder = ImmutableList.builder();

        ImmutableCollection<EntityRecord> stateRecords;
        final Target target = query.getTarget();
        final FieldMask fieldMask = query.getFieldMask();
        final boolean shouldApplyFieldMask = !fieldMask.getPathsList()
                                                       .isEmpty();
        if (target.getIncludeAll()) {
            stateRecords = shouldApplyFieldMask
                           ? standStorage.readAllByType(type, fieldMask)
                           : standStorage.readAllByType(type);
        } else {
            stateRecords = doFetchWithFilters(target, fieldMask);
        }

        for (EntityRecord record : stateRecords) {
            final Any state = record.getState();
            resultBuilder.add(state);
        }

        final ImmutableList<Any> result = resultBuilder.build();
        return result;
    }

    private ImmutableCollection<EntityRecord> doFetchWithFilters(Target target, FieldMask fieldMask) {
        final EntityFilters filters = target.getFilters();
        final boolean idsAreDefined = !filters.getIdFilter()
                                              .getIdsList()
                                              .isEmpty();
        if (!idsAreDefined) {
            return ImmutableList.of();
        }

        final EntityIdFilter idFilter = filters.getIdFilter();
        final Collection<AggregateStateId> stateIds = Collections2.transform(idFilter.getIdsList(),
                                                                             stateIdTransformer);

        final ImmutableCollection<EntityRecord> result = stateIds.size() == 1
                 ? readOne(stateIds.iterator()
                                   .next(), fieldMask)
                 : readMany(stateIds, fieldMask);

        return result;
    }

    private ImmutableCollection<EntityRecord> readOne(AggregateStateId singleId,
                                                      FieldMask fieldMask) {
        final boolean shouldApplyFieldMask = !fieldMask.getPathsList()
                                                       .isEmpty();

        ImmutableCollection<EntityRecord> result;
        final Optional<EntityRecord> singleResult = shouldApplyFieldMask
                                                           ? standStorage.read(singleId, fieldMask)
                                                           : standStorage.read(singleId);
        if (!singleResult.isPresent()) {
            result = ImmutableList.of();
        } else {
            result = ImmutableList.of(singleResult.get());
        }
        return result;
    }

    private ImmutableCollection<EntityRecord> readMany(Collection<AggregateStateId> stateIds,
                                                       FieldMask fieldMask) {
        final boolean applyFieldMask = !fieldMask.getPathsList()
                                                 .isEmpty();
        final Iterable<EntityRecord> bulkReadResults = applyFieldMask
                                                              ? standStorage.readMultiple(stateIds, fieldMask)
                                                              : standStorage.readMultiple(stateIds);
        final ImmutableCollection<EntityRecord> result
                = FluentIterable.from(bulkReadResults)
                               .filter(new Predicate<EntityRecord>() {
                                   @Override
                                   public boolean apply(@Nullable EntityRecord input) {
                                       return input != null;
                                   }
                               })
                               .toList();
        return result;
    }
}
