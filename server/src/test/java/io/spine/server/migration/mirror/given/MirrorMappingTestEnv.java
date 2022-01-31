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

package io.spine.server.migration.mirror.given;

import com.google.protobuf.Any;
import io.spine.base.EntityState;
import io.spine.base.Time;
import io.spine.core.Version;
import io.spine.protobuf.AnyPacker;
import io.spine.protobuf.TypeConverter;
import io.spine.query.ColumnName;
import io.spine.server.ContextSpec;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.RecordWithColumns;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.system.server.Mirror;
import io.spine.system.server.MirrorId;

import static com.google.common.truth.Truth.assertThat;

public class MirrorMappingTestEnv {

    private static final StorageFactory factory = InMemoryStorageFactory.newInstance();
    private static final String tenantId = MirrorMappingTestEnv.class.getSimpleName();
    private static final ContextSpec contextSpec = ContextSpec.singleTenant(tenantId);

    private MirrorMappingTestEnv() {
    }

    public static void assertMatch(RecordWithColumns<ParcelId, EntityRecord> recordWithColumns,
                                   Mirror mirror) {

        assertRecord(recordWithColumns.record(), mirror);
        assertLifecycleColumns(recordWithColumns, mirror);

        var parcel = AnyPacker.unpack(mirror.getState(), Parcel.class);
        assertThat(recordWithColumns.columnValue(ColumnName.of("recipient")))
                .isEqualTo(parcel.getRecipient());
        assertThat(recordWithColumns.columnValue(ColumnName.of("delivered")))
                .isEqualTo(parcel.getDelivered());
    }

    private static void assertLifecycleColumns(RecordWithColumns<?, EntityRecord> recordWithColumns,
                                               Mirror mirror) {

        // total columns = (1x version) + (2x lifecycle columns) + (2x entity's columns)
        var columns = recordWithColumns.columnNames();
        assertThat(columns.size()).isEqualTo(1 + 2 + 2);

        var lifecycleFlags = mirror.getLifecycle();
        assertThat(recordWithColumns.columnValue(ColumnName.of("archived")))
                .isEqualTo(lifecycleFlags.getArchived());
        assertThat(recordWithColumns.columnValue(ColumnName.of("deleted")))
                .isEqualTo(lifecycleFlags.getDeleted());

        var version = mirror.getVersion();
        assertThat(recordWithColumns.columnValue(ColumnName.of("version")))
                .isEqualTo(version);
    }

    private static void assertRecord(EntityRecord record, Mirror mirror) {
        assertThat(record.getEntityId()).isEqualTo(mirror.getId().getValue());
        assertThat(record.getState()).isEqualTo(mirror.getState());
        assertThat(record.getVersion()).isEqualTo(mirror.getVersion());
        assertThat(record.lifecycleFlags()).isEqualTo(mirror.getLifecycle());
    }

    public static <I> EntityRecord
    entityRecord(EntityState<I> state, Version version, LifecycleFlags flags) {
        return EntityRecord.newBuilder()
                .setEntityId(id(state))
                .setState(Any.pack(state))
                .setVersion(version)
                .setLifecycleFlags(flags)
                .vBuild();
    }

    public static <I, S extends EntityState<I>> EntityRecordStorage<I, S>
    createEntityRecordStorage(Class<? extends Entity<I, S>> entityClass) {
        var storage = factory
                .createEntityRecordStorage(contextSpec, entityClass);
        return storage;
    }

    @SuppressWarnings("ConstantConditions") // `Mirror.getId()` would not return `null`.
    public static Storage<MirrorId, Mirror> createMirrorStorage() {
        var recordSpec = new MessageRecordSpec<>(
                MirrorId.class,
                Mirror.class,
                Mirror::getId
        );
        var storage = factory.createRecordStorage(contextSpec, recordSpec);
        return storage;
    }

    public static <I> void write(EntityState<I> state, Storage<MirrorId, Mirror> storage) {
//        var mirror = mirror(state);
//        storage.write(mirror.getId(), mirror);
    }

    public static Mirror mirror(EntityState<?> state) {
        var lifecycle = lifecycle(true, false);
        var version = version(12);
        var mirrorId = mirrorId(state);
        return Mirror.newBuilder()
                .setId(mirrorId)
                .setState(Any.pack(state, state.typeUrl().prefix()))
                .setLifecycle(lifecycle)
                .setVersion(version)
                .setAggregateType(state.typeUrl().value())
                .vBuild();
    }

    private static MirrorId mirrorId(EntityState<?> state) {
        return MirrorId.newBuilder()
                .setValue(id(state))
                .setTypeUrl(state.typeUrl().value())
                .vBuild();
    }

    private static Any id(EntityState<?> state) {
        var idField = state.getDescriptorForType().findFieldByNumber(1);
        var stateId = state.getField(idField);
        return TypeConverter.toAny(stateId);
    }

    public static LifecycleFlags lifecycle(boolean archived, boolean deleted) {
        return LifecycleFlags.newBuilder()
                .setArchived(archived)
                .setDeleted(deleted)
                .vBuild();
    }

    public static Version version(int number) {
        return Version.newBuilder()
                .setNumber(number)
                .setTimestamp(Time.currentTime())
                .vBuild();
    }
}
