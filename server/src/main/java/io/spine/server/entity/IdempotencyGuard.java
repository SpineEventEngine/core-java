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

package io.spine.server.entity;

import io.spine.annotation.Internal;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.commandbus.DuplicateCommandException;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Events.getRootCommandId;

/**
 * This guard ensures that the message was not yet dispatched to the {@link Aggregate aggregate}.
 * If it was the exception is thrown.
 *
 * @author Mykhailo Drachuk
 * @author Dmytro Dashenkov
 */
@Internal
public final class IdempotencyGuard {

    private final RecentHistory history;

    private IdempotencyGuard(RecentHistory history) {
        this.history = history;
    }

    static IdempotencyGuard lookingAt(RecentHistory history) {
        checkNotNull(history);
        return new IdempotencyGuard(history);
    }

    /**
     * Checks that the command was not dispatched to the aggregate.
     * If it was a {@link DuplicateCommandException} is thrown.
     *
     * @param envelope an envelope with a command to check
     * @throws DuplicateCommandException if the command was dispatched to the aggregate
     */
    public void check(CommandEnvelope envelope) {
        if (didHandleSinceLastSnapshot(envelope)) {
            Command command = envelope.getOuterObject();
            throw DuplicateCommandException.of(command);
        }
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
        Iterator<Event> events = history.iterator();
        while (events.hasNext()) {
            Event event = events.next();
            CommandId eventRootCommandId = getRootCommandId(event);
            if (newCommandId.equals(eventRootCommandId)) {
                return true;
            }
        }
        return false;
    }
}
