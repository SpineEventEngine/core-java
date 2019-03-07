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

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.client.CompositeFilter;
import io.spine.client.IdFilter;
import io.spine.client.Target;
import io.spine.client.TargetFilters;
import io.spine.core.Subscribe;
import io.spine.core.Version;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.storage.Column;
import io.spine.server.projection.Projection;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.List;

import static io.spine.client.Filters.all;
import static io.spine.client.Filters.eq;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.FieldMasks.applyMask;
import static io.spine.validate.Validate.isDefault;
import static java.util.stream.Collectors.toList;

/**
 * The {@link Mirror} projection for domain aggregates.
 *
 * <p>This entity belongs to the system context. It mirrors the state of an aggregate. Not every
 * aggregate has a mirror projection. Refer to the {@link MirrorRepository} for more details.
 *
 * <p>The projection defines an {@link io.spine.server.entity.storage.EntityColumn EntityColumn}
 * which stored the {@linkplain #getAggregateType() type URL} of the state, which is mirrored.
 *
 * @implNote
 * Many subscriber methods of this class ignore their arguments. The argument of a subscriber method
 * is an event used by the framework to bind the method to the event type. The content of the event,
 * in those cases, is irrelevant.
 */
public final class MirrorProjection extends Projection<MirrorId, Mirror, MirrorVBuilder> {

    private static final String TYPE_COLUMN_NAME = "aggregate_type";
    private static final String TYPE_COLUMN_QUERY_NAME = "aggregateType";

    MirrorProjection(MirrorId id) {
        super(id);
    }

    @Subscribe
    void on(EntityStateChanged event) {
        builder().setId(id())
                 .setState(event.getNewState())
                 .setVersion(event.getNewVersion());
    }

    @Subscribe
    void on(@SuppressWarnings("unused") EntityArchived event) {
        MirrorVBuilder builder = builder();
        LifecycleFlags flags = builder
                .getLifecycle()
                .toBuilder()
                .setArchived(true)
                .build();
        builder.setId(id())
               .setLifecycle(flags)
               .setVersion(event.getVersion());
        setArchived(true);
    }

    @Subscribe
    void on(@SuppressWarnings("unused") EntityDeleted event) {
        MirrorVBuilder builder = builder();
        LifecycleFlags flags = builder
                .getLifecycle()
                .toBuilder()
                .setDeleted(true)
                .build();
        builder.setId(id())
               .setLifecycle(flags)
               .setVersion(event.getVersion());
        setDeleted(true);
    }

    @Subscribe
    void on(@SuppressWarnings("unused") EntityExtractedFromArchive event) {
        MirrorVBuilder builder = builder();
        LifecycleFlags flags = builder
                .getLifecycle()
                .toBuilder()
                .setArchived(false)
                .build();
        builder.setId(id())
               .setLifecycle(flags)
               .setVersion(event.getVersion());
        setArchived(false);
    }

    @Subscribe
    void on(@SuppressWarnings("unused") EntityRestored event) {
        MirrorVBuilder builder = builder();
        LifecycleFlags flags = builder
                .getLifecycle()
                .toBuilder()
                .setDeleted(false)
                .build();
        builder.setId(id())
               .setLifecycle(flags)
               .setVersion(event.getVersion());
        setDeleted(false);
    }

    /**
     * Builds the {@linkplain TargetFilters entity filters} for the {@link Mirror} projection based
     * on the domain aggregate {@link Target}.
     *
     * @param target
     *         domain aggregate query target
     * @return entity filters for this projection
     */
    static TargetFilters buildFilters(Target target) {
        IdFilter idFilter = buildIdFilter(target);
        TargetFilters filters = target.getFilters();
        CompositeFilter typeFilter = all(eq(TYPE_COLUMN_QUERY_NAME, target.getType()));
        TargetFilters appendedFilters = filters
                .toBuilder()
                .setIdFilter(idFilter)
                .addFilter(typeFilter)
                .build();
        return appendedFilters;
    }

    private static IdFilter buildIdFilter(Target target) {
        if (target.getIncludeAll()) {
            return IdFilter.getDefaultInstance();
        }
        List<Any> domainIds = target.getFilters()
                                    .getIdFilter()
                                    .getIdsList();
        if (domainIds.isEmpty()) {
            return IdFilter.getDefaultInstance();
        }
        IdFilter result = assembleSystemIdFilter(domainIds);
        return result;
    }

    private static IdFilter assembleSystemIdFilter(Collection<Any> domainIds) {
        List<Any> mirrorIds = domainIds
                .stream()
                .map(MirrorProjection::domainToSystemId)
                .collect(toList());
        IdFilter idFilter = IdFilter
                .newBuilder()
                .addAllIds(mirrorIds)
                .build();
        return idFilter;
    }

    private static Any domainToSystemId(Any domainId) {
        MirrorId mirrorId = MirrorId
                .newBuilder()
                .setValue(domainId)
                .build();
        Any systemId = pack(mirrorId);
        return systemId;
    }

    /**
     * Obtains the selected fields of the aggregate state.
     *
     * <p>If the {@link FieldMask} is empty, returns the complete state.
     *
     * @param fields
     *         the fields to obtain
     * @return the state of the mirrored aggregate
     */
    final Any aggregateState(FieldMask fields) {
        Any completeState = aggregateState();
        if (isDefault(fields) || fields.getPathsList().isEmpty()) {
            return completeState;
        }
        Message unpacked = unpack(completeState);
        Message trimmedState = applyMask(fields, unpacked);
        Any result = pack(trimmedState);
        return result;
    }

    /**
     * Obtains an aggregate version.
     *
     * @return the version of the mirrored aggregate
     */
    final Version aggregateVersion() {
        return state().getVersion();
    }

    /**
     * Obtains the type of the mirrored aggregate state.
     *
     * <p>This method defined an {@link io.spine.server.entity.storage.EntityColumn EntityColumn}
     * required for effective querying.
     *
     * <p>The framework never queries for several types of mirrors in a single call.
     * {@link io.spine.annotation.SPI SPI} users may exploit this fact when optimising databases for
     * the system types.
     *
     * @return the state {@link TypeUrl} as a {@code String}
     */
    @Column(name = TYPE_COLUMN_NAME)
    public String getAggregateType() {
        return aggregateState().getTypeUrl();
    }

    private Any aggregateState() {
        return state().getState();
    }
}
