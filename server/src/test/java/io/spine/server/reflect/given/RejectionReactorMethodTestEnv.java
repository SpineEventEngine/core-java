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

import com.google.protobuf.Empty;
import io.spine.core.CommandContext;
import io.spine.core.React;
import io.spine.test.reflect.ReflectRejections.InvalidProjectName;
import io.spine.test.rejection.command.UpdateProjectName;

import java.lang.reflect.Method;

public class RejectionReactorMethodTestEnv {

    /** Prevents instantiation of this utility class. */
    private RejectionReactorMethodTestEnv() {}

    /**
     * The abstract base for test subscriber classes.
     *
     * <p>The purpose of this class is to obtain a reference to a
     * {@linkplain #HANDLER_METHOD_NAME single subscriber method}.
     * This reference will be later used for assertions.
     */
    public abstract static class TestRejectionReactor {

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


    public static class ValidTwoParams extends TestRejectionReactor {
        @React
        public Empty handle(InvalidProjectName rejection, UpdateProjectName command) {
            return Empty.getDefaultInstance();
        }
    }

    public static class ValidThreeParams extends TestRejectionReactor {
        @React
        public Empty handle(InvalidProjectName rejection,
                           UpdateProjectName command, CommandContext context) {
            return Empty.getDefaultInstance();
        }
    }

    public static class ValidButPrivate extends TestRejectionReactor {
        @SuppressWarnings("MethodMayBeStatic") // Need instance method for the test.
        @React
        private Empty handle(InvalidProjectName rejection, UpdateProjectName command) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The subscriber with a method which is not annotated.
     */
    public static class InvalidNoAnnotation extends TestRejectionReactor {
        @SuppressWarnings("unused")
        public Empty handle(InvalidProjectName rejection, UpdateProjectName command) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The subscriber with a method which does not have parameters.
     */
    public static class InvalidNoParams extends TestRejectionReactor {
        @React
        public Empty handle() {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The subscriber which has too many parameters.
     */
    public static class InvalidTooManyParams extends TestRejectionReactor {
        @React
        public Empty handle(InvalidProjectName rejection,
                           UpdateProjectName command,
                           CommandContext context,
                           Object redundant) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The subscriber which has invalid single argument.
     */
    public static class InvalidOneNotMsgParam extends TestRejectionReactor {
        @React
        public Empty handle(Exception invalid) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The subscriber with a method with first invalid parameter.
     */
    public static class InvalidTwoParamsFirstInvalid extends TestRejectionReactor {
        @React
        public Empty handle(Exception invalid,  UpdateProjectName command) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The subscriber which has invalid second parameter.
     */
    public static class InvalidTwoParamsSecondInvalid extends TestRejectionReactor {
        @React
        public Empty handle(InvalidProjectName rejection, Exception invalid) {
            return Empty.getDefaultInstance();
        }
    }

    /**
     * The class with rejection subscriber that returns {@code Object} instead of {@code void}.
     */
    public static class InvalidNotMessage extends TestRejectionReactor {
        @React
        public Object handle(InvalidProjectName rejection, UpdateProjectName command) {
            return rejection;
        }
    }
}
