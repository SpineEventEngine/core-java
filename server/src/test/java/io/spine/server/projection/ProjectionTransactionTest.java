/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import io.spine.code.proto.FieldDeclaration;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.server.entity.TransactionTest;
import io.spine.server.entity.VersionIncrement;
import io.spine.server.entity.given.tx.Id;
import io.spine.server.entity.given.tx.ProjectionState;
import io.spine.server.entity.given.tx.ProjectionState.ProjectionType;
import io.spine.server.entity.given.tx.TxProjection;
import io.spine.server.entity.given.tx.event.TxCreated;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.type.MessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.given.tx.ProjectionState.ProjectionType.VERY_USEFUL;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link io.spine.server.projection.ProjectionTransaction}.
 */
@DisplayName("`ProjectionTransaction` should")
class ProjectionTransactionTest
        extends TransactionTest<Id,
        Projection<Id, ProjectionState, ProjectionState.Builder>,
        ProjectionState,
        ProjectionState.Builder> {

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
    createTx(Projection<Id, ProjectionState, ProjectionState.Builder> entity,
             ProjectionState state,
             Version version) {
        return new ProjectionTransaction<>(entity, state, version);
    }

    @Override
    protected Transaction<Id,
                          Projection<Id, ProjectionState, ProjectionState.Builder>,
                          ProjectionState,
                          ProjectionState.Builder>
    createTx(Projection<Id, ProjectionState, ProjectionState.Builder> entity,
             TransactionListener<Id> listener) {
        ProjectionTransaction<Id, ProjectionState, ProjectionState.Builder> transaction =
                new ProjectionTransaction<>(entity);
        transaction.setListener(listener);
        return transaction;
    }

    @Override
    protected Projection<Id, ProjectionState, ProjectionState.Builder> createEntity() {
        return new TxProjection(id());
    }

    @Override
    protected ProjectionState newState() {
        String nameString = "The new name for the projection state in this tx";
        return ProjectionState
                .newBuilder()
                .setId(id())
                .setName(nameString)
                .setNameLength(nameString.length())
                .setType(VERY_USEFUL)
                .build();
    }


    @Override
    protected void checkEventReceived(
            Projection<Id, ProjectionState, ProjectionState.Builder> entity,
            Event event) {

        TxProjection aggregate = (TxProjection) entity;
        Message actualMessage = unpack(event.getMessage());
        assertTrue(aggregate.receivedEvents()
                            .contains(actualMessage));
    }

    @SuppressWarnings("rawtypes") // For the brevity of the test.
    @Override
    protected DispatchOutcome applyEvent(Transaction tx, Event event) {
        ProjectionTransaction cast = (ProjectionTransaction) tx;
        EventEnvelope envelope = EventEnvelope.of(event);
        return cast.play(envelope);
    }

    /**
     * Tests the version advancement strategy for the {@link Projection}s.
     *
     * <p>The versioning strategy for {@link Projection} is
     * {@link VersionIncrement.AutoIncrement}. This test case substitutes
     * {@link #advanceVersionFromEvent()}, which tested the behavior of
     * {@link VersionIncrement.IncrementFromEvent} strategy.
     */
    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Can ignore value of play() in this test.
    @Test
    @DisplayName("increment version on event")
    void incrementVersionOnEvent() {
        Projection<Id, ProjectionState, ProjectionState.Builder> entity = createEntity();
        Version oldVersion = entity.version();
        Version eventVersion = Version
                .newBuilder()
                .setNumber(42)
                .setTimestamp(currentTime())
                .build();
        Event event = GivenEvent.withMessageAndVersion(createEventMessage(), eventVersion);
        Projection.playOn(entity, singleton(event));
        Version expected = Versions.increment(oldVersion);

        assertThat(entity.version()
                         .getNumber())
                .isEqualTo(expected.getNumber());
        assertThat(entity.version())
                .isNotEqualTo(event.context()
                                   .getVersion());
    }

    @Test
    @DisplayName("propagate column values to the entity state on commit")
    void propagateColumnValues() {
        TxProjection entity = (TxProjection) createEntity();
        String name = "some-projection-name";
        TxCreated txCreated = TxCreated
                .newBuilder()
                .setId(id())
                .setName(name)
                .build();
        Event event = GivenEvent.withMessage(txCreated);
        Projection.playOn(entity, singleton(event));

        int nameLength = entity.state()
                               .getNameLength();
        assertThat(nameLength).isEqualTo(entity.getNameLength());

        ProjectionType type = entity.state()
                                    .getType();
        assertThat(type).isEqualTo(entity.getType());
    }

    @Test
    @DisplayName("propagate `null` column values as default values for the field")
    void propagateNullColumnValues() {
        Id id = Id.newBuilder()
                  .setId(newUuid())
                  .build();
        TxProjection entity = new TxProjection(id, true);

        String name = newUuid();
        TxCreated txCreated = TxCreated
                .newBuilder()
                .setId(id())
                .setName(name)
                .build();
        Event event = GivenEvent.withMessage(txCreated);
        Projection.playOn(entity, singleton(event));

        ProjectionState state = entity.state();
        MessageType messageType = new MessageType(state.getDescriptorForType());
        FieldDeclaration field = messageType.field("type");
        ProjectionType actualType = state.getType();

        EnumValueDescriptor actualTypeDescriptor =
                ((ProtocolMessageEnum) actualType).getValueDescriptor();
        Object defaultTypeDescriptor = field.descriptor()
                                            .getDefaultValue();
        assertThat(actualTypeDescriptor).isEqualTo(defaultTypeDescriptor);

        // Make sure the transaction wasn't rolled back.
        assertThat(entity.version().getNumber()).isEqualTo(1);
    }
}
