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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.entity.EntityWithLifecycle.Predicates.isEntityVisible;
import static io.spine.server.entity.EntityWithLifecycle.Predicates.isRecordVisible;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("EntityWithLifecycle Predicates should")
class PredicatesTest {

    @Test
    @DisplayName("consider archived entity invisible")
    void stateArchivedEntityInvisible() {
        LifecycleFlags status = LifecycleFlags.newBuilder()
                                              .setArchived(true)
                                              .build();
        assertFalse(isEntityVisible().test(status));
    }

    @Test
    @DisplayName("consider deleted entity invisible")
    void stateDeletedEntityInvisible() {
        LifecycleFlags status = LifecycleFlags.newBuilder()
                                              .setDeleted(true)
                                              .build();
        assertFalse(isEntityVisible().test(status));
    }

    @Test
    @DisplayName("consider archived record invisible")
    void stateArchivedRecordInvisible() {
        EntityRecord record = EntityRecord.newBuilder()
                                          .setLifecycleFlags(LifecycleFlags.newBuilder()
                                                                           .setArchived(true))
                                          .build();
        assertFalse(isRecordVisible().test(record));
    }

    @Test
    @DisplayName("consider deleted record invisible")
    void stateDeletedRecordInvisible() {
        EntityRecord record = EntityRecord.newBuilder()
                                          .setLifecycleFlags(LifecycleFlags.newBuilder()
                                                                           .setDeleted(true))
                                          .build();
        assertFalse(isRecordVisible().test(record));
    }

    /**
     * We are not likely to encounter {@code null} records,
     * but making such records "visible" would help identify possible bugs.
     */
    @Test
    @DisplayName("consider null records visible")
    void stateNullRecordsVisible() {
        assertTrue(isEntityVisible().test(null));
    }
}
