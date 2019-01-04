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

package io.spine.server.integration.given;

import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.test.integration.event.ItgProjectStarted;

import static io.spine.util.Exceptions.illegalStateWithCauseOf;

@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
public class ProjectEventsSubscriber extends AbstractEventSubscriber {

    private static ItgProjectCreated externalEvent = null;

    private static ItgProjectStarted domesticEvent = null;

    @Subscribe(external = true)
    public void on(ItgProjectCreated msg) {
        externalEvent = msg;
    }

    @Subscribe
    public void on(ItgProjectStarted msg) {
        domesticEvent = msg;
    }

    public static ItgProjectCreated getExternalEvent() {
        return externalEvent;
    }

    public static ItgProjectStarted getDomesticEvent() {
        return domesticEvent;
    }

    public static void clear() {
        externalEvent = null;
        domesticEvent = null;
    }

    /**
     * Rethrow all the issues, so that they are visible to tests.
     */
    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        throw illegalStateWithCauseOf(exception);
    }
}
