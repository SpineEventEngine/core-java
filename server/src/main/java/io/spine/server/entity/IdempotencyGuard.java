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
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.event.DuplicateEventException;

import static com.google.common.base.Preconditions.checkNotNull;

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

    public void check(CommandEnvelope envelope) {
        Command command = envelope.getCommand();
        boolean duplicate = history.contains(command);
        if (duplicate) {
            throw DuplicateCommandException.of(command);
        }
    }

    public void check(EventEnvelope envelope) {
        Event event = envelope.getOuterObject();
        boolean duplicate = history.contains(event);
        if (duplicate) {
            throw new DuplicateEventException(history.entity(), envelope);
        }
    }
}
