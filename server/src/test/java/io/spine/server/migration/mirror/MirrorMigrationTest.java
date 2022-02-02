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

import com.google.common.collect.Lists;
import io.spine.protobuf.AnyPacker;
import io.spine.server.migration.mirror.given.Courier;
import io.spine.server.migration.mirror.given.CourierAgg;
import io.spine.server.migration.mirror.given.DeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.migration.mirror.given.MirrorMigrationTestEnv.createEntityRecordStorage;
import static io.spine.server.migration.mirror.given.MirrorMigrationTestEnv.createMirrorStorage;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("`MirrorMigration` should")
class MirrorMigrationTest {

    @Nested
    @DisplayName("migrate mirror projections of the specified aggregate")
    class MigrateProjections {

        @Test
        @DisplayName("in a single run")
        void inSingleRun() {
            var mirrors = createMirrorStorage(
                    DeliveryService::generateParcel, 3_000,
                    DeliveryService::generateCourier, 4_000,
                    DeliveryService::generateVehicle, 5_000
            );

            var migration = new MirrorMigration(mirrors);
            var entityToMigrate = CourierAgg.class;
            var entityRecords = createEntityRecordStorage(entityToMigrate);

            migration.run(entityToMigrate, entityRecords);

            var migratedRecords = Lists.newArrayList(entityRecords.readAll());
            assertThat(migratedRecords).hasSize(4_000);
            assertDoesNotThrow(() -> migratedRecords.forEach(
                    record -> AnyPacker.unpack(record.getState(), Courier.class)
            ));
        }

        @Nested
        @DisplayName("step by step with a batch size of")
        class WhenBatchSize {

            @Test
            @DisplayName("one hundred")
            void oneHundred() {
                // assert result
                // assert communicates with batches of 100
            }

            @Test
            @DisplayName("five hundreds")
            void fiveHundreds() {

            }

            @Test
            @DisplayName("five thousands")
            void fiveThousands() {

            }
        }
    }
}
