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
package io.spine.server.rejection.given;

import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.Subscribe;
import io.spine.test.reflect.ReflectRejections.InvalidProjectName;
import io.spine.test.rejection.command.RjUpdateProjectName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

/**
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 */
public class RejectionSubscriberMethodTestEnv {
    /** Prevents instantiation of this utility class. */
    private RejectionSubscriberMethodTestEnv() {
    }

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
            Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName()
                          .equals(HANDLER_METHOD_NAME)) {
                    return method;
                }
            }
            throw new RuntimeException("No rejection subscriber method found " +
                                               HANDLER_METHOD_NAME);
        }
    }

    public static class ValidTwoParams extends TestRejectionSubscriber {
        @Subscribe
        public void handle(InvalidProjectName rejection,
                           RjUpdateProjectName command) {
            // do nothing.
        }
    }

    public static class ValidThreeParams extends TestRejectionSubscriber {

        private @Nullable Message lastRejectionMessage;
        private @Nullable Message lastCommandMessage;
        private @Nullable CommandContext lastCommandContext;

        @Subscribe
        public void handle(InvalidProjectName rejection,
                           RjUpdateProjectName command,
                           CommandContext context) {
            lastRejectionMessage = rejection;
            lastCommandMessage = command;
            lastCommandContext = context;
        }

        public @Nullable Message getLastRejectionMessage() {
            return lastRejectionMessage;
        }

        public @Nullable Message getLastCommandMessage() {
            return lastCommandMessage;
        }

        public @Nullable CommandContext getLastCommandContext() {
            return lastCommandContext;
        }
    }

    public static class ValidButPrivate extends TestRejectionSubscriber {
        @SuppressWarnings("MethodMayBeStatic") // Need instance method for the test.
        @Subscribe
        private void handle(InvalidProjectName rejection,
                            RjUpdateProjectName command) {
            // do nothing.
        }
    }

    /**
     * The subscriber with a method which is not annotated.
     */
    public static class InvalidNoAnnotation extends TestRejectionSubscriber {
        @SuppressWarnings("unused")
        public void handle(InvalidProjectName rejection,
                           RjUpdateProjectName command) {
            // do nothing.
        }
    }

    /**
     * The subscriber with a method which does not have parameters.
     */
    public static class InvalidNoParams extends TestRejectionSubscriber {
        @Subscribe
        public void handle() {
            // do nothing.
        }
    }

    /**
     * The subscriber which has too many parameters.
     */
    public static class InvalidTooManyParams extends TestRejectionSubscriber {
        @Subscribe
        public void handle(InvalidProjectName rejection,
                           RjUpdateProjectName command,
                           CommandContext context,
                           Object redundant) {
            // do nothing.
        }
    }

    /**
     * The subscriber which has invalid single argument.
     */
    public static class InvalidOneNotMsgParam extends TestRejectionSubscriber {
        @Subscribe
        public void handle(Exception invalid) {
            // do nothing.
        }
    }

    /**
     * The subscriber with a method with first invalid parameter.
     */
    public static class InvalidTwoParamsFirstInvalid extends TestRejectionSubscriber {
        @Subscribe
        public void handle(Exception invalid, RjUpdateProjectName command) {
            // do nothing.
        }
    }

    /**
     * The subscriber which has invalid second parameter.
     */
    public static class InvalidTwoParamsSecondInvalid extends TestRejectionSubscriber {
        @Subscribe
        public void handle(InvalidProjectName rejection, Exception invalid) {
            // do nothing.
        }
    }

    /**
     * The class with rejection subscriber that returns {@code Object} instead of {@code void}.
     */
    public static class InvalidNotMessage extends TestRejectionSubscriber {
        @Subscribe
        public Object handle(InvalidProjectName rejection,
                             RjUpdateProjectName command) {
            return rejection;
        }
    }
}
