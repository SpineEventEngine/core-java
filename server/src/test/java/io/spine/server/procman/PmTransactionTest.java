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
package io.spine.server.procman;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.server.entity.TransactionTest;
import io.spine.server.entity.given.tx.Id;
import io.spine.server.entity.given.tx.PmState;
import io.spine.server.entity.given.tx.TxProcessManager;
import io.spine.server.entity.given.tx.event.TxCreated;
import io.spine.server.entity.given.tx.event.TxErrorRequested;
import io.spine.server.type.EventEnvelope;
import io.spine.validate.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;

import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PmTransaction should")
class PmTransactionTest
        extends TransactionTest<Id,
                                ProcessManager<Id, PmState, PmState.Builder>,
                                PmState,
                                PmState.Builder> {

    private static final Id ID = Id.newBuilder()
                                   .setId("procman-transaction-should-project")
                                   .build();

    @Override
    protected Transaction<Id,
                          ProcessManager<Id, PmState, PmState.Builder>,
                          PmState,
                          PmState.Builder>
    createTx(ProcessManager<Id, PmState, PmState.Builder> entity) {
        return new PmTransaction<>(entity);
    }

    @Override
    protected Transaction<Id,
                          ProcessManager<Id, PmState, PmState.Builder>,
                          PmState,
                          PmState.Builder>
    createTxWithState(ProcessManager<Id, PmState, PmState.Builder> entity,
                      PmState state,
                      Version version) {
        return new PmTransaction<>(entity, state, version);
    }

    @Override
    protected Transaction<Id,
                          ProcessManager<Id, PmState, PmState.Builder>,
                          PmState,
                          PmState.Builder>
    createTxWithListener(ProcessManager<Id, PmState, PmState.Builder> entity,
                         TransactionListener<Id,
                                             ProcessManager<Id, PmState, PmState.Builder>,
                                             PmState,
                                             PmState.Builder> listener) {
        PmTransaction<Id, PmState, PmState.Builder> transaction =
                new PmTransaction<>(entity);
        transaction.setListener(listener);
        return transaction;
    }

    @Override
    protected ProcessManager<Id, PmState, PmState.Builder> createEntity() {
        return new TxProcessManager(ID);
    }

    @Override
    protected
    ProcessManager<Id, PmState, PmState.Builder>
    createEntity(ImmutableList<ConstraintViolation> violations) {
        return new TxProcessManager(ID, violations);
    }

    @Override
    protected PmState createNewState() {
        return PmState.newBuilder()
                             .setId(ID)
                             .setName("The new project name for procman tx tests")
                             .build();
    }

    @Override
    protected void
    checkEventReceived(ProcessManager<Id, PmState, PmState.Builder> entity, Event event) {
        TxProcessManager aggregate = (TxProcessManager) entity;
        Message actualMessage = unpack(event.getMessage());
        assertTrue(aggregate.receivedEvents()
                            .contains(actualMessage));
    }

    @Override
    protected EventMessage createEventMessage() {
        return TxCreated.newBuilder()
                        .setProjectId(ID)
                        .build();
    }

    @Override
    protected EventMessage createEventThatFailsInHandler() {
        return TxErrorRequested.newBuilder()
                               .setProjectId(ID)
                               .build();
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Method called to dispatch event.
    @Override
    protected void applyEvent(Transaction tx, Event event) {
        PmTransaction cast = (PmTransaction) tx;
        EventEnvelope envelope = EventEnvelope.of(event);
        cast.dispatchEvent(envelope);
    }
}
