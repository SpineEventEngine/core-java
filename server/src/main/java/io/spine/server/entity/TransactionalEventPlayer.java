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
 * An {@link EventPlayer} which plays events upon the given {@link Transaction}.
 */
final class TransactionalEventPlayer implements EventPlayer {

    private final EventPlayingTransaction<?, ?, ?, ?> transaction;

    /**
     * Creates a new instance of {@code TransactionalEventPlayer}.
     *
     * @param transaction the transaction
     */
    TransactionalEventPlayer(EventPlayingTransaction<?, ?, ?, ?> transaction) {
        this.transaction = checkNotNull(transaction);
    }

    /**
     * Plays the given events upon the underlying entity transaction.
     */
    @Override
    public Propagation play(Iterable<Event> events) {
        checkNotNull(events);
        PropagationProcess process = new PropagationProcess(transaction);
        for (Event event : events) {
            process.play(event);
        }
        return process.buildPropagationResult();
    }

    private static final class PropagationProcess {

        private final EventPlayingTransaction<?, ?, ?, ?> transaction;

        private final Propagation.Builder propagation = Propagation.newBuilder();
        private boolean successful = true;
        private MessageId lastMessage = MessageId.getDefaultInstance();

        private PropagationProcess(EventPlayingTransaction<?, ?, ?, ?> transaction) {
            this.transaction = transaction;
            propagation.setTargetEntity(transaction.entityId());
        }

        private void play(Event event) {
            if (successful) {
                EventEnvelope eventEnvelope = EventEnvelope.of(event);
                PropagationOutcome outcome = transaction.play(eventEnvelope);
                propagation.addOutcome(outcome);
                successful = outcome.hasSuccess();
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

        private Propagation buildPropagationResult() {
            return propagation.vBuild();
        }
    }
}
