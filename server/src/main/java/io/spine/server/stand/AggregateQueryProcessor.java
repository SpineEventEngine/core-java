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
package io.spine.server.stand;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.client.Query;
import io.spine.client.Target;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.RecordReadRequest;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;

/**
 * Processes the queries targeting {@link io.spine.server.aggregate.Aggregate Aggregate} state.
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

    private final Function<EntityId, AggregateStateId> stateIdTransformer =
            new Function<EntityId, AggregateStateId>() {
                @Nullable
                @Override
                public AggregateStateId apply(@Nullable EntityId input) {
                    checkNotNull(input);

                    Any rawId = input.getId();
                    Message unpackedId = AnyPacker.unpack(rawId);
                    AggregateStateId stateId = AggregateStateId.of(unpackedId, type);
                    return stateId;
                }
            };

    @Override
    public ImmutableCollection<Any> process(Query query) {

        ImmutableList.Builder<Any> resultBuilder = ImmutableList.builder();

        Iterator<EntityRecord> stateRecords;
        Target target = query.getTarget();
        FieldMask fieldMask = query.getFieldMask();
        boolean shouldApplyFieldMask = !fieldMask.getPathsList()
                                                       .isEmpty();
        if (target.getIncludeAll()) {
            stateRecords = shouldApplyFieldMask
                           ? standStorage.readAllByType(type, fieldMask)
                           : standStorage.readAllByType(type);
        } else {
            stateRecords = doFetchWithFilters(target, fieldMask);
        }

        while (stateRecords.hasNext()) {
            EntityRecord record = stateRecords.next();
            Any state = record.getState();
            resultBuilder.add(state);
        }

        ImmutableList<Any> result = resultBuilder.build();
        return result;
    }

    private Iterator<EntityRecord> doFetchWithFilters(Target target, FieldMask fieldMask) {
        EntityFilters filters = target.getFilters();
        boolean idsAreDefined = !filters.getIdFilter()
                                              .getIdsList()
                                              .isEmpty();
        if (!idsAreDefined) {
            return ImmutableList.<EntityRecord>of().iterator();
        }

        EntityIdFilter idFilter = filters.getIdFilter();
        Collection<AggregateStateId> stateIds = Collections2.transform(idFilter.getIdsList(),
                                                                             stateIdTransformer);

        Iterator<EntityRecord> result = stateIds.size() == 1
                ? readOne(stateIds.iterator()
                                  .next(), fieldMask)
                : readMany(stateIds, fieldMask);

        return result;
    }

    private Iterator<EntityRecord> readOne(AggregateStateId singleId, FieldMask fieldMask) {
        boolean shouldApplyFieldMask = !fieldMask.getPathsList()
                                                       .isEmpty();
        RecordReadRequest<AggregateStateId> request = new RecordReadRequest<>(singleId);
        Optional<EntityRecord> singleResult = shouldApplyFieldMask
                                                    ? standStorage.read(request, fieldMask)
                                                    : standStorage.read(request);
        Iterator<EntityRecord> result;
        if (!singleResult.isPresent()) {
            result = Collections.emptyIterator();
        } else {
            result = Collections.singleton(singleResult.get())
                                .iterator();
        }
        return result;
    }

    private Iterator<EntityRecord> readMany(Collection<AggregateStateId> stateIds,
                                            FieldMask fieldMask) {
        boolean applyFieldMask = !fieldMask.getPathsList()
                                                 .isEmpty();
        Iterator<EntityRecord> bulkReadResults = applyFieldMask
                ? standStorage.readMultiple(stateIds, fieldMask)
                : standStorage.readMultiple(stateIds);
        Iterator<EntityRecord> result = Iterators.filter(bulkReadResults, notNull());
        return result;
    }
}
