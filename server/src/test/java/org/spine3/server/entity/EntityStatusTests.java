/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity;

import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests of working with entity status.
 *
 * <p>When migrating to Java 8, this test suite may become a {@code @Nested}
 * class of {@link EntityShould}.
 *
 * @author Alexander Yevsyukov
 */
public class EntityStatusTests {

    private Entity<Long, StringValue> entity;

    /**
     * A minimal entity class.
     */
    private static class EntityMini extends Entity<Long, StringValue> {
        private EntityMini(Long id) {
            super(id);
        }
    }

    @Before
    public void setUp() {
        entity = new EntityMini(ThreadLocalRandom.current().nextLong());
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
        final Entity another = new EntityMini(entity.getId());

        another.setArchived(true);

        assertFalse(entity.equals(another));
    }
}
