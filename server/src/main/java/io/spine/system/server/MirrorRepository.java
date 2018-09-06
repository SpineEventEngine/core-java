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

package io.spine.system.server;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.FieldMask;
import io.spine.client.EntityFilters;
import io.spine.client.Query;
import io.spine.client.Target;
import io.spine.option.EntityOption;
import io.spine.option.EntityOption.Kind;
import io.spine.server.projection.ProjectionRepository;
import io.spine.type.TypeUrl;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Streams.stream;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.option.EntityOption.Kind.KIND_UNKNOWN;
import static io.spine.option.Options.option;
import static io.spine.option.OptionsProto.entity;
import static io.spine.server.entity.FieldMasks.maskOf;
import static io.spine.system.server.Mirror.STATE_FIELD_NUMBER;
import static io.spine.system.server.MirrorProjection.buildFilters;

/**
 * @author Dmytro Dashenkov
 */
public class MirrorRepository extends ProjectionRepository<MirrorId, MirrorProjection, Mirror> {

    private static final FieldMask AGGREGATE_STATE_FIELD =
            maskOf(Mirror.getDescriptor(), STATE_FIELD_NUMBER);

    @Override
    public void onRegistered() {
        super.onRegistered();
        prepareRouting();
    }

    private void prepareRouting() {
        getEventRouting()
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
               ? of(idFrom(historyId))
               : of();
    }

    private static boolean shouldMirror(TypeUrl type) {
        Descriptor descriptor = type.toName()
                                    .getMessageDescriptor();
        Optional<EntityOption> option = option(descriptor, entity);
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
                .build();
        return result;
    }

    /**
     * Executes the given query upon the aggregate states of the target type.
     *
     * @param query an aggregate query
     * @return an {@code Iterator} over the result aggregate states
     */
    Iterator<Any> execute(Query query) {
        FieldMask aggregateFields = query.getFieldMask();
        Target target = query.getTarget();
        EntityFilters filters = buildFilters(target);
        Iterator<MirrorProjection> mirrors = find(filters, AGGREGATE_STATE_FIELD);
        Iterator<Any> result = aggregateStates(mirrors, aggregateFields);
        return result;
    }

    private static Iterator<Any> aggregateStates(Iterator<MirrorProjection> projections,
                                                 FieldMask requiredFields) {
        Iterator<Any> result = stream(projections)
                .map(mirror -> mirror.aggregateState(requiredFields))
                .iterator();
        return result;
    }
}
