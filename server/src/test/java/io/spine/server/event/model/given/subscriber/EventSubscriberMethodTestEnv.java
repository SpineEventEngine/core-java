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

package io.spine.server.event.model.given.subscriber;

import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.test.reflect.event.RefProjectCreated;

/**
 * @author Alexander Yevsyukov
 */
public class EventSubscriberMethodTestEnv {

    /** Prevents instantiation of this utility class. */
    private EventSubscriberMethodTestEnv() {
    }

    public static class ValidOneParam extends TestEventSubscriber {
        @Subscribe
        public void handle(RefProjectCreated event) {
        }
    }

    public static class ValidTwoParams extends TestEventSubscriber {
        @Subscribe
        public void handle(RefProjectCreated event, EventContext context) {
        }
    }

    public static class ValidButPrivate extends TestEventSubscriber {
        @Subscribe
        private void handle(RefProjectCreated event) {
        }
    }

    /**
     * The subscriber with a method which is not annotated.
     */
    public static class InvalidNoAnnotation extends TestEventSubscriber {
        @SuppressWarnings("unused")
        public void handle(RefProjectCreated event, EventContext context) {
        }
    }

    /**
     * The subscriber with a method which does not have parameters.
     */
    public static class InvalidNoParams extends TestEventSubscriber {
        @Subscribe
        public void handle() {
        }
    }

}
