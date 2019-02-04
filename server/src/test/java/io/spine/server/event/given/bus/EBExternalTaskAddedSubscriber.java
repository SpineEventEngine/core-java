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

package io.spine.server.event.given.bus;

import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.json.Json;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.test.event.EBTaskAdded;
import io.spine.test.event.ProjectCreated;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

public class EBExternalTaskAddedSubscriber extends AbstractEventSubscriber {

    private boolean receivedMessage;

    @Subscribe(external = true)
    void on(EBTaskAdded message, EventContext context) {
        if (!context.getExternal()) {
            fail(format(
                    "Domestic event %s was delivered to an external subscriber.",
                    message.getClass()
            ));
        }
        receivedMessage = true;
    }

    /**
     * Must be present in order for the subscriber to be valid for EventBus registration.
     *
     * <p>This subscriber should never be called.
     *
     * @param event ignored
     */
    @Subscribe
    public void on(ProjectCreated event) {
        fail("Unexpected event " + Json.toJson(event));
    }

    public boolean receivedExternalMessage() {
        return receivedMessage;
    }
}
