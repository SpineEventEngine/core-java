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

package io.spine.system.server;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.FieldMask;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.EntityStateWithVersionVBuilder;
import io.spine.client.Query;
import io.spine.client.Target;
import io.spine.client.TargetFilters;
import io.spine.code.proto.EntityStateOption;
import io.spine.option.EntityOption;
import io.spine.option.EntityOption.Kind;
import io.spine.system.server.event.EntityArchived;
import io.spine.system.server.event.EntityDeleted;
import io.spine.system.server.event.EntityExtractedFromArchive;
import io.spine.system.server.event.EntityRestored;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.type.TypeUrl;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Streams.stream;
import static com.google.protobuf.util.FieldMaskUtil.fromFieldNumbers;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.option.EntityOption.Kind.KIND_UNKNOWN;
import static io.spine.system.server.Mirror.ID_FIELD_NUMBER;
import static io.spine.system.server.Mirror.STATE_FIELD_NUMBER;
import static io.spine.system.server.Mirror.VERSION_FIELD_NUMBER;
import static io.spine.system.server.MirrorProjection.buildFilters;

/**
 * The repository for {@link Mirror} projections.
 *
 * <p>An entity has a mirror if all of the following conditions are met:
 * <ul>
 *     <li>the entity repository is registered in a domain bounded context;
 *     <li>the entity state is marked as an {@link EntityOption.Kind#AGGREGATE AGGREGATE}.
 * </ul>
 *
 * <p>In other cases, an entity won't have a {@link Mirror}.
 */
final class MirrorRepository
        extends SystemProjectionRepository<MirrorId, MirrorProjection, Mirror> {

    private static final FieldMask AGGREGATE_STATE_WITH_VERSION =
            fromFieldNumbers(Mirror.class,
                             ID_FIELD_NUMBER, STATE_FIELD_NUMBER, VERSION_FIELD_NUMBER);

    @Override
    public void onRegistered() {
        super.onRegistered();
        prepareRouting();
    }

    private void prepareRouting() {
        eventRouting()
                .route(EntityStateChanged.class,
                       (message, context) -> targetsFrom(message.getId()))
                .route(EntityArchived.class,
                       (message, context) -> targetsFrom(message.getId()))
                .route(EntityDeleted.class,
                       (message, context) -> targetsFrom(message.getId()))
                .route(EntityExtractedFromArchive.class,
                       (message, context) -> targetsFrom(message.getId()))
                .route(EntityRestored.class,
                       (message, context) -> targetsFrom(message.getId()));
    }

    private static Set<MirrorId> targetsFrom(EntityHistoryId historyId) {
        TypeUrl type = TypeUrl.parse(historyId.getTypeUrl());
        boolean shouldMirror = shouldMirror(type);
        return shouldMirror
               ? ImmutableSet.of(idFrom(historyId))
               : ImmutableSet.of();
    }

    private static boolean shouldMirror(TypeUrl type) {
        Descriptor descriptor = type.toTypeName()
                                    .messageDescriptor();
        Optional<EntityOption> option = EntityStateOption.valueOf(descriptor);
        Kind kind = option.map(EntityOption::getKind)
                          .orElse(KIND_UNKNOWN);
        boolean aggregate = kind == AGGREGATE;
        return aggregate;
    }

    private static MirrorId idFrom(EntityHistoryId historyId) {
        Any any = historyId.getEntityId()
                           .getId();
        MirrorId result = MirrorId
                .newBuilder()
                .setValue(any)
                .setTypeUrl(historyId.getTypeUrl())
                .build();
        return result;
    }

    /**
     * Executes the given query upon the aggregate states of the target type.
     *
     * <p>In a multitenant environment, this method should only be invoked if the current tenant is
     * set to the one in the {@link Query}.
     *
     * @param query an aggregate query
     * @return an {@code Iterator} over the result aggregate states
     * @see SystemReadSide#readDomainAggregate(Query)
     */
    Iterator<EntityStateWithVersion> execute(Query query) {
        FieldMask aggregateFields = query.getFieldMask();
        Target target = query.getTarget();
        TargetFilters filters = buildFilters(target);
        Iterator<MirrorProjection> mirrors = find(filters, 
                                                  query.getOrderBy(), 
                                                  query.getPagination(),
                                                  AGGREGATE_STATE_WITH_VERSION);
        Iterator<EntityStateWithVersion> result = aggregateStates(mirrors, aggregateFields);
        return result;
    }

    private static Iterator<EntityStateWithVersion>
    aggregateStates(Iterator<MirrorProjection> projections, FieldMask requiredFields) {
        Iterator<EntityStateWithVersion> result = stream(projections)
                .map(mirror -> toAggregateState(mirror, requiredFields))
                .iterator();
        return result;
    }

    private static EntityStateWithVersion
    toAggregateState(MirrorProjection mirror, FieldMask requiredFields) {
        EntityStateWithVersion result = EntityStateWithVersionVBuilder
                .newBuilder()
                .setState(mirror.aggregateState(requiredFields))
                .setVersion(mirror.aggregateVersion())
                .build();
        return result;
    }
}
