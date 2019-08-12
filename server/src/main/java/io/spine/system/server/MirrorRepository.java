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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import io.spine.annotation.Internal;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.ResponseFormat;
import io.spine.client.Target;
import io.spine.client.TargetFilters;
import io.spine.core.MessageId;
import io.spine.server.entity.Repository;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRouting;
import io.spine.system.server.event.EntityLifecycleEvent;
import io.spine.type.TypeUrl;

import java.util.Iterator;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Streams.stream;
import static com.google.protobuf.util.FieldMaskUtil.fromFieldNumbers;
import static io.spine.system.server.Mirror.ID_FIELD_NUMBER;
import static io.spine.system.server.Mirror.STATE_FIELD_NUMBER;
import static io.spine.system.server.Mirror.VERSION_FIELD_NUMBER;
import static io.spine.system.server.MirrorProjection.buildFilters;

/**
 * The repository for {@link Mirror} projections.
 *
 * <p>The mirrored entity types are gathered at runtime and are usually
 * {@linkplain #registerMirroredType(Repository) registered} by the corresponding entity
 * repositories.
 */
@Internal
public final class MirrorRepository
        extends ProjectionRepository<MirrorId, MirrorProjection, Mirror> {

    private static final FieldMask AGGREGATE_STATE_WITH_VERSION = fromFieldNumbers(
            Mirror.class, ID_FIELD_NUMBER, STATE_FIELD_NUMBER, VERSION_FIELD_NUMBER
    );

    private final Set<TypeUrl> mirroredTypes = newHashSet();

    @Override
    protected void setupEventRouting(EventRouting<MirrorId> routing) {
        super.setupEventRouting(routing);
        routing.route(EntityLifecycleEvent.class,
                      (message, context) -> targetsFrom(message));
    }

    private Set<MirrorId> targetsFrom(EntityLifecycleEvent event) {
        TypeUrl type = event.entityType();
        MessageId entityId = event.getEntity();
        return isMirroring(type)
               ? ImmutableSet.of(idFrom(entityId))
               : ImmutableSet.of();
    }

    /**
     * Registers a {@code repository} state type as mirrored by this {@code MirrorRepository}.
     */
    public void registerMirroredType(Repository<?, ?> repository) {
        addMirroredType(repository.entityStateType());
    }

    /**
     * Returns {@code true} if the given type is mirrored by this {@code MirrorRepository}.
     */
    public boolean isMirroring(TypeUrl type) {
        return mirroredTypes.contains(type);
    }

    @VisibleForTesting
    void addMirroredType(TypeUrl type) {
        mirroredTypes.add(type);
    }

    private static MirrorId idFrom(MessageId messageId) {
        Any any = messageId.getId();
        MirrorId result = MirrorId
                .newBuilder()
                .setValue(any)
                .setTypeUrl(messageId.getTypeUrl())
                .build();
        return result;
    }

    /**
     * Executes the given query upon the aggregate states of the target type.
     *
     * <p>In a multitenant environment, this method should only be invoked if the current tenant is
     * set to the one in the {@link Query}.
     *
     * @param query
     *         an aggregate query
     * @return an {@code Iterator} over the result aggregate states
     * @see SystemReadSide#readDomainAggregate(Query)
     */
    Iterator<EntityStateWithVersion> execute(Query query) {
        ResponseFormat requestedFormat = query.getFormat();
        FieldMask aggregateFields = requestedFormat.getFieldMask();
        ResponseFormat responseFormat = requestedFormat
                .toBuilder()
                .setFieldMask(AGGREGATE_STATE_WITH_VERSION)
                .vBuild();
        Target target = query.getTarget();
        TargetFilters filters = buildFilters(target);
        Iterator<MirrorProjection> mirrors = find(filters, responseFormat);
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
        EntityStateWithVersion result = EntityStateWithVersion
                .newBuilder()
                .setState(mirror.aggregateState(requiredFields))
                .setVersion(mirror.aggregateVersion())
                .build();
        return result;
    }
}
