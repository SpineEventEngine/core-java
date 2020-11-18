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

import io.spine.core.Subscribe;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.system.server.HandlerFailedUnexpectedly;

import static io.spine.json.Json.toJson;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Performs logging of failed signals handling or fails the test depending the current
 * context exception tolerance.
 */
final class ExceptionGuard extends AbstractEventSubscriber implements ExceptionLogging {

    private ExceptionTolerance tolerance = ExceptionTolerance.RAISE;

    @Subscribe
    void on(HandlerFailedUnexpectedly event) {
        String msg = format(
                "The entity (state type `%s`) could not handle the signal `%s`:%n%s%n",
                event.getEntity().getTypeUrl(),
                event.getHandledSignal().getTypeUrl(),
                event.getError().getMessage()
        );
        switch (tolerance){
            case LOG:
                log(event, msg);
                break;
            case RAISE:
            default:
                fail(() -> msg + lineSeparator() + toJson(event));
        }
    }

    /**
     * Asks the guard to tolerate exceptions.
     */
    void tolerate() {
        tolerance = ExceptionTolerance.LOG;
    }
}
