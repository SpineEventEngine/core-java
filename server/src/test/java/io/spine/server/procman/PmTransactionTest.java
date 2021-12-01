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
package io.spine.server.procman;

import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.server.entity.TransactionTest;
import io.spine.server.entity.given.tx.Id;
import io.spine.server.entity.given.tx.PmState;
import io.spine.server.entity.given.tx.TxProcessManager;
import io.spine.server.type.EventEnvelope;
import org.junit.jupiter.api.DisplayName;

import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PmTransaction should")
class PmTransactionTest
        extends TransactionTest<Id,
                                ProcessManager<Id, PmState, PmState.Builder>,
                                PmState,
                                PmState.Builder> {

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
    createTx(ProcessManager<Id, PmState, PmState.Builder> entity,
             PmState state,
             Version version) {
        return new PmTransaction<>(entity, state, version);
    }

    @Override
    protected Transaction<Id,
                          ProcessManager<Id, PmState, PmState.Builder>,
                          PmState,
                          PmState.Builder>
    createTx(ProcessManager<Id, PmState, PmState.Builder> entity,
             TransactionListener<Id> listener) {
        var transaction = new PmTransaction<>(entity);
        transaction.setListener(listener);
        return transaction;
    }

    @Override
    protected ProcessManager<Id, PmState, PmState.Builder> createEntity() {
        return new TxProcessManager(id());
    }

    @Override
    protected PmState newState() {
        return PmState.newBuilder()
                .setId(id())
                .setName("The new project name for procman tx tests")
                .build();
    }

    @Override
    protected void
    checkEventReceived(ProcessManager<Id, PmState, PmState.Builder> entity, Event event) {
        var aggregate = (TxProcessManager) entity;
        var actualMessage = unpack(event.getMessage());
        assertTrue(aggregate.receivedEvents()
                            .contains(actualMessage));
    }

    @Override
    @SuppressWarnings("rawtypes") /* Avoiding the looong list of generics. */
    protected DispatchOutcome applyEvent(Transaction tx, Event event) {
        var cast = (PmTransaction) tx;
        var envelope = EventEnvelope.of(event);
        return cast.dispatchEvent(envelope);
    }
}
