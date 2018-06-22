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

import com.google.protobuf.StringValue;
import io.spine.server.entity.rejection.CannotModifyArchivedEntity;
import io.spine.server.entity.rejection.CannotModifyDeletedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of working with entity visibility.
 *
 * <p>When migrating to JUnit 5, this class may become a
 * {@code @Nested} class of {@link EntityTest}.
 *
 * @author Alexander Yevsyukov
 */
// todo move to entity test
@DisplayName("Entity visibility should")
class VisibilityTests {

    private AbstractVersionableEntity<Long, StringValue> entity;

    /**
     * A minimal entity class.
     */
    private static class MiniEntity extends AbstractVersionableEntity<Long, StringValue> {
        private MiniEntity(Long id) {
            super(id);
        }
    }

    @BeforeEach
    void setUp() {
        entity = new MiniEntity(ThreadLocalRandom.current()
                                                 .nextLong());
    }

    @Test
    @DisplayName("return default status after constructor")
    void returnDefaultStatusAfterConstructor() {
        assertEquals(LifecycleFlags.getDefaultInstance(), new MiniEntity(1L).getLifecycleFlags());
    }

    @Test
    @DisplayName("be not archived when created")
    void beNotArchivedWhenCreated() {
        assertFalse(entity.isArchived());
    }

    @Test
    @DisplayName("support archiving")
    void supportArchiving() {
        entity.setArchived(true);

        assertTrue(entity.isArchived());
    }

    @Test
    @DisplayName("support un archiving")
    void supportUnArchiving() {
        entity.setArchived(true);
        entity.setArchived(false);

        assertFalse(entity.isArchived());
    }

    @Test
    @DisplayName("be not deleted when created")
    void beNotDeletedWhenCreated() {
        assertFalse(entity.isDeleted());
    }

    @Test
    @DisplayName("support deletion")
    void supportDeletion() {
        entity.setDeleted(true);

        assertTrue(entity.isDeleted());
    }

    @Test
    @DisplayName("support restoration")
    void supportRestoration() {
        entity.setDeleted(true);
        entity.setDeleted(false);
        assertFalse(entity.isDeleted());
    }

    @Test
    @DisplayName("assure entities with different status are not equal")
    void assureEntitiesWithDifferentStatusAreNotEqual() {
        // Create an entity with the same ID and the same (default) state.
        final AbstractVersionableEntity another = new MiniEntity(entity.getId());

        another.setArchived(true);

        assertFalse(entity.equals(another));
    }

    @Test
    @DisplayName("assign status")
    void assignStatus() {
        final LifecycleFlags status = LifecycleFlags.newBuilder()
                                                    .setArchived(true)
                                                    .setDeleted(false)
                                                    .build();
        entity.setLifecycleFlags(status);
        assertEquals(status, entity.getLifecycleFlags());
    }

    @Test
    @DisplayName("check not archived")
    void checkNotArchived() throws Throwable {
        entity.setArchived(true);

        // This should pass.
        entity.checkNotDeleted();

        assertThrows(CannotModifyArchivedEntity.class, () -> entity.checkNotArchived());
    }

    @Test
    @DisplayName("check not deleted")
    void checkNotDeleted() throws Throwable {
        entity.setDeleted(true);

        // This should pass.
        entity.checkNotArchived();

        assertThrows(CannotModifyDeletedEntity.class, () -> entity.checkNotDeleted());
    }
}
