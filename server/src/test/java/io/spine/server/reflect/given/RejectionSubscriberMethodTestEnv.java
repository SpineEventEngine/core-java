/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.reflect.given;

import io.spine.core.CommandContext;
import io.spine.core.Subscribe;
import io.spine.test.reflect.ReflectRejections.InvalidProjectName;
import io.spine.test.rejection.command.UpdateProjectName;

import java.lang.reflect.Method;

public class RejectionSubscriberMethodTestEnv {

    /** Prevents instantiation of this utility class. */
    private RejectionSubscriberMethodTestEnv() {}

    /**
     * The abstract base for test subscriber classes.
     *
     * <p>The purpose of this class is to obtain a reference to a
     * {@linkplain #HANDLER_METHOD_NAME single subscriber method}.
     * This reference will be later used for assertions.
     */
    public abstract static class TestRejectionSubscriber {

        @SuppressWarnings("DuplicateStringLiteralInspection")
        private static final String HANDLER_METHOD_NAME = "handle";

        public Method getMethod() {
            final Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(HANDLER_METHOD_NAME)) {
                    return method;
                }
            }
            throw new RuntimeException("No rejection subscriber method found " +
                                               HANDLER_METHOD_NAME);
        }
    }


    public static class ValidSubscriberTwoParams extends TestRejectionSubscriber {
        @Subscribe
        public void handle(InvalidProjectName rejection, UpdateProjectName command) {
        }
    }

    public static class ValidSubscriberThreeParams extends TestRejectionSubscriber {
        @Subscribe
        public void handle(InvalidProjectName rejection,
                           UpdateProjectName command, CommandContext context) {
        }
    }

    public static class ValidSubscriberButPrivate extends TestRejectionSubscriber {
        @Subscribe
        private void handle(InvalidProjectName rejection, UpdateProjectName command) {
        }
    }

    /**
     * The subscriber with a method which is not annotated.
     */
    public static class InvalidSubscriberNoAnnotation extends TestRejectionSubscriber {
        @SuppressWarnings("unused")
        public void handle(InvalidProjectName rejection, UpdateProjectName command) {
        }
    }

    /**
     * The subscriber with a method which does not have parameters.
     */
    public static class InvalidSubscriberNoParams extends TestRejectionSubscriber {
        @Subscribe
        public void handle() {
        }
    }

    /**
     * The subscriber which has too many parameters.
     */
    public static class InvalidSubscriberTooManyParams extends TestRejectionSubscriber {
        @Subscribe
        public void handle(InvalidProjectName rejection,
                           UpdateProjectName command,
                           CommandContext context,
                           Object redundant) {
        }
    }

    /**
     * The subscriber which has invalid single argument.
     */
    public static class InvalidSubscriberOneNotMsgParam extends TestRejectionSubscriber {
        @Subscribe
        public void handle(Exception invalid) {
        }
    }

    /**
     * The subscriber with a method with first invalid parameter.
     */
    public static class InvalidSubscriberTwoParamsFirstInvalid extends TestRejectionSubscriber {
        @Subscribe
        public void handle(Exception invalid,  UpdateProjectName command) {
        }
    }

    /**
     * The subscriber which has invalid second parameter.
     */
    public static class InvalidSubscriberTwoParamsSecondInvalid extends TestRejectionSubscriber {
        @Subscribe
        public void handle(InvalidProjectName rejection, Exception invalid) {
        }
    }

    /**
     * The class with rejection subscriber that returns {@code Object} instead of {@code void}.
     */
    public static class InvalidSubscriberNotVoid extends TestRejectionSubscriber {
        @Subscribe
        public Object handle(InvalidProjectName rejection, UpdateProjectName command) {
            return rejection;
        }

    }
}
