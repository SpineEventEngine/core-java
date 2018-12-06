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
package io.spine.server.projection;

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.core.given.GivenEvent;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.server.entity.TransactionTest;
import io.spine.server.projection.given.ProjectionTransactionTestEnv.PatchedProjectBuilder;
import io.spine.server.projection.given.ProjectionTransactionTestEnv.TestProjection;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjTaskAdded;
import io.spine.validate.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link io.spine.server.projection.ProjectionTransaction}.
 *
 * @author Alex Tymchenko
 */
@DisplayName("ProjectionTransaction should")
class ProjectionTransactionTest
        extends TransactionTest<ProjectId,
                                Projection<ProjectId, Project, PatchedProjectBuilder>,
                                Project,
                                PatchedProjectBuilder> {

    private static final ProjectId ID = ProjectId.newBuilder()
                                                 .setId("projection-transaction-should-project")
                                                 .build();

    @Override
    protected Transaction<ProjectId,
                          Projection<ProjectId, Project, PatchedProjectBuilder>,
                          Project,
                          PatchedProjectBuilder>
    createTx(Projection<ProjectId, Project, PatchedProjectBuilder> entity) {
        return new ProjectionTransaction<>(entity);
    }

    @Override
    protected Transaction<ProjectId,
                          Projection<ProjectId, Project, PatchedProjectBuilder>,
                          Project,
                          PatchedProjectBuilder>
    createTxWithState(Projection<ProjectId, Project, PatchedProjectBuilder> entity,
                      Project state,
                      Version version) {
        return new ProjectionTransaction<>(entity, state, version);
    }

    @Override
    protected Transaction<ProjectId,
                          Projection<ProjectId, Project, PatchedProjectBuilder>,
                          Project,
                          PatchedProjectBuilder>
    createTxWithListener(Projection<ProjectId, Project, PatchedProjectBuilder> entity,
                         TransactionListener<ProjectId,
                                             Projection<ProjectId, Project, PatchedProjectBuilder>,
                                             Project,
                                             PatchedProjectBuilder> listener) {
        ProjectionTransaction<ProjectId, Project, PatchedProjectBuilder> transaction =
                new ProjectionTransaction<>(entity);
        transaction.setListener(listener);
        return transaction;
    }

    @Override
    protected Projection<ProjectId, Project, PatchedProjectBuilder> createEntity() {
        return new TestProjection(ID);
    }

    @Override
    protected Projection<ProjectId, Project, PatchedProjectBuilder>
    createEntity(List<ConstraintViolation> violations) {
        return new TestProjection(ID, violations);
    }

    @Override
    protected Project createNewState() {
        return Project.newBuilder()
                      .setId(ID)
                      .setName("The new name for the projection state in this tx")
                      .build();
    }

    @Override
    protected void checkEventReceived(Projection<ProjectId, Project, PatchedProjectBuilder> entity,
                                      Event event) {

        TestProjection aggregate = (TestProjection) entity;
        Message actualMessage = unpack(event.getMessage());
        assertTrue(aggregate.getReceivedEvents()
                            .contains(actualMessage));
    }

    @Override
    protected EventMessage createEventMessage() {
        return PrjProjectCreated.newBuilder()
                                .setProjectId(ID)
                                .build();
    }

    @Override
    protected EventMessage createEventMessageThatFailsInHandler() {
        return PrjTaskAdded.newBuilder()
                           .setProjectId(ID)
                           .build();
    }

    @Override
    protected void applyEvent(Transaction tx, Event event) {
        ProjectionTransaction cast = (ProjectionTransaction) tx;
        EventEnvelope envelope = EventEnvelope.of(event);
        cast.play(envelope);
    }

    @Override
    protected void breakEntityValidation(
            Projection<ProjectId, Project, PatchedProjectBuilder> entity,
            RuntimeException toThrow) {
        entity.getBuilder()
              .setShouldThrow(toThrow);
    }

    /**
     * Tests the version advancement strategy for the {@link Projection}s.
     *
     * <p>The versioning strategy for {@link Projection} is
     * {@link io.spine.server.entity.AutoIncrement}. This test case substitutes
     * {@link #advanceVersionFromEvent()}, which tested the behavior of
     * {@link io.spine.server.entity.IncrementFromEvent} strategy.
     */
    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Can ignore value of play() in this test.
    @Test
    @DisplayName("increment version on event")
    void incrementVersionOnEvent() {
        Projection<ProjectId, Project, PatchedProjectBuilder> entity = createEntity();
        Version oldVersion = entity.getVersion();
        Event event = GivenEvent.withMessage(createEventMessage());
        Projection.playOn(entity, Collections.singleton(event));
        Version expected = Versions.increment(oldVersion);
        assertEquals(expected.getNumber(), entity.getVersion()
                                                 .getNumber());
        assertNotEquals(event.getContext()
                             .getVersion(), entity.getVersion());
    }
}
