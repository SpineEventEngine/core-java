/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import io.spine.server.commandbus.DuplicateCommandException;

import java.util.Iterator;

import static io.spine.core.Events.getRootCommandId;

class IdempotencyGuard {
    private final Aggregate<?, ?, ?> aggregate;

    IdempotencyGuard(Aggregate<?, ?, ?> aggregate) {
        this.aggregate = aggregate;
    }

    void ensureIdempotence(CommandEnvelope envelope) {
        if (didHandleSinceLastSnapshot(envelope)) {
            final Command command = envelope.getOuterObject();
            throw DuplicateCommandException.forAggregate(command);
        }
    }

    /**
     * Checks if the command was already handled by the aggregate since last snapshot.
     *
     * <p> The check is performed by searching for an event caused by this command that was
     * committed since last snapshot.
     *
     * <p> This functionality support the ability to stop duplicate commands from being dispatched
     * to the aggregate.
     *
     * @param envelope the command to check
     * @return {@code true} if the command was handled since last snapshot, {@code false} otherwise
     */
    private boolean didHandleSinceLastSnapshot(CommandEnvelope envelope) {
        final CommandId newCommandId = envelope.getId();
        final Iterator<Event> iterator = aggregate.historyBackward();
        while (iterator.hasNext()) {
            final Event event = iterator.next();
            final CommandId eventRootCommandId = getRootCommandId(event);
            if (newCommandId.equals(eventRootCommandId)) {
                return true;
            }
        }
        return false;
    }
}
