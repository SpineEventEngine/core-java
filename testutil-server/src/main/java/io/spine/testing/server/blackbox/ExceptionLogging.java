/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import io.spine.base.EventMessage;
import io.spine.logging.Logging;

import java.io.PrintStream;

import static io.spine.json.Json.toJson;
import static java.lang.String.format;

/**
 * Provides a handy shortcut for logging exceptions happened during
 * {@linkplain EventMessage events} handling.
 */
interface ExceptionLogging extends Logging {

    /**
     * Performs exception logging of the supplied {@code event}.
     *
     * @param errorMessage
     *         the error message to log
     * @param formatArgs
     *         the arguments, if any, for the error message
     */
    @FormatMethod
    default void log(EventMessage event, @FormatString String errorMessage, Object... formatArgs) {
        String msg = format(errorMessage, formatArgs);
        log(event, msg);
    }

    /**
     * Performs exception logging of the supplied {@code event}.
     *
     * @param msg
     *         the formatted error message to log
     */
    @FormatMethod
    default void log(EventMessage event, String msg) {
        FluentLogger.Api severeLogger = logger()
                .atSevere()
                .withStackTrace(StackSize.NONE);
        boolean loggingEnabled = severeLogger.isEnabled();
        if (loggingEnabled) {
            severeLogger.log(msg);
            severeLogger.log(toJson(event));
        } else {
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            // Edge case for disabled/misconfigured logging .
            PrintStream stderr = System.err;
            stderr.println(msg);
            stderr.println(toJson(event));
        }
    }
}
