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
import io.spine.server.ContextSpec;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.migration.mirror.given.Courier;
import io.spine.server.migration.mirror.given.CourierAgg;
import io.spine.server.migration.mirror.given.DeliveryService;
import io.spine.server.migration.mirror.given.MemoizingSupervisor;
import io.spine.server.migration.mirror.given.MirrorMappingTestEnv;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.system.server.Mirror;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.migration.mirror.given.MirrorMigrationTestEnv.fill;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("`MirrorMigration` should")
class MirrorMigrationTest {

    private static final StorageFactory storageFactory = InMemoryStorageFactory.newInstance();
    private static final String tenantId = MirrorMappingTestEnv.class.getSimpleName();
    private static final ContextSpec contextSpec = ContextSpec.singleTenant(tenantId);

    @Nested
    @DisplayName("migrate mirror records step by step with a batches of")
    class MigrateMirrorsInBatchesOf {

        @Test
        @DisplayName("one hundred")
        void oneHundred() {
            testInBatchesOf(100);
        }

        @Test
        @DisplayName("one thousand")
        void fiveThousands() {
            testInBatchesOf(1_000);
        }

        private void testInBatchesOf(int batchSize) {
            var migration = new MirrorMigration<>(contextSpec, storageFactory, CourierAgg.class);
            var mirrorStorage = migration.mirrorStorage();
            var numberOfMirrors = 4_000;

            fill(mirrorStorage, DeliveryService::generateParcel, 3_000);
            fill(mirrorStorage, DeliveryService::generateCourier, numberOfMirrors);
            fill(mirrorStorage, DeliveryService::generateVehicle, 5_000);

            var supervisor = new MemoizingSupervisor(batchSize);
            migration.run(supervisor);

            assertEntityRecords(migration.entityRecordStorage(), numberOfMirrors);
            assertUsedBatchSize(supervisor, batchSize);
            assertMigratedMirrors(mirrorStorage, numberOfMirrors);
        }

        private void assertEntityRecords(EntityRecordStorage<?, ?> entityRecordStorage,
                                         int expected) {

            var entityRecords = Lists.newArrayList(entityRecordStorage.readAll());
            assertThat(entityRecords).hasSize(expected);
            assertDoesNotThrow(() -> entityRecords.forEach(
                    entityRecord -> AnyPacker.unpack(entityRecord.getState(), Courier.class))
            );
        }

        private void assertUsedBatchSize(MemoizingSupervisor supervisor, int batchSize) {
            supervisor.completedSteps().forEach(
                    step -> assertThat(step.getMigrated()).isAtMost(batchSize)
            );
        }

        private void assertMigratedMirrors(MirrorStorage mirrorStorage, int expected) {
            var migratedNumber = mirrorStorage.queryBuilder()
                                              .where(Mirror.Column.wasMigrated())
                                              .is(true)
                                              .build();
            var migratedMirrors = Lists.newArrayList(mirrorStorage.readAll(migratedNumber));
            assertThat(migratedMirrors).hasSize(expected);
        }
    }

    @Nested
    @DisplayName("notify a supervisor")
    class NotifySupervisor {

        @Test
        @DisplayName("on a migration staring")
        void onMigrationStarting() {

        }

        @Test
        @DisplayName("on a migration completion")
        void onMigrationCompletion() {

        }

        @Test
        @DisplayName("on a migration step staring")
        void onMigrationStepStarting() {

        }

        @Test
        @DisplayName("on a migration step completion")
        void onMigrationStepCompletion() {

        }
    }

    @Test
    @DisplayName("terminate on a supervisor's refusal")
    void terminateMigration() {

    }
}
