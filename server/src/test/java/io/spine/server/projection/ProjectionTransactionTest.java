/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.server.entity.TransactionTest;
import io.spine.server.entity.given.tx.Id;
import io.spine.server.entity.given.tx.ProjectionState;
import io.spine.server.entity.given.tx.TxProjection;
import io.spine.server.entity.given.tx.event.TxCreated;
import io.spine.server.entity.given.tx.event.TxErrorRequested;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.validate.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link io.spine.server.projection.ProjectionTransaction}.
 */
@DisplayName("ProjectionTransaction should")
class ProjectionTransactionTest
        extends TransactionTest<Id,
                                Projection<Id, ProjectionState, ProjectionState.Builder>,
        ProjectionState,
        ProjectionState.Builder> {

    private static final Id ID = Id.newBuilder()
                                   .setId("projection-transaction-should-project")
                                   .build();

    @Override
    protected Transaction<Id,
                          Projection<Id, ProjectionState, ProjectionState.Builder>,
            ProjectionState,
            ProjectionState.Builder>
    createTx(Projection<Id, ProjectionState, ProjectionState.Builder> entity) {
        return new ProjectionTransaction<>(entity);
    }

    @Override
    protected Transaction<Id,
                          Projection<Id, ProjectionState, ProjectionState.Builder>,
            ProjectionState,
            ProjectionState.Builder>
    createTxWithState(Projection<Id, ProjectionState, ProjectionState.Builder> entity,
                      ProjectionState state,
                      Version version) {
        return new ProjectionTransaction<>(entity, state, version);
    }

    @Override
    protected Transaction<Id,
                          Projection<Id, ProjectionState, ProjectionState.Builder>,
            ProjectionState,
            ProjectionState.Builder>
    createTxWithListener(Projection<Id, ProjectionState, ProjectionState.Builder> entity,
                         TransactionListener<Id,
                                             Projection<Id, ProjectionState, ProjectionState.Builder>,
                                 ProjectionState,
                                 ProjectionState.Builder> listener) {
        ProjectionTransaction<Id, ProjectionState, ProjectionState.Builder> transaction =
                new ProjectionTransaction<>(entity);
        transaction.setListener(listener);
        return transaction;
    }

    @Override
    protected Projection<Id, ProjectionState, ProjectionState.Builder> createEntity() {
        return new TxProjection(ID);
    }

    @Override
    protected Projection<Id, ProjectionState, ProjectionState.Builder>
    createEntity(ImmutableList<ConstraintViolation> violations) {
        return new TxProjection(ID, violations);
    }

    @Override
    protected ProjectionState createNewState() {
        return ProjectionState.newBuilder()
                             .setId(ID)
                             .setName("The new name for the projection state in this tx")
                             .build();
    }

    @Override
    protected void checkEventReceived(Projection<Id, ProjectionState, ProjectionState.Builder> entity,
                                      Event event) {

        TxProjection aggregate = (TxProjection) entity;
        Message actualMessage = unpack(event.getMessage());
        assertTrue(aggregate.receivedEvents()
                            .contains(actualMessage));
    }

    @Override
    protected EventMessage createEventMessage() {
        return TxCreated.newBuilder()
                        .setId(ID)
                        .build();
    }

    @Override
    protected EventMessage createEventThatFailsInHandler() {
        return TxErrorRequested.newBuilder()
                               .setId(ID)
                               .build();
    }

    @Override
    protected void applyEvent(Transaction tx, Event event) {
        ProjectionTransaction cast = (ProjectionTransaction) tx;
        EventEnvelope envelope = EventEnvelope.of(event);
        cast.play(envelope);
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
        Projection<Id, ProjectionState, ProjectionState.Builder> entity = createEntity();
        Version oldVersion = entity.version();
        Event event = GivenEvent.withMessage(createEventMessage());
        Projection.playOn(entity, Collections.singleton(event));
        Version expected = Versions.increment(oldVersion);

        assertThat(entity.version().getNumber())
                .isEqualTo(expected.getNumber());
        assertThat(entity.version())
                .isNotEqualTo(event.context()
                                   .getVersion());
    }
}
