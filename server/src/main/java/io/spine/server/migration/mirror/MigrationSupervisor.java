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

import io.spine.annotation.SPI;
import io.spine.system.server.Mirror;

/**
 * Directs and oversees the process of {@link MirrorMigration}.
 *
 * <p>There are three responsibility of a supervisor:
 *
 * <ol>
 *     <li>Specifies the {@linkplain #stepSize step size} which is a number of records
 *         processed at once.</li>
 *     <li>After each step, a supervisor is {@linkplain #shouldContinueAfter(MigrationStep) asked}
 *         whether to continue or terminate.</li>
 *     <li>Receives notifications about the key stages in the course of the migration.</li>
 * </ol>
 *
 * @see MirrorMigration
 * @see MigrationStep
 */
@SPI
public class MigrationSupervisor {

    private final int stepSize;

    /**
     * Creates a supervisor with the step size specified.
     *
     * @param stepSize
     *         the number of records, processed within a single {@link MigrationStep}
     */
    public MigrationSupervisor(int stepSize) {
        this.stepSize = stepSize;
    }

    /**
     * Number of records, processed within a single {@link MigrationStep}.
     */
    public int stepSize() {
        return stepSize;
    }

    /**
     * Determines if the migration should be continued after the given step is completed.
     *
     * <p>This method is not called when the last step migrated zero records. In this case
     * the migration is considered completed anyway.
     *
     * <ol>
     *     <li>In order to <b>continue</b> the interrupted migration, just
     *         {@link MirrorMigration#run(MigrationSupervisor) run} it again. The migration process
     *         {@link Mirror#getWasMigrated() marks} already migrated records. Thus, they will not
     *         be migrated twice and duplicated.</li>
     *     <li>In order to <b>re-run</b> the migration, values of {@link Mirror#getWasMigrated()}
     *          column should be reset to {@code false} or no value.</li>
     * </ol>
     *
     * @param step
     *         the last completed step
     */
    public boolean shouldContinueAfter(MigrationStep step) {
        return true;
    }

    /**
     * Called when the migration is started.
     */
    public void onMigrationStarted() {
        // do nothing.
    }

    /**
     * Called when the migration process is completed.
     *
     * <p>This method is called in two cases:
     *
     * <ol>
     *     <li>The last step migrated {@link MigrationStep#getMigrated() zero} records, meaning
     *         all the records have been processed.</li>
     *     <li>The migration has been {@link #shouldContinueAfter(MigrationStep) terminated}
     *         ahead of time.</li>
     * </ol>
     */
    public void onMigrationCompleted() {
        // do nothing.
    }

    /**
     * Called when the next {@linkplain MigrationStep} is started.
     */
    public void onStepStarted() {
        // do nothing.
    }

    /**
     * Called when the current {@linkplain MigrationStep} is completed.
     */
    public void onStepCompleted(MigrationStep step) {
        // do nothing.
    }
}
