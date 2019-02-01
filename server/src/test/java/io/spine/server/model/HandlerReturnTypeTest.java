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
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.server.model.given.HandlerReturnTypeTestEnv.MessageProducer;
import io.spine.test.model.ModCreateProject;
import io.spine.test.model.ModProjectCreated;
import io.spine.test.model.ModProjectOwnerAssigned;
import io.spine.test.model.ModProjectStarted;
import io.spine.test.model.ModStartProject;
import io.spine.test.model.Rejections.ModCannotAssignOwnerToProject;
import io.spine.test.model.Rejections.ModProjectAlreadyExists;
import io.spine.type.MessageClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("HandlerReturnType should")
class HandlerReturnTypeTest {

    @Nested
    @DisplayName("extract produced message type")
    class ExtractProducedMessage {

        @Test
        @DisplayName("from command return type")
        void fromCommand() {
            checkProduces("emitCommand", ImmutableSet.of(ModCreateProject.class));
        }

        @Test
        @DisplayName("from event return type")
        void fromEvent() {
            checkProduces("emitEvent", ImmutableSet.of(ModProjectCreated.class));
        }

        @Test
        @DisplayName("from `Optional` return type")
        void fromOptional() {
            checkProduces("emitOptionalEvent", ImmutableSet.of(ModProjectStarted.class));
        }

        @Test
        @DisplayName("from `Iterable` return type")
        void fromIterable() {
            checkProduces("emitListOfCommands", ImmutableSet.of(ModStartProject.class));
        }
    }

    @Nested
    @DisplayName("extract multiple produced types")
    class ExtractMultipleProducedTypes {

        @Test
        @DisplayName("from `Either` return type")
        void fromEither() {
            checkProduces("emitEither",
                          ImmutableSet.of(ModProjectCreated.class,
                                          ModProjectAlreadyExists.class));
        }

        @Test
        @DisplayName("from `Tuple` return type")
        void fromTuple() {
            checkProduces("emitPair",
                          ImmutableSet.of(ModProjectCreated.class,
                                          ModProjectOwnerAssigned.class,
                                          ModCannotAssignOwnerToProject.class));
        }

        @Test
        @DisplayName("from type that mixes concrete type params and too broad type params")
        void fromMixedReturnType() {
            checkProduces("emitEitherWithTooBroad",
                          ImmutableSet.of(ModProjectOwnerAssigned.class,
                                          ModCannotAssignOwnerToProject.class));
        }
    }

    @Nested
    @DisplayName("return empty produced messages list")
    class ReturnEmptyList {

        @Test
        @DisplayName("for `void` return type")
        void forVoid() {
            checkProducesNothing("returnVoid");
        }

        @Test
        @DisplayName("for `Nothing` return type")
        void forNothing() {
            checkProducesNothing("returnNothing");
        }

        @Test
        @DisplayName("for `Empty` return type")
        void forEmpty() {
            checkProducesNothing("returnEmpty");
        }

        @Test
        @DisplayName("for method returning too broad message type")
        void forTooBroadType() {
            checkProducesNothing("returnTooBroadEvent");
        }

        @Test
        @DisplayName("for method parameterized with too broad message type")
        void forTooBroadTypeParam() {
            checkProducesNothing("returnTooBroadIterable");
        }

        private void checkProducesNothing(String methodName) {
            checkProduces(methodName, ImmutableSet.of());
        }
    }

    private static void checkProduces(String methodName,
                                      Collection<Class<? extends Message>> messageTypes) {
        try {
            Method method = MessageProducer.class.getMethod(methodName);
            HandlerReturnType<?> returnType = HandlerReturnType.of(method);
            Set<? extends MessageClass<?>> expectedTypes = messageTypes
                    .stream()
                    .map(HandlerReturnTypeTest::toCommandOrEventClass)
                    .collect(toSet());
            ImmutableSet<?> classes = returnType.producedMessages();
            assertThat(classes).containsExactlyElementsIn(expectedTypes);
        } catch (NoSuchMethodException e) {
            fail(e);
        }
    }

    @SuppressWarnings("unchecked") // Checked logically.
    private static MessageClass<?> toCommandOrEventClass(Class<? extends Message> type) {
        if (CommandMessage.class.isAssignableFrom(type)) {
            return CommandClass.from((Class<? extends CommandMessage>) type);
        }
        if (EventMessage.class.isAssignableFrom(type)) {
            return EventClass.from((Class<? extends EventMessage>) type);
        }
        throw newIllegalArgumentException("Unknown command/event type: %s",
                                          type.getCanonicalName());
    }
}
