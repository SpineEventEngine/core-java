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
import io.spine.server.model.given.HandlerReturnTypeTestEnv.MessageProducer;
import io.spine.server.type.CommandClass;
import io.spine.server.type.EventClass;
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

@DisplayName("ProducedTypeSet should")
class ProducedTypeSetTest {

    @Nested
    @DisplayName("be collected from a single produced message type")
    class IncludeProducedMessageType {

        @Test
        @DisplayName("from command return type")
        void fromCommand() {
            checkCollectedFor("emitCommand", ImmutableSet.of(ModCreateProject.class));
        }

        @Test
        @DisplayName("from event return type")
        void fromEvent() {
            checkCollectedFor("emitEvent", ImmutableSet.of(ModProjectCreated.class));
        }

        @Test
        @DisplayName("from `Optional` return type")
        void fromOptional() {
            checkCollectedFor("emitOptionalEvent", ImmutableSet.of(ModProjectStarted.class));
        }

        @Test
        @DisplayName("from `Iterable` return type")
        void fromIterable() {
            checkCollectedFor("emitListOfCommands", ImmutableSet.of(ModStartProject.class));
        }
    }

    @Nested
    @DisplayName("be collected from multiple produced types")
    class IncludeMultipleProducedTypes {

        @Test
        @DisplayName("from `Either` return type")
        void fromEither() {
            checkCollectedFor("emitEither",
                              ImmutableSet.of(ModProjectCreated.class,
                                              ModProjectAlreadyExists.class));
        }

        @Test
        @DisplayName("from `Tuple` return type")
        void fromTuple() {
            checkCollectedFor("emitPair",
                              ImmutableSet.of(ModProjectCreated.class,
                                              ModProjectOwnerAssigned.class,
                                              ModCannotAssignOwnerToProject.class));
        }

        @Test
        @DisplayName("from type that mixes concrete type params and too broad type params")
        void fromMixedReturnType() {
            checkCollectedFor("emitEitherWithTooBroad",
                              ImmutableSet.of(ModProjectOwnerAssigned.class,
                                              ModCannotAssignOwnerToProject.class));
        }
    }

    @Nested
    @DisplayName("be an empty produced messages list")
    class BeEmptyList {

        @Test
        @DisplayName("for `void` return type")
        void forVoid() {
            checkIsEmptyFor("returnVoid");
        }

        @Test
        @DisplayName("for `Nothing` return type")
        void forNothing() {
            checkIsEmptyFor("returnNothing");
        }

        @Test
        @DisplayName("for `Empty` return type")
        void forEmpty() {
            checkIsEmptyFor("returnEmpty");
        }

        @Test
        @DisplayName("for method returning too broad message type")
        void forTooBroadType() {
            checkIsEmptyFor("returnTooBroadEvent");
        }

        @Test
        @DisplayName("for method parameterized with too broad message type")
        void forTooBroadTypeParam() {
            checkIsEmptyFor("returnTooBroadIterable");
        }

        private void checkIsEmptyFor(String methodName) {
            checkCollectedFor(methodName, ImmutableSet.of());
        }
    }

    private static void checkCollectedFor(String methodName,
                                          Collection<Class<? extends Message>> expectedTypes) {
        try {
            Method method = MessageProducer.class.getMethod(methodName);
            ProducedTypeSet<?> producedTypes = ProducedTypeSet.collect(method);
            Set<? extends MessageClass<?>> expectedClasses = expectedTypes
                    .stream()
                    .map(ProducedTypeSetTest::toCommandOrEventClass)
                    .collect(toSet());
            ImmutableSet<?> classes = producedTypes.typeSet();
            assertThat(classes).containsExactlyElementsIn(expectedClasses);
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
