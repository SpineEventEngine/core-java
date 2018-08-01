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
import io.spine.core.Version;

import java.util.Optional;

/**
 * @author Dmytro Dashenkov
 */
public interface HandledError {

    /**
     * Rethrows the handled exception if it was <b>not</b> caused by a rejection or
     * rethrown earlier.
     *
     * <p>Otherwise, preforms no action.
     */
    void rethrowOnce();

    /**
     * Converts the handled error into a rejection {@linkplain Event event}.
     *
     * <p>The produced {@link Event} does not have a {@link Version}.
     * The {@linkplain io.spine.core.EventContext#getProducerId() producer ID} is a string with
     * the value equal to {@code "CommandErrorHandler"}.
     *
     * @return the handled rejection event or {@link Optional#empty()} if the handled error is
     * not a command rejection
     */
    Optional<Event> asRejection();

    static HandledError forRuntime(RuntimeException exception) {
        return new HandledRuntimeError(exception);
    }

    static HandledError forRejection(RuntimeException rejection, CommandEnvelope command) {
        return new HandledRejection(command, rejection);
    }
}
