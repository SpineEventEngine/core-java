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
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.client.Target;
import io.spine.core.Subscribe;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.storage.Column;
import io.spine.server.projection.Projection;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.List;

import static io.spine.client.ColumnFilters.all;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.FieldMasks.applyMask;
import static java.util.stream.Collectors.toList;

/**
 * @author Dmytro Dashenkov
 */
public class MirrorProjection extends Projection<MirrorId, Mirror, MirrorVBuilder> {

    private static final String TYPE_COLUMN_NAME = "aggregate_type";
    private static final String TYPE_COLUMN_QUERY_NAME = "aggregateType";

    private MirrorProjection(MirrorId id) {
        super(id);
    }

    @Subscribe
    public void on(EntityStateChanged event) {
        getBuilder().setId(getId())
                    .setState(event.getNewState());
    }

    @Subscribe
    public void on(@SuppressWarnings("unused")
                   EntityArchived event) {
        LifecycleFlags flags = getBuilder().getLifecycle()
                                           .toBuilder()
                                           .setArchived(true)
                                           .build();
        getBuilder().setId(getId())
                    .setLifecycle(flags);
        setArchived(true);
    }

    @Subscribe
    public void on(@SuppressWarnings("unused")
                   EntityDeleted event) {
        LifecycleFlags flags = getBuilder().getLifecycle()
                                           .toBuilder()
                                           .setDeleted(true)
                                           .build();
        getBuilder().setId(getId())
                    .setLifecycle(flags);
        setDeleted(true);
    }

    @Subscribe
    public void on(@SuppressWarnings("unused")
                   EntityExtractedFromArchive event) {
        LifecycleFlags flags = getBuilder().getLifecycle()
                                           .toBuilder()
                                           .setArchived(false)
                                           .build();
        getBuilder().setId(getId())
                    .setLifecycle(flags);
        setArchived(false);
    }

    @Subscribe
    public void on(@SuppressWarnings("unused")
                   EntityRestored event) {
        LifecycleFlags flags = getBuilder().getLifecycle()
                                           .toBuilder()
                                           .setDeleted(false)
                                           .build();
        getBuilder().setId(getId())
                    .setLifecycle(flags);
        setDeleted(false);
    }

    static EntityFilters buildFilters(Target target) {
        EntityIdFilter idFilter = buildIdFilter(target);
        EntityFilters filters = target.getFilters();
        CompositeColumnFilter typeFilter = all(eq(TYPE_COLUMN_QUERY_NAME, target.getType()));
        EntityFilters appendedFilters = filters.toBuilder()
                                               .setIdFilter(idFilter)
                                               .addFilter(typeFilter)
                                               .build();
        return appendedFilters;
    }

    private static EntityIdFilter buildIdFilter(Target target) {
        if (target.getIncludeAll()) {
            return EntityIdFilter.getDefaultInstance();
        }
        List<EntityId> domainIds = target.getFilters()
                                         .getIdFilter()
                                         .getIdsList();
        if (domainIds.isEmpty()) {
            return EntityIdFilter.getDefaultInstance();
        }
        EntityIdFilter result = assembleSystemIdFilter(domainIds);
        return result;
    }

    private static EntityIdFilter assembleSystemIdFilter(Collection<EntityId> domainIds) {
        List<EntityId> mirrorIds = domainIds.stream()
                                            .map(MirrorProjection::domainToSystemId)
                                            .collect(toList());
        EntityIdFilter idFilter = EntityIdFilter
                .newBuilder()
                .addAllIds(mirrorIds)
                .build();
        return idFilter;
    }

    private static EntityId domainToSystemId(EntityId domainId) {
        Any aggregateId = domainId.getId();
        MirrorId mirrorId = MirrorId
                .newBuilder()
                .setValue(aggregateId)
                .build();
        Any anyId = pack(mirrorId);
        EntityId systemId = EntityId
                .newBuilder()
                .setId(anyId)
                .build();
        return systemId;
    }

    final Any aggregateState(FieldMask fields) {
        Any completeState = aggregateState();
        Message unpacked = unpack(completeState);
        Message trimmedState = applyMask(fields, unpacked, TypeUrl.ofEnclosed(completeState));
        Any result = pack(trimmedState);
        return result;
    }

    private Any aggregateState() {
        return getState().getState();
    }

    @Column(name = TYPE_COLUMN_NAME)
    public String getAggregateType() {
        return aggregateState().getTypeUrl();
    }
}
