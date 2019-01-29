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

package io.spine.server.model;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.server.model.given.EmittedTypesExtractorTestEnv.MessageEmitter;
import io.spine.test.model.ModCreateProject;
import io.spine.test.model.ModProjectCreated;
import io.spine.test.model.ModProjectOwnerAssigned;
import io.spine.test.model.ModProjectStarted;
import io.spine.test.model.ModStartProject;
import io.spine.test.model.Rejections.ModCannotAssignOwnerToProject;
import io.spine.test.model.Rejections.ModProjectAlreadyExists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("ReturnTypeAnalyzerShould")
class EmittedTypesExtractorTest {

    @Nested
    @DisplayName("extract emitted message type")
    class ExtractEmitted {

        @Test
        @DisplayName("from command return type")
        void fromCommand() {
            checkEmits("emitCommand", ImmutableSet.of(ModCreateProject.class));
        }

        @Test
        @DisplayName("from event return type")
        void fromEvent() {
            checkEmits("emitEvent", ImmutableSet.of(ModProjectCreated.class));
        }

        @Test
        @DisplayName("from `Optional` return type")
        void fromOptional() {
            checkEmits("emitOptionalEvent", ImmutableSet.of(ModProjectStarted.class));
        }

        @Test
        @DisplayName("from `Iterable` return type")
        void fromIterable() {
            checkEmits("emitListOfCommands", ImmutableSet.of(ModStartProject.class));
        }

        @Test
        @DisplayName("from non-parameterized `Iterable` descendant")
        void fromIterableDescendant() {
            checkEmits("emitModProjectStartedList", ImmutableSet.of(ModProjectStarted.class));
        }
    }

    @Nested
    @DisplayName("extract multiple emitted types")
    class ExtractMultipleEmitted {

        @Test
        @DisplayName("from `Either` return type")
        void fromEither() {
            checkEmits("emitEither",
                       ImmutableSet.of(ModProjectCreated.class,
                                       ModProjectAlreadyExists.class));
        }

        @Test
        @DisplayName("from `Tuple` return type")
        void fromTuple() {
            checkEmits("emitPair",
                       ImmutableSet.of(ModProjectCreated.class,
                                       ModProjectOwnerAssigned.class,
                                       ModCannotAssignOwnerToProject.class));
        }

        @Test
        @DisplayName("from type that mixes concrete type params and too broad type params")
        void fromMixedReturnType() {
            checkEmits("emitEitherWithTooBroad",
                       ImmutableSet.of(ModProjectOwnerAssigned.class,
                                       ModCannotAssignOwnerToProject.class));
        }
    }

    @Nested
    @DisplayName("return empty emitted messages list")
    class ReturnEmptyList {

        @Test
        @DisplayName("for `void` return type")
        void forVoid() {
            checkEmitsNothing("returnVoid");
        }

        @Test
        @DisplayName("for `Nothing` return type")
        void forNothing() {
            checkEmitsNothing("returnNothing");
        }

        @Test
        @DisplayName("for `Empty` return type")
        void forEmpty() {
            checkEmitsNothing("returnEmpty");
        }

        @Test
        @DisplayName("for method returning too broad message type")
        void forTooBroadType() {
            checkEmitsNothing("returnTooBroadEvent");
        }

        @Test
        @DisplayName("for method parameterized with too broad message type")
        void forTooBroadTypeParam() {
            checkEmitsNothing("returnTooBroadIterable");
        }

        private void checkEmitsNothing(String methodName) {
            checkEmits(methodName, ImmutableSet.of());
        }
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Method called to throw exception.
    @Test
    @DisplayName("throw IAE when created for method with non-familiar return type")
    void throwOnUnknownReturnType() throws NoSuchMethodException {
        Method method = MessageEmitter.class.getMethod("returnRandomType");
        assertThrows(IllegalArgumentException.class,
                     () -> EmittedTypesExtractor.forMethod(method));
    }

    private static void checkEmits(String methodName,
                                   Iterable<Class<? extends Message>> messageTypes) {
        try {
            Method method = MessageEmitter.class.getMethod(methodName);
            EmittedTypesExtractor extractor = EmittedTypesExtractor.forMethod(method);
            ImmutableSet<Class<? extends Message>> classes = extractor.extract();
            assertThat(classes).containsExactlyElementsIn(messageTypes);
        } catch (NoSuchMethodException e) {
            fail(e);
        }
    }
}
