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
import io.spine.server.ContextSpec;
import io.spine.server.migration.mirror.given.DeliveryService;
import io.spine.server.migration.mirror.given.MemoizingMonitor;
import io.spine.server.migration.mirror.given.MirrorToEntityRecordTestEnv;
import io.spine.server.migration.mirror.given.ParcelAgg;
import io.spine.server.migration.mirror.given.PreparedMirrorStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.migration.mirror.given.MirrorMigrationTestEnv.assertMigratedMirrors;
import static io.spine.server.migration.mirror.given.MirrorMigrationTestEnv.assertUsedBatchSize;
import static io.spine.server.migration.mirror.given.MirrorMigrationTestEnv.assertWithinBc;

@DisplayName("`MirrorMigration` should")
final class MirrorMigrationTest {

    private static final StorageFactory storageFactory = InMemoryStorageFactory.newInstance();
    private static final String tenantId = MirrorToEntityRecordTestEnv.class.getSimpleName();
    private static final ContextSpec contextSpec = ContextSpec.singleTenant(tenantId);

    @Nested
    @DisplayName("migrate mirror records step-by-step with a step size of")
    class MigrateMirrorsInBatchesOf {

        @Test
        @DisplayName("one hundred records")
        void oneHundredRecords() {
            testInBatchesOf(100);
        }

        @Test
        @DisplayName("one thousand records")
        void fiveThousandsRecords() {
            testInBatchesOf(1_000);
        }

        private void testInBatchesOf(int batchSize) {
            var delivered = 2_175;
            var inProgress = 3_780;

            var migration = new MirrorMigration<>(contextSpec, storageFactory, ParcelAgg.class);
            var mirrorStorage = new PreparedMirrorStorage(migration.mirrorStorage())
                    .put(DeliveryService::generateCourier, 3_000)
                    .put(DeliveryService::generateVehicle, 4_000)
                    .put(DeliveryService::generateDeliveredParcel, delivered)
                    .put(DeliveryService::generateInProgressParcel, inProgress)
                    .get();
            var supervisor = new MemoizingMonitor(batchSize);

            migration.run(supervisor);

            assertWithinBc(migration.entityRecordStorage(), delivered, inProgress);
            assertMigratedMirrors(mirrorStorage, delivered + inProgress);
            assertUsedBatchSize(supervisor, batchSize);
        }
    }

    @Nested
    @DisplayName("notify a supervisor")
    class NotifySupervisor {

        @Test
        @DisplayName("on a migration start and completion")
        void onMigrationRunning() {
            var supervisor = new MemoizingMonitor(1_000);
            runMigration(supervisor);

            assertThat(supervisor.startedTimes()).isEqualTo(1);
            assertThat(supervisor.completedTimes()).isEqualTo(1);
        }

        @Test
        @DisplayName("on a migration's step start and completion")
        void onStepRunning() {
            var supervisor = new MemoizingMonitor(1_000);
            runMigration(supervisor);

            assertThat(supervisor.stepStartedTimes()).isEqualTo(5);
            assertThat(supervisor.completedSteps()).hasSize(5);
        }

        private void runMigration(MemoizingMonitor supervisor) {
            var migration = new MirrorMigration<>(contextSpec, storageFactory, ParcelAgg.class);
            new PreparedMirrorStorage(migration.mirrorStorage())
                    .put(DeliveryService::generateInProgressParcel, 3_050);
            migration.run(supervisor);
        }
    }

    @Test
    @DisplayName("terminate on a supervisor's refusal")
    void terminateMigration() {
        var expectedNumber = 5_000;
        var migration = new MirrorMigration<>(contextSpec, storageFactory, ParcelAgg.class);
        new PreparedMirrorStorage(migration.mirrorStorage())
                .put(DeliveryService::generateInProgressParcel, expectedNumber);

        // Too small batch size would slow down the migration.
        // We are going to terminate the migration when it takes more than three seconds.
        var batchSize = 10;
        var supervisor = new MirrorMigrationMonitor(batchSize) {

            private LocalDateTime whenStarted;

            @Override
            public void onMigrationStarted() {
                whenStarted = LocalDateTime.now();
            }

            @Override
            public boolean shouldContinueAfter(MirrorsMigrated step) {
                var secondsSinceStart = ChronoUnit.SECONDS
                        .between(whenStarted, LocalDateTime.now());
                return secondsSinceStart <= 3;
            }
        };

        migration.run(supervisor);

        var entityRecordStorage = migration.entityRecordStorage();
        var entityRecords = Lists.newArrayList(entityRecordStorage.readAll());
        assertThat(entityRecords.size()).isLessThan(expectedNumber);
        assertThat(entityRecords.size()).isAtLeast(batchSize);
    }
}
