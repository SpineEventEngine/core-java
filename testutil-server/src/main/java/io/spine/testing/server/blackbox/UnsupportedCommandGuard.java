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

package io.spine.testing.server.blackbox;

import io.spine.base.Error;
import io.spine.core.CommandValidationError;
import io.spine.system.server.event.CommandErrored;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.CommandValidationError.UNSUPPORTED_COMMAND_VALUE;
import static io.spine.server.commandbus.CommandException.ATTR_COMMAND_TYPE_NAME;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A guard that verifies that the commands posted to the {@link BlackBoxBoundedContext} are not the
 * {@linkplain io.spine.server.bus.DeadMessageHandler "dead"} messages.
 */
final class UnsupportedCommandGuard {

    private static final String COMMAND_VALIDATION_ERROR_TYPE =
            CommandValidationError.getDescriptor()
                                  .getFullName();

    /**
     * A command type for which the violation occurs in printable form.
     */
    private @Nullable String commandType;

    /**
     * Checks if the given {@link CommandErrored} event represents an "unsupported" error and,
     * if so, remembers its data.
     */
    boolean checkAndRemember(CommandErrored event) {
        Error error = event.getError();
        if (!isUnsupportedError(error)) {
            return false;
        }
        commandType = error.getAttributesMap()
                           .get(ATTR_COMMAND_TYPE_NAME)
                           .getStringValue();
        return true;
    }

    /**
     * Throws an {@link AssertionError}.
     *
     * <p>The method is assumed to be called after a violation was found for some
     * {@link #commandType}.
     */
    void failTest() {
        checkNotNull(commandType);
        fail(format("Handler for commands of type %s is not registered within the context.",
                    commandType));
    }

    private static boolean isUnsupportedError(Error error) {
        return COMMAND_VALIDATION_ERROR_TYPE.equals(error.getType())
                && error.getCode() == UNSUPPORTED_COMMAND_VALUE;
    }
}
