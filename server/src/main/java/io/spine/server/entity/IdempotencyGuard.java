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
import io.spine.core.TenantId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.event.DuplicateEventException;
import io.spine.server.tenant.TenantAwareOperation;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.validate.Validate.isNotDefault;

/**
 * This guard ensures that the message was not yet dispatched to the {@link Aggregate aggregate}.
 * If it was the exception is thrown.
 *
 * @author Mykhailo Drachuk
 * @author Dmytro Dashenkov
 */
@Internal
public final class IdempotencyGuard {

    private final HistoryLog history;

    private IdempotencyGuard(HistoryLog history) {
        this.history = history;
    }

    static IdempotencyGuard lookingAt(HistoryLog history) {
        checkNotNull(history);
        return new IdempotencyGuard(history);
    }

    public void check(CommandEnvelope envelope) {
        tenantAware(envelope.getTenantId(), () -> checkCommand(envelope));
    }

    private void checkCommand(CommandEnvelope envelope) {
        Command command = envelope.getCommand();
        boolean duplicate = history.contains(command);
        if (duplicate) {
            throw DuplicateCommandException.of(command);
        }
    }

    public void check(EventEnvelope envelope) {
        tenantAware(envelope.getTenantId(), () -> checkEvent(envelope));
    }

    private void checkEvent(EventEnvelope envelope) {
        Event event = envelope.getOuterObject();
        boolean duplicate = history.contains(event);
        if (duplicate) {
            throw new DuplicateEventException(history.id(), envelope);
        }
    }

    private static void tenantAware(TenantId tenantId, Runnable operation) {
        if (isNotDefault(tenantId)) {
            new TenantAwareOperation(tenantId) {
                @Override
                public void run() {
                    operation.run();
                }
            }.execute();
        } else {
            operation.run();
        }
    }
}
