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

package io.spine.server.event.given;

import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived;
import io.spine.test.reflect.event.RefProjectCreated;

import java.lang.reflect.Method;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Alexander Yevsyukov
 */
public class EventSubscriberMethodTestEnv {

    /** Prevents instantiation of this utility class. */
    private EventSubscriberMethodTestEnv() {
    }

    public static class ValidOneParam extends TestEventSubscriber {
        @Subscribe
        void handle(RefProjectCreated event) {
        }
    }

    public static class ValidTwoParams extends TestEventSubscriber {
        @Subscribe
        public void handle(RefProjectCreated event, EventContext context) {
        }
    }

    public static class ValidButPrivate extends TestEventSubscriber {
        @Subscribe
        void handle(RefProjectCreated event) {
        }
    }

    /**
     * The subscriber with a method which is not annotated.
     */
    public static class InvalidNoAnnotation extends TestEventSubscriber {
        @SuppressWarnings("unused")
        void handle(RefProjectCreated event, EventContext context) {
        }
    }

    /**
     * The subscriber with a method which does not have parameters.
     */
    public static class InvalidNoParams extends TestEventSubscriber {
        @Subscribe
        void handle() {
        }
    }

    /**
     * The subscriber which has too many parameters.
     */
    public static class InvalidTooManyParams extends TestEventSubscriber {
        @Subscribe
        void handle(RefProjectCreated event, EventContext context, Object redundant) {
        }
    }

    /**
     * The subscriber which has invalid single argument.
     */
    public static class InvalidOneNotMsgParam extends TestEventSubscriber {
        @Subscribe
        void handle(Exception invalid) {
        }
    }

    /**
     * The subscriber with a method with first invalid parameter.
     */
    public static class InvalidTwoParamsFirstInvalid extends TestEventSubscriber {
        @Subscribe
        void handle(Exception invalid, EventContext context) {
        }
    }

    /**
     * The subscriber which has invalid second parameter.
     */
    public static class InvalidTwoParamsSecondInvalid extends TestEventSubscriber {
        @Subscribe
        void handle(RefProjectCreated event, Exception invalid) {
        }
    }

    /**
     * The class with event subscriber that returns {@code Object} instead of {@code void}.
     */
    public static class InvalidNotVoid extends TestEventSubscriber {
        @Subscribe
        Object handle(RefProjectCreated event) {
            return event;
        }
    }

    /**
     * The class which subscribes to a rejection message, not an event message.
     */
    public static class ARejectionSubscriber extends TestEventSubscriber {
        @Subscribe
        void handle(EntityAlreadyArchived rejection) {
        }
    }

    public static class ExternalSubscriber extends TestEventSubscriber {
        @Subscribe(external = true)
        void handle(RefProjectCreated externalEvent) {
        }
    }

    /**
     * The abstract base for test subscriber classes.
     *
     * <p>The purpose of this class is to obtain a reference to a
     * {@linkplain #HANDLER_METHOD_NAME single subscriber method}.
     * This reference will be later used for assertions.
     */
    public abstract static class TestEventSubscriber {

        @SuppressWarnings("DuplicateStringLiteralInspection")
        private static final String HANDLER_METHOD_NAME = "handle";

        public Method getMethod() {
            Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName()
                          .equals(HANDLER_METHOD_NAME)) {
                    return method;
                }
            }
            throw newIllegalStateException("No subscriber method found: %s", HANDLER_METHOD_NAME);
        }
    }
}
