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

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.React;
import io.spine.test.reflect.ReflectRejections.InvalidProjectName;
import io.spine.test.rejection.command.RjUpdateProjectName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

/**
 * @author Alexander Yevsyukov
 */
public class RejectionReactorMethodTestEnv {

    /** Prevents instantiation of this utility class. */
    private RejectionReactorMethodTestEnv() {
    }

    /**
     * The abstract base for testing reactor classes.
     *
     * <p>The purpose of this class is to obtain a reference to a
     * {@linkplain #HANDLER_METHOD_NAME single reactor method}.
     * This reference will be later used for assertions.
     */
    public abstract static class TestRejectionReactor {

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
            throw new RuntimeException("No rejection reactor method found " +
                                               HANDLER_METHOD_NAME);
        }
    }

    public static class RValidTwoParams extends TestRejectionReactor {
        @React
        public Empty handle(InvalidProjectName rejection, RjUpdateProjectName command) {
            return Empty.getDefaultInstance();
        }
    }

    public static class RValidThreeParams extends TestRejectionReactor {

        private @Nullable Message lastRejectionMessage;
        private @Nullable Message lastCommandMessage;
        private @Nullable CommandContext lastCommandContext;

        @React
        public Empty handle(InvalidProjectName rejection,
                            RjUpdateProjectName command,
                            CommandContext context) {
            lastRejectionMessage = rejection;
            lastCommandMessage = command;
            lastCommandContext = context;

            return Empty.getDefaultInstance();
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

    public static class RValidButPrivate extends TestRejectionReactor {
        @SuppressWarnings("MethodMayBeStatic") // Need instance method for the test.
        @React
        private Empty handle(InvalidProjectName rejection, RjUpdateProjectName command) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The reactor with a method which is not annotated.
     */
    public static class RInvalidNoAnnotation extends TestRejectionReactor {
        @SuppressWarnings("unused")
        public Empty handle(InvalidProjectName rejection, RjUpdateProjectName command) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The reactor with a method which does not have parameters.
     */
    public static class RInvalidNoParams extends TestRejectionReactor {
        @React
        public Empty handle() {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The reactor which has too many parameters.
     */
    public static class RInvalidTooManyParams extends TestRejectionReactor {
        @React
        public Empty handle(InvalidProjectName rejection,
                            RjUpdateProjectName command,
                            CommandContext context,
                            Object redundant) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The reactor which has invalid single argument.
     */
    public static class RInvalidOneNotMsgParam extends TestRejectionReactor {
        @React
        public Empty handle(Exception invalid) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The reactor with a method with first invalid parameter.
     */
    public static class RInvalidTwoParamsFirstInvalid extends TestRejectionReactor {
        @React
        public Empty handle(Exception invalid, RjUpdateProjectName command) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The reactor which has invalid second parameter.
     */
    public static class RInvalidTwoParamsSecondInvalid extends TestRejectionReactor {
        @React
        public Empty handle(InvalidProjectName rejection, Exception invalid) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The class with rejection reactor that returns {@code Object} instead of {@code Message}.
     */
    public static class RInvalidNotMessage extends TestRejectionReactor {
        @React
        public Object handle(InvalidProjectName rejection, RjUpdateProjectName command) {
            return rejection;
        }
    }
}
