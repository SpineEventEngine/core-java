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

import io.spine.annotation.Internal;
import io.spine.core.CommandEnvelope;
import io.spine.server.event.RejectionEnvelope;

import java.util.Optional;

/**
 * A result of an error handling.
 *
 * <p>Provides capabilities for further dealing with the error.
 *
 * <p>If the error is not handled before, it can be {@linkplain #rethrowOnce() rethrown}.
 * A rethrown error is considered handled, thus will not be rethrown if caught again.
 *
 * <p>If the error represents a command rejection, the rejection can be
 * {@linkplain #asRejection() obtained} for further manipulations. A rejection cannot be rethrown.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public interface CaughtError {

    /**
     * Rethrows the caught exception if it was <b>not</b> caused by a rejection or
     * rethrown earlier.
     *
     * <p>Otherwise, preforms no action.
     */
    default void rethrowOnce() throws CommandDispatchingException {
        // Do nothing.
    }

    /**
     * Converts the caught error into a {@linkplain RejectionEnvelope rejection}.
     *
     * @return the caught rejection event or {@link Optional#empty()} if the caught error is
     * not a command rejection
     */
    default Optional<RejectionEnvelope> asRejection() {
        return Optional.empty();
    }

    /**
     * Obtains a {@link CaughtError} for a previously handled error.
     */
    static CaughtError handled() {
        return HandledError.INSTANCE;
    }

    /**
     * Obtains a {@link CaughtError} for the given {@link RuntimeException}.
     *
     * @param exception the caught error
     * @return wrapped error
     */
    static CaughtError ofRuntime(RuntimeException exception) {
        return new CaughtRuntimeError(exception);
    }

    /**
     * Obtains a {@link CaughtError} for the given {@linkplain io.spine.base.ThrowableMessage
     * rejection}.
     *
     * @param rejection the {@link RuntimeException} caused by
     *                  a {@linkplain io.spine.base.ThrowableMessage ThrowableMessage}
     * @param command   the rejected command
     * @return wrapped rejection
     */
    static CaughtError ofRejection(RuntimeException rejection, CommandEnvelope command) {
        return new CaughtRejection(command, rejection);
    }
}
