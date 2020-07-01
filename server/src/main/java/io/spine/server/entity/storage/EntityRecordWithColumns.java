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

package io.spine.server.entity.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.WithLifecycle;
import io.spine.server.storage.OldColumn;
import io.spine.server.storage.RecordWithColumns;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyMap;

/**
 * A value of {@link EntityRecord} associated with the values of its {@linkplain OldColumn columns}.
 */
@Internal
public final class EntityRecordWithColumns<I>
        extends RecordWithColumns<I, EntityRecord> implements WithLifecycle {

    private EntityRecordWithColumns(I id, EntityRecord record, Map<OldColumnName, Object> columns) {
        super(id, record, columns);
    }

    /**
     * Creates the new instance of {@code EntityRecordWithColumns} by evaluating the values
     * of the passed columns for the passed entity.
     *
     * @param entity
     *         the entity to use as a provider of the record identifier and the column values
     * @param recordSpec
     *         the specification of the stored record format
     * @param record
     *         the record prepared for storage
     * @param <I>
     *         the type of the entity identifiers
     * @param <E>
     *         the type of the entity
     * @return a new instance of {@code EntityRecordWithColumns}
     */
    public static <I, E extends Entity<I, ?>> EntityRecordWithColumns<I>
    create(E entity, EntityRecordSpec<I> recordSpec, EntityRecord record) {
        Map<OldColumnName, @Nullable Object> storageFields = recordSpec.valuesIn(entity);
        return new EntityRecordWithColumns<>(entity.id(), record, storageFields);
    }

    /**
     * Creates the new instance of {@code EntityRecordWithColumns} using the pre-created
     * entity record and the entity identifier.
     *
     * <p>This method considers only the values of the
     * {@linkplain LifecycleColumn lifecycle columns}.
     *
     * @param id
     *         the identifier of the entity
     * @param record
     *         the record to store; it is also used as a source for the lifecycle column values
     * @param <I>
     *         the type of the identiiers
     * @return a new instance of {@code EntityRecordWithColumns}
     */
    public static <I> EntityRecordWithColumns<I> create(I id, EntityRecord record) {
        checkNotNull(id);
        checkNotNull(record);
        ImmutableMap<OldColumnName, Object> lifecycleValues = EntityRecordSpec.lifecycleValuesIn(record);
        return new EntityRecordWithColumns<>(id, record, lifecycleValues);
    }

    /**
     * Wraps a passed entity record into a {@code EntityWithColumns} with no storage fields.
     *
     * <p>This is a shortcut for {@link #of(EntityRecord, Map) of(EntityRecord, Map)}.
     *
     * @see #of(EntityRecord, Map) for the notes on usage
     */
    @VisibleForTesting
    public static <I> EntityRecordWithColumns<I> of(EntityRecord record) {
        return of(record, emptyMap());
    }

    /**
     * Creates a new instance from the passed record and storage fields.
     *
     * @apiNote This test-only method unpacks the identifier of the passed record and casts
     *         it to the type {@code I}. It is a responsibility of the caller to provide the record
     *         with the matching identifier
     */
    @VisibleForTesting
    public static <I> EntityRecordWithColumns<I>
    of(EntityRecord record, Map<OldColumnName, Object> storageFields) {
        I id = extractId(record);
        return new EntityRecordWithColumns<>(id, record, storageFields);
    }

    /**
     * Extracts the identifier from the passed record and casts it to the type {@code I}.
     *
     * <p>It is a responsibility of the caller to provide a record with the matching identifier.
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})    // see the docs.
    private static <I> I extractId(EntityRecord record) {
        return (I) Identifier.unpack(record.getEntityId());
    }

    @Override
    public LifecycleFlags getLifecycleFlags() {
        return record().getLifecycleFlags();
    }

    @Override
    public boolean isArchived() {
        return record().isArchived();
    }

    @Override
    public boolean isDeleted() {
        return record().isDeleted();
    }

    @Override
    public boolean isActive() {
        return record().isActive();
    }
}
