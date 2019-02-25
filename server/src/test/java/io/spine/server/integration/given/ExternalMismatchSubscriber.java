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

package io.spine.server.integration.given;

import io.spine.core.Subscribe;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.type.EventEnvelope;
import io.spine.test.integration.command.ItgStartProject;
import io.spine.test.integration.rejection.IntegrationRejections;

import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * A subscriber for testing of external attribute mismatch check.
 */
@SuppressWarnings("unused") // OK to have unused params in this test env. class
public final class ExternalMismatchSubscriber extends AbstractEventSubscriber {

    @Subscribe(external = true)
    void on(IntegrationRejections.ItgCannotStartArchivedProject rejection, ItgStartProject command) {
        // do nothing.
    }

    @Subscribe
    public void on(IntegrationRejections.ItgCannotStartArchivedProject rejection) {
        // do nothing.
    }

    /**
     * Rethrow all the issues, so that they are visible to tests.
     */
    @Override
    public void onError(EventEnvelope event, RuntimeException exception) {
        throw illegalStateWithCauseOf(exception);
    }
}
