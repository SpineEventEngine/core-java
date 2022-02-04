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

import com.google.common.collect.Lists;
import io.spine.environment.Tests;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.ServerEnvironment;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.migration.mirror.MirrorStorage;
import io.spine.system.server.Mirror;
import io.spine.testing.server.blackbox.BlackBox;

import static com.google.common.truth.Truth.assertThat;

public final class MirrorMigrationTestEnv {

    private MirrorMigrationTestEnv() {
    }

    public static void assertWithinBc(EntityRecordStorage<ParcelId, Parcel> entityRecordStorage,
                                      int expectedDelivered,
                                      int expectedInProgress) {

        ServerEnvironment.instance().reset();
        ServerEnvironment.when(Tests.class)
                         .use(PreparedStorageFactory.with(entityRecordStorage));

        var context = BlackBox.from(
                BoundedContextBuilder.assumingTests()
                                     .add(ParcelAgg.class)
        );
        var client = context.clients()
                            .withMatchingTenant()
                            .asGuest();

        var delivered = client.run(
                Parcel.query()
                      .delivered().is(true)
                      .build()
        );
        var parcels = client.run(
                Parcel.query()
                      .delivered().is(false)
                      .build()
        );

        assertThat(delivered).hasSize(expectedDelivered);
        assertThat(parcels).hasSize(expectedInProgress);

        ServerEnvironment.instance().reset();
        context.close();
    }

    public static void assertMigratedMirrors(MirrorStorage mirrorStorage, int expected) {
        var migratedNumber = mirrorStorage.queryBuilder()
                                          .where(Mirror.Column.wasMigrated()).is(true)
                                          .build();
        var iterator = mirrorStorage.readAll(migratedNumber);
        var migratedMirrors = Lists.newArrayList(iterator);
        assertThat(migratedMirrors).hasSize(expected);
    }

    public static void assertUsedBatchSize(MemoizingSupervisor supervisor, int batchSize) {
        supervisor.completedSteps()
                  .forEach(step -> assertThat(step.getMigrated()).isAtMost(batchSize));
    }
}
