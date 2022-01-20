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

package io.spine.server.migration.mirror;

import io.spine.query.ColumnName;
import io.spine.server.entity.EntityRecord;
import io.spine.server.migration.mirror.given.ParcelId;
import io.spine.server.migration.mirror.given.ParcelAgg;
import io.spine.server.storage.RecordWithColumns;
import io.spine.system.server.Mirror;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.migration.mirror.given.DeliveryService.generateCourier;
import static io.spine.server.migration.mirror.given.DeliveryService.generateParcel;
import static io.spine.server.migration.mirror.given.MirrorMigrationTestEnv.lifecycle;
import static io.spine.server.migration.mirror.given.MirrorMigrationTestEnv.mirror;
import static io.spine.server.migration.mirror.given.MirrorMigrationTestEnv.version;

@DisplayName("MirrorMigration should")
class MirrorMigrationTest {

    @Test
    @DisplayName("transform a `Mirror` to an `EntityRecordWithColumns`")
    void migrateSingleMirror() {
        var courier = generateCourier();
        var lifecycle = lifecycle(true, false);
        var version = version(12);
        var mirror = mirror(courier, lifecycle, version);

        var migration = new MirrorTransformation();
        var record = migration.transform(mirror);
        assertEqual(record, mirror);
    }

    private static void assertEqual(EntityRecord record, Mirror mirror) {
        assertThat(record.getEntityId()).isEqualTo(mirror.getId().getValue());
        assertThat(record.getState()).isEqualTo(mirror.getState());
        assertThat(record.getVersion()).isEqualTo(mirror.getVersion());
        assertThat(record.lifecycleFlags()).isEqualTo(mirror.getLifecycle());
    }

    @Test
    void testColumns() {
        var parcel = generateParcel();
        var lifecycle = lifecycle(true, false);
        var version = version(12);
        var mirror = mirror(parcel, lifecycle, version);

        var transformation = new MirrorTransformation();
        var recordWithColumns = transformation.transform(ParcelAgg.class, mirror);

        assertMatch(recordWithColumns);
    }

    private static void assertMatch(RecordWithColumns<ParcelId, EntityRecord> recordWithColumns) {
        assertThat(recordWithColumns.columnNames())
                .containsExactly(
                        ColumnName.of("recipient"),
                        ColumnName.of("delivered")
                );
    }
}
