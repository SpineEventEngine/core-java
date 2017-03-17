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
import org.spine3.server.entity.LifecycleFlags;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests covering the behavior of the {@link AggregateStorage} regarding the {@link LifecycleFlags}.
 *
 * @author Dmytro Dashenkov.
 */
public abstract class AggregateStorageVisibilityHandlingShould {

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
        final LifecycleFlags status = LifecycleFlags.newBuilder()
                                                    .setArchived(true)
                                                    .build();
        storage.writeLifecycleFlags(id, status);
        final Optional<LifecycleFlags> readStatus = storage.readLifecycleFlags(id);
        assertTrue(readStatus.isPresent());
        assertEquals(status, readStatus.get());
    }

    @Test
    public void save_whole_status() {
        final boolean archived = true;
        final boolean deleted = true;
        final LifecycleFlags expected = LifecycleFlags.newBuilder()
                                                      .setArchived(archived)
                                                      .setDeleted(deleted)
                                                      .build();
        storage.writeLifecycleFlags(id, expected);
        final Optional<LifecycleFlags> optionalActual = storage.readLifecycleFlags(id);
        assertStatus(optionalActual, true, true);
    }

    @Test
    public void retrieve_empty_status_if_never_written() {
        final Optional<LifecycleFlags> entityStatus = storage.readLifecycleFlags(id);
        assertNotNull(entityStatus);
        assertFalse(entityStatus.isPresent());
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalGetWithoutIsPresent"})
    private static void assertStatus(Optional<LifecycleFlags> entityStatus,
                                     boolean archived,
                                     boolean deleted) {
        assertTrue(entityStatus.isPresent());
        final LifecycleFlags status = entityStatus.get();
        assertEquals(archived, status.getArchived());
        assertEquals(deleted, status.getDeleted());
    }


    private static class TestAggregate extends Aggregate<ProjectId, Project, Project.Builder> {

        protected TestAggregate(ProjectId id) {
            super(id);
        }
    }
}
