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

package io.spine.server.aggregate;

import com.google.common.base.Optional;
import io.spine.base.Identifier;
import io.spine.server.entity.LifecycleFlags;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests covering the behavior of the {@link AggregateStorage} regarding the {@link LifecycleFlags}.
 *
 * @author Dmytro Dashenkov.
 */
@DisplayName("AggregateStorage, when saving aggregate with lifecycle flags, should")
public abstract class AggregateStorageVisibilityHandlingTest {

    protected abstract AggregateStorage<ProjectId> getAggregateStorage(
            Class<? extends Aggregate<ProjectId, ?, ?>> aggregateClass);

    private AggregateStorage<ProjectId> storage;

    private final ProjectId id = ProjectId.newBuilder()
                                          .setId(Identifier.newUuid())
                                          .build();

    @BeforeEach
    void setUp() {
        storage = getAggregateStorage(TestAggregate.class);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // Checked in an assertion
    @Test
    @DisplayName("write entity status of aggregate")
    void writeEntityStatusOfAggregate() {
        final LifecycleFlags status = LifecycleFlags.newBuilder()
                                                    .setArchived(true)
                                                    .build();
        storage.writeLifecycleFlags(id, status);
        final Optional<LifecycleFlags> readStatus = storage.readLifecycleFlags(id);
        assertTrue(readStatus.isPresent());
        assertEquals(status, readStatus.get());
    }

    @Test
    @DisplayName("save whole status")
    void saveWholeStatus() {
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
    @DisplayName("retrieve empty status if never written")
    void getEmptyStatusIfNeverWritten() {
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


    private static class TestAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        protected TestAggregate(ProjectId id) {
            super(id);
        }
    }
}
