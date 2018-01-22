/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests of working with entity visibility.
 *
 * <p>When migrating to JUnit 5, this class may become a
 * {@code @Nested} class of {@link EntityShould}.
 *
 * @author Alexander Yevsyukov
 */
public class VisibilityTests {

    private AbstractVersionableEntity<Long, StringValue> entity;

    /**
     * A minimal entity class.
     */
    private static class MiniEntity extends AbstractVersionableEntity<Long, StringValue> {
        private MiniEntity(Long id) {
            super(id);
        }
    }

    @Before
    public void setUp() {
        entity = new MiniEntity(ThreadLocalRandom.current()
                                                 .nextLong());
    }

    @Test
    public void return_default_status_after_constructor() {
        assertEquals(LifecycleFlags.getDefaultInstance(), new MiniEntity(1L).getLifecycleFlags());
    }

    @Test
    public void be_not_archived_when_created() {
        assertFalse(entity.isArchived());
    }

    @Test
    public void support_archiving() {
        entity.setArchived(true);

        assertTrue(entity.isArchived());
    }

    @Test
    public void support_un_archiving() {
        entity.setArchived(true);
        entity.setArchived(false);

        assertFalse(entity.isArchived());
    }

    @Test
    public void be_not_deleted_when_created() {
        assertFalse(entity.isDeleted());
    }

    @Test
    public void support_deletion() {
        entity.setDeleted(true);

        assertTrue(entity.isDeleted());
    }

    @Test
    public void support_restoration() {
        entity.setDeleted(true);
        entity.setDeleted(false);
        assertFalse(entity.isDeleted());
    }

    @Test
    public void assure_entities_with_different_status_are_not_equal() {
        // Create an entity with the same ID and the same (default) state.
        final AbstractVersionableEntity another = new MiniEntity(entity.getId());

        another.setArchived(true);

        assertFalse(entity.equals(another));
    }

    @Test
    public void assign_status() {
        final LifecycleFlags status = LifecycleFlags.newBuilder()
                                                    .setArchived(true)
                                                    .setDeleted(false)
                                                    .build();
        entity.setLifecycleFlags(status);
        assertEquals(status, entity.getLifecycleFlags());
    }

    @Test(expected = CannotModifyArchivedEntity.class)
    public void check_not_archived() throws Throwable {
        entity.setArchived(true);

        // This should pass.
        entity.checkNotDeleted();

        // This should throw.
        entity.checkNotArchived();
    }

    @Test(expected = CannotModifyDeletedEntity.class)
    public void check_not_deleted() throws Throwable {
        entity.setDeleted(true);

        // This should pass.
        entity.checkNotArchived();

        // This should throw.
        entity.checkNotDeleted();
    }
}
