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
package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.server.entity.TransactionTest;
import io.spine.server.entity.given.tx.AggregateState;
import io.spine.server.entity.given.tx.Id;
import io.spine.server.entity.given.tx.TxAggregate;
import io.spine.server.entity.given.tx.event.TxErrorRequested;
import io.spine.server.type.EventEnvelope;
import io.spine.validate.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.unpack;

@DisplayName("AggregateTransaction should")
class AggregateTransactionTest
        extends TransactionTest<Id,
                                Aggregate<Id, AggregateState, AggregateState.Builder>,
                                AggregateState,
                                AggregateState.Builder> {

    private static final Id ID = Id.newBuilder()
                                   .setId("aggregate-transaction-should-project")
                                   .build();

    @Override
    protected Transaction<Id,
                          Aggregate<Id, AggregateState, AggregateState.Builder>,
                          AggregateState,
                          AggregateState.Builder>
    createTx(Aggregate<Id, AggregateState, AggregateState.Builder> entity) {
        return new AggregateTransaction<>(entity);
    }

    @Override
    protected Transaction<Id,
                          Aggregate<Id, AggregateState, AggregateState.Builder>,
                          AggregateState,
                          AggregateState.Builder>
    createTxWithState(Aggregate<Id, AggregateState, AggregateState.Builder> entity,
                      AggregateState state,
                      Version version) {
        return new AggregateTransaction<>(entity, state, version);
    }

    @Override
    protected Transaction<Id,
                          Aggregate<Id, AggregateState, AggregateState.Builder>,
                          AggregateState,
                          AggregateState.Builder>
    createTxWithListener(Aggregate<Id, AggregateState, AggregateState.Builder> entity,
                         TransactionListener<Id,
                                             Aggregate<Id, AggregateState, AggregateState.Builder>,
                                             AggregateState,
                                             AggregateState.Builder> listener) {
        AggregateTransaction<Id, AggregateState, AggregateState.Builder> transaction =
                new AggregateTransaction<>(entity);
        transaction.setListener(listener);
        return transaction;
    }

    @Override
    protected Aggregate<Id, AggregateState, AggregateState.Builder> createEntity() {
        return new TxAggregate(ID);
    }

    @Override
    protected Aggregate<Id, AggregateState, AggregateState.Builder>
    createEntity(ImmutableList<ConstraintViolation> violations) {
        return new TxAggregate(ID, violations);
    }

    @Override
    protected AggregateState createNewState() {
        return AggregateState.newBuilder()
                             .setId(ID)
                             .setName("The new project name to set in tx")
                             .build();
    }

    @Override
    protected
    void checkEventReceived(Aggregate<Id, AggregateState, AggregateState.Builder> entity,
                            Event event) {
        TxAggregate aggregate = (TxAggregate) entity;
        Message actualMessage = unpack(event.getMessage());

        assertThat(aggregate.receivedEvents())
                .contains(actualMessage);
    }

    @Override
    protected EventMessage createEventMessage() {
        return TxAggregate.projectCreated(ID, "Project created in a transaction");
    }

    @Override
    protected EventMessage createEventThatFailsInHandler() {
        return taskAdded(ID);
    }

    public static TxErrorRequested taskAdded(Id id) {
        return TxErrorRequested
                .newBuilder()
                .setId(id)
                .build();
    }

    @Override
    protected void applyEvent(Transaction tx, Event event) {
        AggregateTransaction cast = (AggregateTransaction) tx;
        EventEnvelope envelope = EventEnvelope.of(event);
        cast.play(envelope);
    }

    @Test
    @DisplayName("advance version from event")
    void eventFromVersion() {
        advanceVersionFromEvent();
    }

}
