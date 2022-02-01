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
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.storage.RecordWithColumns;
import io.spine.system.server.Mirror;
import io.spine.system.server.MirrorId;

import static com.google.common.truth.Truth.assertThat;

public class MirrorMappingTestEnv {

    private MirrorMappingTestEnv() {
    }

    /**
     * Asserts that common fields in {@linkplain EntityRecord} and {@linkplain Mirror} match.
     */
    public static void assertRecord(EntityRecord record, Mirror mirror) {
        assertThat(record.getEntityId()).isEqualTo(mirror.getId().getValue());
        assertThat(record.getState()).isEqualTo(mirror.getState());
        assertThat(record.getVersion()).isEqualTo(mirror.getVersion());
        assertThat(record.lifecycleFlags()).isEqualTo(mirror.getLifecycle());
    }

    /**
     * Asserts that version and lifecycle columns match to the ones in mirror.
     *
     * <p>Those columns are defined for all entity records.
     */
    public static void assertLifecycleColumns(RecordWithColumns<?, EntityRecord> recordWithColumns,
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

    /**
     * Asserts that aggregate's columns defined for {@linkplain Parcel} aggregate are present
     * in the record.
     *
     * <p>Those columns are specific to a concrete aggregate.
     */
    public static void assertEntityColumns(RecordWithColumns<ParcelId, EntityRecord> recordWithColumns,
                                           Mirror mirror) {

        // This method is hardcoded for `Parcel` aggregate.
        // Try to generify by accepting the aggregate's class.

        var parcel = AnyPacker.unpack(mirror.getState(), Parcel.class);
        assertThat(recordWithColumns.columnValue(ColumnName.of("recipient")))
                .isEqualTo(parcel.getRecipient());
        assertThat(recordWithColumns.columnValue(ColumnName.of("delivered")))
                .isEqualTo(parcel.getDelivered());
    }

    /**
     * Creates {@linkplain Mirror} projection from the passed state.
     */
    public static Mirror mirror(EntityState<?> state) {
        var lifecycle = lifecycle(false, false);
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

    private static LifecycleFlags lifecycle(boolean archived, boolean deleted) {
        return LifecycleFlags.newBuilder()
                .setArchived(archived)
                .setDeleted(deleted)
                .vBuild();
    }

    private static Version version(int number) {
        return Version.newBuilder()
                .setNumber(number)
                .setTimestamp(Time.currentTime())
                .vBuild();
    }
}
