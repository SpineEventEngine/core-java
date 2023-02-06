/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.type.TypeName;
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
import static java.lang.String.format;

/**
 * The repository for {@link Mirror} projections.
 *
 * <p>The mirrored entity types are gathered at runtime and are usually
 * {@linkplain #registerMirroredType(Repository) registered} by the corresponding entity
 * repositories.
 *
 * <p>The catch-up is disabled for the instances of this repository.
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

    /**
     * Returns the custom type URL for {@code Mirror} type to use during the {@code Inbox}
     * registration in {@code Delivery}.
     *
     * <p>The prefix part in the resulting type URL includes the name of the Bounded Context
     * in which this repository is registered.
     *
     * <p>It is a workaround preventing some known issues of delivering signals
     * to {@code Mirror} instances in an application consisting of several Bounded Contexts.
     * As long as each of the Bounded Contexts may have their own {@code Mirror}
     * as a system entity, application-wide {@code Delivery} must have a way
     * to distinguish the inboxes for each of such {@code Mirror} entity types.
     * Inserting the name of the Bounded Context into the type URL helps to achieve this goal.
     *
     * <p>This type URL must be used solely for {@code Inbox}-specific set up,
     * and must not be associated with any entity state types, since its "prefix" part
     * is customized.
     *
     * <p>Please note, this feature along with {@code Mirror} itself is removed in Spine 2.x.
     */
    @Internal
    @Override
    @SuppressWarnings("resource")   /* Not using any resources from `context()`. */
    protected TypeUrl inboxStateType() {
        String contextName = context().name()
                                      .value();
        TypeUrl originalValue = super.inboxStateType();
        String originalPrefix = originalValue.prefix();
        TypeName typeName = originalValue.toTypeName();

        String rawTypeUrl = format("%s@%s/%s", originalPrefix, contextName, typeName.value());
        TypeUrl result = TypeUrl.parse(rawTypeUrl);
        return result;
    }

    @Internal
    @Override
    protected boolean isCatchUpEnabled() {
        return false;
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
