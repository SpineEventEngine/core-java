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

package io.spine.server.command;

import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.server.event.RejectionEnvelope;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper for a handled {@linkplain io.spine.base.ThrowableMessage rejection}.
 *
 * <p>Performs no action on {@link #rethrowOnce()} and returns the given rejection event
 * on {@link #asRejection()}.
 */
final class CaughtRejection implements CaughtError {

    private final RejectionEnvelope rejection;

    CaughtRejection(CommandEnvelope origin, RuntimeException exception) {
        checkNotNull(origin);
        checkNotNull(exception);

        this.rejection = RejectionEnvelope.from(origin, exception);
    }

    @Override
    public Optional<Event> asRejection() {
        Event event = rejection.getOuterObject();
        return Optional.of(event);
    }
}
