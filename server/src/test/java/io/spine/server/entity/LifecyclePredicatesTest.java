/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity;

import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.entity.LifecyclePredicates.isEntityActive;
import static io.spine.server.entity.LifecyclePredicates.isRecordActive;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LifecyclePredicates utility class should")
class LifecyclePredicatesTest extends UtilityClassTest<LifecyclePredicates> {

    LifecyclePredicatesTest() {
        super(LifecyclePredicates.class);
    }

    @Test
    @DisplayName("consider archived entity inactive")
    void stateArchivedEntityInactive() {
        LifecycleFlags status = LifecycleFlags
                .newBuilder()
                .setArchived(true)
                .build();
        assertFalse(isEntityActive().test(status));
    }

    @Test
    @DisplayName("consider deleted entity inactive")
    void stateDeletedEntityInactive() {
        LifecycleFlags status = LifecycleFlags
                .newBuilder()
                .setDeleted(true)
                .build();
        assertFalse(isEntityActive().test(status));
    }

    @Test
    @DisplayName("consider archived record inactive")
    void stateArchivedRecordInactive() {
        EntityRecord record = EntityRecord
                .newBuilder()
                .setLifecycleFlags(LifecycleFlags.newBuilder()
                                                 .setArchived(true))
                .build();
        assertFalse(isRecordActive().test(record));
    }

    @Test
    @DisplayName("consider deleted record inactive")
    void stateDeletedRecordInactive() {
        EntityRecord record = EntityRecord
                .newBuilder()
                .setLifecycleFlags(LifecycleFlags.newBuilder()
                                                 .setDeleted(true))
                .build();
        assertFalse(isRecordActive().test(record));
    }

    /**
     * We are not likely to encounter {@code null} records,
     * but making such records active would help identify possible bugs.
     */
    @Test
    @DisplayName("consider null records active")
    void stateNullRecordsActive() {
        assertTrue(isEntityActive().test(null));
    }
}
