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

package io.spine.server.entity;

import io.spine.core.Event;
import io.spine.core.MessageId;
import io.spine.server.type.EventEnvelope;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A process of propagation of a number of events.
 */
final class PropagationProcess {

    private final EventPlayingTransaction<?, ?, ?, ?> transaction;

    private final Propagation.Builder propagation = Propagation.newBuilder();
    private boolean successful = true;
    private MessageId lastMessage = MessageId.getDefaultInstance();

    PropagationProcess(EventPlayingTransaction<?, ?, ?, ?> transaction) {
        this.transaction = checkNotNull(transaction);
        propagation.setTargetEntity(transaction.entityId());
    }

    /**
     * Propagates the given event and memorizes the propagation result.
     */
    void play(Event event) {
        if (successful) {
            EventEnvelope eventEnvelope = EventEnvelope.of(event);
            PropagationOutcome outcome = transaction.play(eventEnvelope);
            propagation.addOutcome(outcome);
            successful = !outcome.hasError();
            lastMessage = event.messageId();
        } else {
            Interruption interruption = Interruption
                    .newBuilder()
                    .setStoppedAt(lastMessage)
                    .buildPartial();
            PropagationOutcome outcome = PropagationOutcome
                    .newBuilder()
                    .setPropagatedSignal(event.messageId())
                    .setInterrupted(interruption)
                    .vBuild();
            propagation.addOutcome(outcome);
        }
    }

    /**
     * Obtains the summary of all the propagates events.
     */
    Propagation summary() {
        return propagation
                .setSuccessful(successful)
                .vBuild();
    }
}
