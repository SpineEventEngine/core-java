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

package org.spine3.server.aggregate;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Identifiers;
import org.spine3.server.entity.status.EntityStatus;
import org.spine3.test.entity.Project;
import org.spine3.test.entity.ProjectId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests covering the behavior of the {@link AggregateStorage} regarding the {@link EntityStatus}.
 *
 * @author Dmytro Dashenkov.
 */
public abstract class EntityStatusHolderShould {

    protected abstract AggregateStorage<ProjectId> getAggregateStorage(
            Class<? extends Aggregate<ProjectId, ?, ?>> aggregateClass);

    private AggregateStorage<ProjectId> storage;

    private final ProjectId id = ProjectId.newBuilder()
                                          .setId(Identifiers.newUuid())
                                          .build();

    @Before
    public void setUp() {
        storage = getAggregateStorage(TestAggregate.class);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // Checked in an assertion
    @Test
    public void write_entity_status_of_aggregate() {
        final EntityStatus status = EntityStatus.newBuilder()
                                                .setArchived(true)
                                                .build();
        storage.writeStatus(id, status);
        final Optional<EntityStatus> readStatus = storage.readStatus(id);
        assertTrue(readStatus.isPresent());
        assertEquals(status, readStatus.get());
    }

    @Test
    public void save_whole_status() {
        final boolean archived = true;
        final boolean deleted = true;
        final EntityStatus expected = EntityStatus.newBuilder()
                                                  .setArchived(archived)
                                                  .setDeleted(deleted)
                                                  .build();
        storage.writeStatus(id, expected);
        final Optional<EntityStatus> optionalActual = storage.readStatus(id);
        assertStatus(optionalActual, true, true);
    }

    @Test
    public void mark_aggregate_archived() {
        final boolean success = storage.markArchived(id);
        assertTrue(success);
        final Optional<EntityStatus> aggregateStatus = storage.readStatus(id);
        assertArchived(aggregateStatus);
    }

    @Test
    public void mark_aggregate_deleted() {
        final boolean success = storage.markDeleted(id);
        assertTrue(success);
        final Optional<EntityStatus> aggregateStatus = storage.readStatus(id);
        assertDeleted(aggregateStatus);
    }

    @Test
    public void do_not_mark_aggregate_archived_twice() {
        final boolean firstSuccessful = storage.markArchived(id);
        assertTrue(firstSuccessful);
        final Optional<EntityStatus> firstRead = storage.readStatus(id);
        assertArchived(firstRead);

        final boolean secondSuccessful = storage.markArchived(id);
        assertFalse(secondSuccessful);

        final Optional<EntityStatus> secondRead = storage.readStatus(id);
        assertArchived(secondRead);
    }

    @Test
    public void do_not_mark_aggregate_deleted_twice() {
        final boolean firstSuccessful = storage.markDeleted(id);
        assertTrue(firstSuccessful);
        final Optional<EntityStatus> firstRead = storage.readStatus(id);
        assertDeleted(firstRead);

        final boolean secondSuccessful = storage.markDeleted(id);
        assertFalse(secondSuccessful);

        final Optional<EntityStatus> secondRead = storage.readStatus(id);
        assertDeleted(secondRead);
    }

    @Test
    public void mark_archived_if_deleted() {
        final boolean deletingSuccess = storage.markDeleted(id);
        assertTrue(deletingSuccess);
        assertDeleted(storage.readStatus(id));
        final boolean archivingSuccess = storage.markArchived(id);
        assertTrue(archivingSuccess);
        assertStatus(storage.readStatus(id), true, true);
    }

    @Test
    public void mark_deleted_if_archived() {
        final boolean archivingSuccess = storage.markArchived(id);
        assertTrue(archivingSuccess);
        assertArchived(storage.readStatus(id));
        final boolean deletingSuccess = storage.markDeleted(id);
        assertTrue(deletingSuccess);
        assertStatus(storage.readStatus(id), true, true);
    }

    @Test
    public void retrieve_empty_status_if_never_written() {
        final Optional<EntityStatus> entityStatus = storage.readStatus(id);
        assertNotNull(entityStatus);
        assertFalse(entityStatus.isPresent());
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static void assertArchived(Optional<EntityStatus> entityStatus) {
        assertStatus(entityStatus, true, false);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static void assertDeleted(Optional<EntityStatus> entityStatus) {
        assertStatus(entityStatus, false, true);
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalGetWithoutIsPresent"})
    private static void assertStatus(Optional<EntityStatus> entityStatus, boolean archived, boolean deleted) {
        assertTrue(entityStatus.isPresent());
        final EntityStatus status = entityStatus.get();
        assertEquals(archived, status.getArchived());
        assertEquals(deleted, status.getDeleted());
    }


    private static class TestAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        protected TestAggregate(ProjectId id) {
            super(id);
        }
    }
}
