/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.event.given;

import io.spine.core.EventContext;
import io.spine.core.External;
import io.spine.core.Subscribe;
import io.spine.logging.Logging;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.test.event.FailRequested;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.TaskAdded;

public final class EventSubscriberTestEnv {

    /** Prevents instantiation of this utility class. */
    private EventSubscriberTestEnv() {
    }

    /** The subscriber which throws exception from the subscriber method. */
    public static class FailingSubscriber extends AbstractEventSubscriber implements Logging {

        private boolean methodCalled = false;

        @Subscribe
        void on(FailRequested message, EventContext context) {
            methodCalled = true;
            if (!message.getShouldFail()) {
                throw new UnsupportedOperationException("Do not want false messages!");
            }
        }

        @Subscribe
        void on(ProjectCreated message) {
            // Do nothing. Just expose the method.
        }

        @Subscribe
        void on(ProjectStarted message) {
            // Do nothing. Just expose the method.
        }

        @Subscribe
        void on(@External TaskAdded message) {
            // Do nothing. Just expose the method.
        }

        public boolean isMethodCalled() {
            return this.methodCalled;
        }
    }
}
