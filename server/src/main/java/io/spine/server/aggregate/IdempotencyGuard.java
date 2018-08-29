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

import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.EventId;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.event.DuplicateEventException;

import java.util.Iterator;

/**
 * This guard ensures that the message was not yet dispatched to the {@link Aggregate aggregate}.
 * If it was, the exception is thrown.
 *
 * @author Mykhailo Drachuk
 * @author Dmytro Dashenkov
 */
final class IdempotencyGuard {

    private final Aggregate<?, ?, ?> aggregate;

    IdempotencyGuard(Aggregate<?, ?, ?> aggregate) {
        this.aggregate = aggregate;
    }

    /**
     * Checks that the command was not dispatched to the aggregate.
     * If it was a {@link DuplicateCommandException} is thrown.
     *
     * @param envelope an envelope with a command to check
     * @throws DuplicateCommandException if the command was dispatched to the aggregate
     */
    void check(CommandEnvelope envelope) {
        if (didHandleSinceLastSnapshot(envelope)) {
            Command command = envelope.getOuterObject();
            throw DuplicateCommandException.of(command);
        }
    }

    /**
     * Checks that the event was not dispatched to the aggregate.
     * If it was a {@link DuplicateEventException} is thrown.
     *
     * @param envelope an envelope with an event to check
     * @throws DuplicateEventException if the event was dispatched to the aggregate
     */
    void check(EventEnvelope envelope) {
        if (didHandleSinceLastSnapshot(envelope)) {
            Event event = envelope.getOuterObject();
            throw new DuplicateEventException(event);
        }
    }

    /**
     * Checks if the event was already handled by the aggregate since last snapshot.
     *
     * <p>The check is performed by searching for an event caused by this event that was
     * committed since last snapshot.
     *
     * <p>This functionality supports the ability to stop duplicate events from being dispatched
     * to the aggregate.
     *
     * @param envelope the event to check
     * @return {@code true} if the event was handled since last snapshot, {@code false} otherwise
     */
    private boolean didHandleSinceLastSnapshot(EventEnvelope envelope) {
        EventId eventId = envelope.getId();
        Iterator<Event> iterator = aggregate.historyBackward();
        while (iterator.hasNext()) {
            Event event = iterator.next();
            EventId originEventId = event.getContext().getEventId();
            if (eventId.equals(originEventId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the command was already handled by the aggregate since last snapshot.
     *
     * <p>The check is performed by searching for an event caused by this command that was
     * committed since last snapshot.
     *
     * <p>This functionality supports the ability to stop duplicate commands from being dispatched
     * to the aggregate.
     *
     * @param envelope the command to check
     * @return {@code true} if the command was handled since last snapshot, {@code false} otherwise
     */
    private boolean didHandleSinceLastSnapshot(CommandEnvelope envelope) {
        CommandId newCommandId = envelope.getId();
        Iterator<Event> iterator = aggregate.historyBackward();
        while (iterator.hasNext()) {
            Event event = iterator.next();
            CommandId originCommandId = event.getContext().getCommandId();
            if (newCommandId.equals(originCommandId)) {
                return true;
            }
        }
        return false;
    }
}
